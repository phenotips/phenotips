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

import org.phenotips.data.Patient;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
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
 * Migration for PhenoTips issue #1151: Replace non-HPO custom terms from the detailed phenotype mapping with the
 * equivalent new HPO terms.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("R54595PhenoTips#1151")
@Singleton
public final class R54595PhenoTips1151DataMigration extends AbstractHibernateDataMigration
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

    @Inject
    private VocabularyManager vocabularies;

    @Override
    public String getDescription()
    {
        return "Replace non-HPO custom terms from the detailed phenotype mapping with the equivalent new HPO terms";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54595);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new UpdateCustomPhenotypesCallback());
    }

    /** Searches for all documents containing custom phenotypes and replaces them with the equivalent new HPO terms. */
    private final class UpdateCustomPhenotypesCallback implements XWikiHibernateBaseStore.HibernateCallback<Object>
    {
        /** The names of the properties to fix. */
        private final String[] propertyNames = { "phenotype", "negative_phenotype" };

        /** Mapping between non-HPO custom terms that need replacing and their equivalent official HPO terms. */
        private final Map<String, String> translations = new HashMap<>();

        private UpdateCustomPhenotypesCallback()
        {
            this.translations.put("_c_high_posterior_hairline", "HP:0012891");
            this.translations.put("_c_euryblepharon", "HP:0012905");
            this.translations.put("_c_broad_chest", "HP:0000914");
            this.translations.put("_c_posterior_anus", "HP:0012890");
            this.translations.put("_c_decreased_rom", "HP:0001376");
            this.translations.put("_c_sacral_sinus", "HP:0000960");
            this.translations.put("_c_skin_cals", "HP:0000957");
            this.translations.put("_c_peringuinal_fibroma", "HP:0100804");
        }

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            Query q =
                session.createQuery("select distinct o.name from BaseObject o, DBStringListProperty p join p.list as i"
                    + " where p.id.id = o.id and o.className = ? and p.id.name in (?, ?) and i in ("
                    + StringUtils.removeEnd(StringUtils.repeat("?, ", this.translations.size()), ", ") + ")");
            q.setParameter(0, R54595PhenoTips1151DataMigration.this.serializer.serialize(Patient.CLASS_REFERENCE));
            q.setParameter(1, this.propertyNames[0]);
            q.setParameter(2, this.propertyNames[1]);
            int i = 2;
            for (String deprecatedValue : this.translations.keySet()) {
                q.setParameter(++i, deprecatedValue);
            }
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            R54595PhenoTips1151DataMigration.this.logger.debug("Found {} documents with custom phenotypes",
                documents.size());
            for (String docName : documents) {
                R54595PhenoTips1151DataMigration.this.logger.debug("Checking [{}]", docName);
                XWikiDocument doc =
                    xwiki.getDocument(R54595PhenoTips1151DataMigration.this.resolver.resolve(docName), context);

                boolean modified = false;
                for (BaseObject object : doc.getXObjects(Patient.CLASS_REFERENCE)) {
                    if (object == null) {
                        continue;
                    }
                    for (String propertyName : this.propertyNames) {
                        DBStringListProperty property = (DBStringListProperty) object.get(propertyName);
                        DBStringListProperty extendedProperty =
                            (DBStringListProperty) object.get("extended_" + propertyName);
                        modified = fixList(property, extendedProperty) || modified;
                    }
                }
                if (!modified) {
                    continue;
                }

                doc.setComment(R54595PhenoTips1151DataMigration.this.getDescription());
                doc.setMinorEdit(true);
                try {
                    // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                    // so we must clear the session cache first.
                    session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                    session.flush();
                    R54595PhenoTips1151DataMigration.this.logger.debug("Updated [{}]", docName);
                } catch (DataMigrationException e) {
                    // We're in the middle of a migration, we're not expecting another migration
                }
            }
            return null;
        }

        private boolean fixList(DBStringListProperty property, DBStringListProperty extendedProperty)
        {
            if (property == null) {
                return false;
            }
            boolean modified = false;
            List<String> values = property.getList();
            List<String> extendedValues = null;
            if (extendedProperty != null) {
                extendedValues = extendedProperty.getList();
            }
            for (Map.Entry<String, String> translation : this.translations.entrySet()) {
                if (values.contains(translation.getKey())) {
                    R54595PhenoTips1151DataMigration.this.logger.debug(
                        "Replacing {} with {}", translation.getKey(), translation.getValue());
                    values.remove(translation.getKey());
                    values.add(translation.getValue());
                    if (extendedValues != null) {
                        extendedValues.remove(translation.getKey());
                        VocabularyTerm newTerm =
                            R54595PhenoTips1151DataMigration.this.vocabularies.resolveTerm(translation.getValue());
                        for (VocabularyTerm ancestor : newTerm.getAncestorsAndSelf()) {
                            if (!extendedValues.contains(ancestor.getId())) {
                                extendedValues.add(ancestor.getId());
                            }
                        }
                    }
                    modified = true;
                }
            }
            if (modified) {
                property.setList(values);
                if (extendedValues != null) {
                    extendedProperty.setList(extendedValues);
                }
            }
            return modified;
        }
    }
}
