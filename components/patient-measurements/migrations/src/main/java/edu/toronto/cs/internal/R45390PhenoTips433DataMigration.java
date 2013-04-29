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

package edu.toronto.cs.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
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
    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Override
    public String getDescription()
    {
        return "Migrate existing old ClinicalInformationCode.MeasurementsClass to the new PhenoTips.MeasurementsClass";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(45381);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // migrate data
        getStore().executeWrite(getXWikiContext(), new R45390Callback());
    }

    /**
     * Searches for all documents containing {@code ClinicalInformationCode.MeasurementsClass} objects, and for each
     * such documents and foreach such object, creates a new {@code PhenoTips.MeasurementsClass} object and copies all
     * the values from the deprecated object to the new one, and then deletes the old objects.
     */
    private class R45390Callback implements HibernateCallback<Object>
    {
        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            DocumentReference oldClassReference =
                new DocumentReference(context.getDatabase(), "ClinicalInformationCode", "MeasurementsClass");
            DocumentReference newClassReference =
                new DocumentReference(context.getDatabase(), "PhenoTips", oldClassReference.getName());
            BaseClass newClass = xwiki.getXClass(newClassReference, context);
            Query q =
                session.createQuery("select distinct o.name from BaseObject o"
                    + " where o.className = 'ClinicalInformationCode.MeasurementsClass'");
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R45390PhenoTips433DataMigration.this.resolver.resolve(docName), context);
                for (BaseObject oldObject : doc.getXObjects(oldClassReference)) {
                    BaseObject newObject = doc.newXObject(newClassReference, context);
                    BaseProperty value;
                    for (String property : newClass.getPropertyList()) {
                        value = (BaseProperty) oldObject.get(property);
                        if (value != null && value.getValue() != null) {
                            newObject.set(property, value.getValue(), context);
                        }
                    }
                    // "head_circumference" has been renamed to "hc"
                    value = (BaseProperty) oldObject.get("head_circumference");
                    if (value != null && value.getValue() != null) {
                        newObject.set("hc", value.getValue(), context);
                    }
                }
                doc.removeXObjects(oldClassReference);
                doc.setComment("Migrated measurements");
                doc.setMinorEdit(true);
                try {
                    // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                    // so we must clear the session cache first.
                    session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, true);
                    session.flush();
                } catch (DataMigrationException e) {
                    // We're in the middle of a migration, we're not expecting another migration
                }
            }
            return null;
        }
    }
}
