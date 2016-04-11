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

import org.phenotips.Constants;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.NumberProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #633: Automatically migrate existing {@code in_vitro} values to the {@code ivf} field.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component
@Named("R52090Phenotips#633")
@Singleton
public class R52090PhenoTips633DataMigration extends AbstractHibernateDataMigration
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
        return "Migrate existing in_vitro values to the new ivf field";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(52090);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigrateIVFCallback());
    }

    /**
     * Searches for all documents containing values for the {@code in_vitro} property, and for each such document and
     * for each such object, sets the value for {@code ivf}.
     */
    private class MigrateIVFCallback implements HibernateCallback<Object>
    {
        /** The name of the old property. */
        private static final String OLD_NAME = "in_vitro";

        /** The name of the new property. */
        private static final String NEW_NAME = "ivf";

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            DocumentReference classReference =
                new DocumentReference(context.getWikiId(), Constants.CODE_SPACE, "PatientClass");
            Query q =
                session.createQuery("select distinct o.name from BaseObject o, IntegerProperty p where o.className = '"
                    + R52090PhenoTips633DataMigration.this.serializer.serialize(classReference)
                    + "' and p.id.id = o.id and p.id.name = '" + OLD_NAME + "' and p.value IS NOT NULL");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R52090PhenoTips633DataMigration.this.resolver.resolve(docName), context);
                BaseObject object = doc.getXObject(classReference);
                NumberProperty oldProperty = (NumberProperty) object.get(OLD_NAME);
                if (oldProperty == null) {
                    continue;
                }
                object.removeField(OLD_NAME);
                NumberProperty newProperty = oldProperty.clone();
                newProperty.setName(NEW_NAME);
                object.addField(NEW_NAME, newProperty);
                doc.setComment("Migrated in_vitro to ivf");
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
