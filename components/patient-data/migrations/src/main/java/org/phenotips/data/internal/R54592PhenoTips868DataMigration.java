/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.internal;

import org.phenotips.Constants;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #868: Fix broken SVG code generated before fixing issue #556.
 * 
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("R54592Phenotips#868")
@Singleton
public class R54592PhenoTips868DataMigration extends AbstractHibernateDataMigration
{
    private static final EntityReference PEDIGREE_CLASS = new EntityReference("PedigreeClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String getDescription()
    {
        return "Fix broken SVG code generated before fixing issue #556";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54592);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new FixPedigreeImageCallback());
    }

    /**
     * Searches for all documents containing pedigree images containing a wrongly placed quote and fixes them.
     */
    private class FixPedigreeImageCallback implements XWikiHibernateBaseStore.HibernateCallback<Object>
    {
        /** The name of the property to fix. */
        private static final String PROPERTY_NAME = "image";

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            Query q =
                session.createQuery("select distinct o.name from BaseObject o, LargeStringProperty p where"
                    + " o.className = '" + R54592PhenoTips868DataMigration.this.serializer.serialize(PEDIGREE_CLASS)
                    + "' and p.id.id = o.id and p.id.name = '" + PROPERTY_NAME + "' and p.value like '% \"width=\"%'");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R54592PhenoTips868DataMigration.this.resolver.resolve(docName), context);
                for (BaseObject object : doc.getXObjects(PEDIGREE_CLASS)) {
                    if (object == null) {
                        continue;
                    }
                    LargeStringProperty oldProperty = (LargeStringProperty) object.get(PROPERTY_NAME);
                    if (oldProperty == null || StringUtils.isBlank(oldProperty.getValue())) {
                        continue;
                    }
                    String image = oldProperty.getValue();
                    // Remove original attributes that should have been removed but are left as duplicates
                    image = image.replaceFirst(" width=\"\\d+\"", "").replaceFirst(" height=\"\\d+\"", "");
                    // Fix broken attributes
                    image = image.replaceFirst(" \"width=", "\" width=");
                    oldProperty.setValue(image);
                }
                doc.setComment("Fixed broken pedigree image");
                doc.setMinorEdit(true);
                try {
                    // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                    // so we must clear the session cache first.
                    session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                    session.flush();
                } catch (DataMigrationException e) {
                    // We're in the middle of a migration, we're not expecting another migration
                }
            }
            return null;
        }
    }
}
