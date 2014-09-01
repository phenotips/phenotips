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

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;
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
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #949: Automatically migrate existing medical notes fields to their new names. More
 * specifically, renames {@code family_comments} to {@code family_history}, {@code prenatal_comments} to
 * {@code prenatal_development}, {@code medical_developmental_history} to {@code medical_history}, and {@code comments}
 * to {@code diagnosis_notes}.
 *
 * @version $Id$
 * @since 1.0M13
 */
@Component
@Named("R54593Phenotips#949")
@Singleton
public class R54593PhenoTips949DataMigration extends AbstractHibernateDataMigration
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
        return "Migrate existing medical notes fields to their new names";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54593);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigrateNotesCallback("family_comments", "family_history"));
        getStore().executeWrite(getXWikiContext(),
            new MigrateNotesCallback("prenatal_comments", "prenatal_development"));
        getStore().executeWrite(getXWikiContext(),
            new MigrateNotesCallback("medical_developmental_history", "medical_history"));
        getStore().executeWrite(getXWikiContext(), new MigrateNotesCallback("comments", "diagnosis_notes"));
    }

    /**
     * Searches for all documents containing values for a property with the {@link #oldName}, and for each such document
     * and for each such object, changes the property name to {@link #newName} .
     */
    private final class MigrateNotesCallback implements XWikiHibernateBaseStore.HibernateCallback<Object>
    {
        /** The name of the old property. */
        private final String oldName;

        /** The name of the new property. */
        private final String newName;

        private MigrateNotesCallback(String oldName, String newName)
        {
            this.oldName = oldName;
            this.newName = newName;
        }

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            Query q = session.createQuery("select distinct o.name from BaseObject o, LargeStringProperty p"
                + " where o.className = '"
                + R54593PhenoTips949DataMigration.this.serializer.serialize(Patient.CLASS_REFERENCE)
                + "' and p.id.id = o.id and p.id.name = '" + this.oldName + "'");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R54593PhenoTips949DataMigration.this.resolver.resolve(docName), context);
                for (BaseObject object : doc.getXObjects(Patient.CLASS_REFERENCE)) {
                    if (object == null) {
                        continue;
                    }
                    LargeStringProperty oldProperty = (LargeStringProperty) object.get(this.oldName);
                    if (oldProperty == null) {
                        continue;
                    }
                    object.removeField(this.oldName);
                    LargeStringProperty newProperty = (LargeStringProperty) oldProperty.clone();
                    newProperty.setName(this.newName);
                    object.addField(this.newName, newProperty);
                }
                doc.setComment("Migrated " + this.oldName + " to " + this.newName);
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
