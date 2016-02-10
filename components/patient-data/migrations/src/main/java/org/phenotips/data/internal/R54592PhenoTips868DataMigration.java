/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.internal;

import org.phenotips.studies.family.Pedigree;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
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
 * @since 1.0M13
 */
@Component
@Named("R54592Phenotips#868")
@Singleton
public class R54592PhenoTips868DataMigration extends AbstractHibernateDataMigration
{
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
        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            Query q =
                session.createQuery("select distinct o.name from BaseObject o, LargeStringProperty p where"
                    + " o.className = '"
                    + R54592PhenoTips868DataMigration.this.serializer.serialize(Pedigree.CLASS_REFERENCE)
                    + "' and p.id.id = o.id and p.id.name = '" + Pedigree.IMAGE + "' and p.value like '% \"width=\"%'");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R54592PhenoTips868DataMigration.this.resolver.resolve(docName), context);
                for (BaseObject object : doc.getXObjects(Pedigree.CLASS_REFERENCE)) {
                    if (object == null) {
                        continue;
                    }
                    LargeStringProperty oldProperty = (LargeStringProperty) object.get(Pedigree.IMAGE);
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
