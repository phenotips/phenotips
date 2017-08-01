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

package org.phenotips.studies.family.migrations;

import org.phenotips.Constants;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONObject;
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

/**
 * Migration for PhenoTips issue PT-1393: Improve pedigree node colouring and legend.
 *
 * @version $Id$
 * @since 1.4M1
 */
@Component
@Named("R71503-PT-1393")
@Singleton
public class R71503PhenoTips1393DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    /**
     * Pedigree XClass that holds pedigree data (image, structure, etc).
     */
    private static final EntityReference PEDIGREE_CLASS_REFERENCE =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";

    private static final String PEDIGREE_SETTINGS_KEY = "settings";

    private static final String PEDIGREE_LEGEND_SETTINGS_KEY = "legendSettings";

    private static final String PEDIGREE_LEGEND_PREFERENCES_KEY = "preferences";

    private static final String PEDIGREE_LEGEND_ABNORMALITIES_KEY = "abnormalities";

    private static final String PEDIGREE_SETTINGS_COLORS_KEY = "colors";

    private static final String PEDIGREE_SETTINGS_NAMES_KEY = "names";

    private static final String SETTINGS_ABNORMAITY_COLOR = "color";

    private static final String SETTINGS_ABNORMAITY_NAME = "name";

    private static final String SETTINGS_ABNORMAITY_PROPERTIES = "properties";

    private static final List<String> LEGEND_ABNORMALITY_TYPES =
            Arrays.asList("disorders", "candidateGenes", "causalGenes", "carrierGenes", "phenotypes", "cancers");

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
        return "Update format of pedigree legend";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71503);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Select all families
        Query q = session.createQuery("select distinct o.name from BaseObject o where o.className = '"
            + this.serializer.serialize(PEDIGREE_CLASS_REFERENCE)
            + "' and o.name <> 'PhenoTips.FamilyTemplate'");

        @SuppressWarnings("unchecked")
        List<String> docs = q.list();

        this.logger.debug("Found {} documents", docs.size());

        for (String docName : docs) {
            XWikiDocument xDocument;

            try {
                xDocument = xwiki.getDocument(this.resolver.resolve(docName), context);
                if (xDocument == null) {
                    continue;
                }

                BaseObject pedigreeXObject = xDocument.getXObject(PEDIGREE_CLASS_REFERENCE);
                if (pedigreeXObject == null) {
                    continue;
                }

                this.logger.debug("Updating pedigree for document {}.", docName);

                this.updatePedigreeLegendSetting(pedigreeXObject, context, docName);

                xDocument.setComment(this.getDescription());
                xDocument.setMinorEdit(true);

            } catch (Exception e) {
                this.logger.error("Error updating pedigree legend settings for document {}: [{}]",
                        docName, e.getMessage());
                continue;
            }

            try {
                // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                // so we must clear the session cache first.
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(xDocument, context, false);
                session.flush();
            } catch (DataMigrationException e) {
                //
            }

        }
        return null;
    }

    private void updatePedigreeLegendSetting(BaseObject pedigreeXObject, XWikiContext context, String documentName)
    {
        try {
            String dataText = pedigreeXObject.getStringValue(PEDIGREECLASS_JSONDATA_KEY);
            if (StringUtils.isEmpty(dataText)) {
                return;
            }

            JSONObject pedigree = new JSONObject(dataText);

            JSONObject pedigreeSettings = pedigree.optJSONObject(PEDIGREE_SETTINGS_KEY);
            if (pedigreeSettings != null) {

                JSONObject oldColorSettings = pedigreeSettings.optJSONObject(PEDIGREE_SETTINGS_COLORS_KEY);
                JSONObject oldNamesSettings = pedigreeSettings.optJSONObject(PEDIGREE_SETTINGS_NAMES_KEY);

                JSONObject legendSettings = new JSONObject();

                // only one style is suported in current implementation, thus it is hardcoded
                JSONObject legendPreferences = new JSONObject("{\"style\": \"multisector\"}");
                legendSettings.put(PEDIGREE_LEGEND_PREFERENCES_KEY, legendPreferences);

                JSONObject legenedAbnormalities = new JSONObject();
                for (String type : LEGEND_ABNORMALITY_TYPES)
                {
                    this.updateOneType(oldColorSettings, oldNamesSettings, legenedAbnormalities, type);
                }
                legendSettings.put(PEDIGREE_LEGEND_ABNORMALITIES_KEY, legenedAbnormalities);

                JSONObject newPedigreeSettings = new JSONObject();
                newPedigreeSettings.put(PEDIGREE_LEGEND_SETTINGS_KEY, legendSettings);
                pedigree.put(PEDIGREE_SETTINGS_KEY, newPedigreeSettings);
                String pedigreeData = pedigree.toString();
                pedigreeXObject.set(PEDIGREECLASS_JSONDATA_KEY, pedigreeData, context);
            }
        } catch (Exception e) {
            this.logger.error("Error updating JSON for docuemnt {}: [{}]", documentName, e);
        }
    }

    private void updateOneType(JSONObject oldColorSettings, JSONObject oldNamesSettings,
            JSONObject newLegenedAbnormalities, String abnormalityName)
    {
        JSONObject abnormalitySettings = new JSONObject();

        if (oldColorSettings != null && oldColorSettings.has(abnormalityName)) {
            JSONObject oldSettings = oldColorSettings.optJSONObject(abnormalityName);
            if (oldSettings != null) {
                for (String key : oldSettings.keySet()) {
                    JSONObject element = new JSONObject();
                    element.put(SETTINGS_ABNORMAITY_COLOR, oldSettings.getString(key));
                    element.put(SETTINGS_ABNORMAITY_PROPERTIES, new JSONObject("{\"enabled\": true}"));
                    element.put(SETTINGS_ABNORMAITY_NAME, key);
                    abnormalitySettings.put(key, element);
                }
            }
        }

        if (oldNamesSettings != null && oldNamesSettings.has(abnormalityName)) {
            JSONObject oldSettings = oldNamesSettings.optJSONObject(abnormalityName);
            if (oldSettings != null) {
                for (String key : oldSettings.keySet()) {
                    JSONObject element = abnormalitySettings.optJSONObject(key);
                    if (element != null) {
                        element.put(SETTINGS_ABNORMAITY_NAME, oldSettings.getString(key));
                        abnormalitySettings.put(key, element);
                    }
                }
            }
        }

        newLegenedAbnormalities.put(abnormalityName, abnormalitySettings);
    }
}
