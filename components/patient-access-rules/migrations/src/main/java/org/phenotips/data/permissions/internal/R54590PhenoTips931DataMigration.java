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

package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.Owner;

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
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

/**
 * Migration for PhenoTips issue #931: Add a {@code PhenoTips.OwnerClass} to all patient records that don't have one.
 *
 * @version $Id$
 * @since 1.0M13
 */
@Component(roles = { HibernateDataMigration.class })
@Named("R54590Phenotips#931")
@Singleton
public class R54590PhenoTips931DataMigration extends AbstractHibernateDataMigration
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Serializes the owner name. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String getDescription()
    {
        return "Add a PhenoTips.OwnerClass to all patient records that don't have one";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54590);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new AddOwnerCallback());
    }

    /**
     * Searches for all patient documents that don't contain a {@code PhenoTips.OwnerClass} object, excluding
     * {@code PatientTemplate}, and adds a new owner object with the document's creator set as the owner.
     */
    private class AddOwnerCallback implements HibernateCallback<Object>
    {
        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            String pclass = R54590PhenoTips931DataMigration.this.serializer.serialize(Patient.CLASS_REFERENCE);
            String oclass = R54590PhenoTips931DataMigration.this.serializer.serialize(Owner.CLASS_REFERENCE);
            Query q =
                session.createQuery("select distinct doc.fullName from XWikiDocument doc, BaseObject p where "
                    + "p.name = doc.fullName and p.className = '" + pclass + "' and "
                    + "doc.name <> 'PatientTemplate' and not exists "
                    + "(from BaseObject o where o.name = doc.fullName and o.className = '" + oclass + "')");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                try {
                    XWikiDocument doc =
                        xwiki.getDocument(R54590PhenoTips931DataMigration.this.resolver.resolve(docName), context);
                    if (doc == null) {
                        continue;
                    }
                    BaseObject owner = doc.newXObject(Owner.CLASS_REFERENCE, context);
                    if (doc.getCreatorReference() == null) {
                        owner.setStringValue(Owner.PROPERTY_NAME, "");
                    } else {
                        owner.setStringValue(Owner.PROPERTY_NAME,
                            R54590PhenoTips931DataMigration.this.serializer.serialize(doc.getCreatorReference()));
                    }
                    doc.setMinorEdit(true);
                    // Preserve the old author, there's no need to show this change in the history
                    doc.setMetaDataDirty(false);
                    doc.setContentDirty(false);
                    // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                    // so we must clear the session cache first.
                    session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                    session.flush();
                } catch (DataMigrationException ex) {
                    // We're in the middle of a migration, we're not expecting another migration
                } catch (XWikiException ex) {
                    R54590PhenoTips931DataMigration.this.logger.warn("Failed to add owner object to patient [{}]: {}",
                        docName, ex.getMessage());
                }
            }
            return null;
        }
    }
}
