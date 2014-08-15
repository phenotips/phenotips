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

package org.phenotips.measurements.internal;

import org.phenotips.Constants;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.Collections;
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
 * Migration for PhenoTips issue #433: Automatically migrate existing old
 * {@code ClinicalInformationCode.MeasurementsClass} to the new {@code PhenoTips.MeasurementsClass}.
 *
 * @version $Id$
 * @since 1.0M6
 */
@Component
@Named("R45390Phenotips#433")
@Singleton
public class R45390PhenoTips433DataMigration extends AbstractHibernateDataMigration
{
    /** The old class, without a wiki specified. */
    private static final EntityReference OLD_CLASS = new EntityReference("MeasurementsClass", EntityType.DOCUMENT,
        new EntityReference("ClinicalInformationCode", EntityType.SPACE));

    /** The new class, without a wiki specified. */
    private static final EntityReference NEW_CLASS = new EntityReference(OLD_CLASS.getName(), EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Resolves class names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> entityResolver;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String getDescription()
    {
        return "Migrate existing old ClinicalInformationCode.MeasurementsClass to the new PhenoTips.MeasurementsClass";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(45390);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigrateObjectsCallback());
    }

    /**
     * Searches for all documents containing {@code ClinicalInformationCode.MeasurementsClass} objects, and for each
     * such documents and foreach such object, creates a new {@code PhenoTips.MeasurementsClass} object and copies all
     * the values from the deprecated object to the new one, and then deletes the old objects.
     */
    private class MigrateObjectsCallback implements HibernateCallback<Object>
    {
        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            DocumentReference oldClassReference =
                R45390PhenoTips433DataMigration.this.entityResolver.resolve(OLD_CLASS);
            DocumentReference newClassReference =
                R45390PhenoTips433DataMigration.this.entityResolver.resolve(NEW_CLASS);
            Query q =
                session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                    + R45390PhenoTips433DataMigration.this.serializer.serialize(oldClassReference) + "'");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R45390PhenoTips433DataMigration.this.resolver.resolve(docName), context);
                for (BaseObject oldObject : doc.getXObjects(oldClassReference)) {
                    BaseObject newObject = oldObject.duplicate();
                    newObject.setXClassReference(newClassReference);
                    doc.addXObject(newObject);
                    // "head_circumference" has been renamed to "hc"
                    NumberProperty hc = (NumberProperty) newObject.get("head_circumference");
                    if (hc != null && hc.getValue() != null) {
                        newObject.removeField(hc.getName());
                        newObject.setFieldsToRemove(Collections.emptyList());
                        newObject.safeput("hc", hc);
                    }
                }
                doc.removeXObjects(oldClassReference);
                doc.setComment("Migrated measurements");
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
