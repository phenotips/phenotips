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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #994: Update prenatal phenotypes to use the specific congenital terms instead of the
 * generic adult ones.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("R54594PhenoTips#994")
@Singleton
public final class R54594PhenoTips994DataMigration extends AbstractHibernateDataMigration
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

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
        return "Update prenatal phenotypes to use the specific congenital terms instead of the generic adult ones";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54594);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new UpdatePrenatalPhenotypesCallback());
    }

    /**
     * Searches for all documents containing prenatal phenotypes and replaces generic measurement terms with their
     * congenital subterms.
     */
    private final class UpdatePrenatalPhenotypesCallback implements XWikiHibernateBaseStore.HibernateCallback<Object>
    {
        /** Obesity was wrongly used in place of Overweight for a while, and it should be removed. */
        private static final String OBESITY = "HP:0001513";

        /** The names of the properties to fix. */
        private final String[] propertyNames = { "prenatal_phenotype", "negative_prenatal_phenotype" };

        /** Mapping between generic terms that need replacing and their congenital subterms. */
        private final Map<String, String> translations = new HashMap<>();

        private UpdatePrenatalPhenotypesCallback()
        {
            final String congenitalOverweight = "HP:0001520";
            this.translations.put("HP:0004325", "HP:0001518");
            this.translations.put("HP:0004324", congenitalOverweight);
            this.translations.put(OBESITY, congenitalOverweight);
            this.translations.put("HP:0004322", "HP:0003561");
            this.translations.put("HP:0000098", "HP:0003517");
            this.translations.put("HP:0000252", "HP:0011451");
            this.translations.put("HP:0000256", "HP:0004488");
        }

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            Query q = session.createQuery("select distinct o.name from BaseObject o, DBStringListProperty p"
                + " where p.id.id = o.id and o.className = ? and p.id.name in (?, ?)");
            q.setParameter(0, R54594PhenoTips994DataMigration.this.serializer.serialize(Patient.CLASS_REFERENCE));
            q.setParameter(1, this.propertyNames[0]);
            q.setParameter(2, this.propertyNames[1]);
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            R54594PhenoTips994DataMigration.this.logger.debug("Found {} documents with prenatal phenotypes",
                documents.size());
            for (String docName : documents) {
                R54594PhenoTips994DataMigration.this.logger.debug("Checking [{}]", docName);
                XWikiDocument doc =
                    xwiki.getDocument(R54594PhenoTips994DataMigration.this.resolver.resolve(docName), context);

                boolean modified = false;
                for (BaseObject object : doc.getXObjects(Patient.CLASS_REFERENCE)) {
                    if (object == null) {
                        continue;
                    }
                    for (String propertyName : this.propertyNames) {
                        DBStringListProperty property = (DBStringListProperty) object.get(propertyName);
                        DBStringListProperty extendedProperty =
                            (DBStringListProperty) object.get("extended_" + propertyName);
                        if (property == null || property.getList().isEmpty()) {
                            continue;
                        }
                        modified = fixList(property, extendedProperty) || modified;
                    }
                }
                if (!modified) {
                    continue;
                }

                doc.setComment(R54594PhenoTips994DataMigration.this.getDescription());
                doc.setMinorEdit(true);
                try {
                    // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                    // so we must clear the session cache first.
                    session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                    session.flush();
                    R54594PhenoTips994DataMigration.this.logger.debug("Updated [{}]", docName);
                } catch (DataMigrationException e) {
                    // We're in the middle of a migration, we're not expecting another migration
                }
            }
            return null;
        }

        private boolean fixList(DBStringListProperty property, DBStringListProperty extendedProperty)
        {
            boolean modified = false;
            List<String> values = property.getList();
            List<String> extendedValues = null;
            if (extendedProperty != null) {
                extendedValues = extendedProperty.getList();
            }
            for (Map.Entry<String, String> translation : this.translations.entrySet()) {
                if (values.contains(translation.getKey())) {
                    R54594PhenoTips994DataMigration.this.logger.debug(
                        "Replacing {} with {}", translation.getKey(), translation.getValue());
                    values.remove(translation.getKey());
                    if (!values.contains(translation.getValue())) {
                        values.add(translation.getValue());
                        if (extendedValues != null) {
                            extendedValues.add(translation.getValue());
                        }
                    }
                    modified = true;
                }
            }
            if (modified) {
                property.setList(values);
                if (extendedValues != null) {
                    extendedValues.remove(OBESITY);
                    extendedProperty.setList(extendedValues);
                }
            }
            return modified;
        }
    }
}
