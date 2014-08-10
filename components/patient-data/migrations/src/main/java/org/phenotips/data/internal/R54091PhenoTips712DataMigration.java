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
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #712: Automatically migrate existing {@code age_of_onset} values to the
 * {@code global_age_of_onset} field.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component
@Named("R54091Phenotips#712")
@Singleton
public class R54091PhenoTips712DataMigration extends AbstractHibernateDataMigration
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
        return "Migrate existing age_of_onset values to the new global_age_of_onset field";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54091);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigrateAoOCallback());
    }

    /**
     * Searches for all documents containing values for the {@code age_of_onset} property,
     * and for each such document and for each such object, sets the value for {@code global_age_of_onset}.
     */
    private class MigrateAoOCallback implements XWikiHibernateBaseStore.HibernateCallback<Object>
    {
        /** The name of the old property. */
        private static final String OLD_NAME = "age_of_onset";

        /** The name of the new property. */
        private static final String NEW_NAME = "global_age_of_onset";

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            DocumentReference classReference =
                new DocumentReference(context.getDatabase(), Constants.CODE_SPACE, "PatientClass");
            Query q =
                session.createQuery("select distinct o.name from BaseObject o, StringProperty p where o.className = '"
                    + R54091PhenoTips712DataMigration.this.serializer.serialize(classReference)
                    + "' and p.id.id = o.id and p.id.name = '" + OLD_NAME + "' and p.value IS NOT NULL");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R54091PhenoTips712DataMigration.this.resolver.resolve(docName), context);
                BaseObject object = doc.getXObject(classReference);
                StringProperty oldProperty = (StringProperty) object.get(OLD_NAME);
                if (oldProperty == null) {
                    continue;
                }
                object.removeField(OLD_NAME);
                StringProperty newProperty = (StringProperty) oldProperty.clone();
                newProperty.setName(NEW_NAME);
                object.addField(NEW_NAME, newProperty);
                doc.setComment("Migrated age_of_onset to global_age_of_onset");
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
