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
import org.xwiki.text.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
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
 * Migration for PhenoTips issue PT-3292: linking existing cancers to their OncoTree identifiers.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("R71506-PT-3292")
@Singleton
public class R71506PhenoTips3292DataMigration extends AbstractHibernateDataMigration
    implements HibernateCallback<Object>
{
    /** Pedigree XClass that holds pedigree data (image, structure, etc). */
    private static final EntityReference PEDIGREE_CLASS_REFERENCE =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";

    private static final String PEDIGREE_GRAPH_KEY = "GG";

    private static final String PEDIGREE_PROP_FIELD = "prop";

    private static final String PEDIGREE_CANCERS_FIELD = "cancers";

    private static final String PEDIGREE_SETTINGS_FIELD = "settings";

    private static final String LEGEND_SETTINGS_FIELD = "legendSettings";

    private static final String LEGEND_ABNORMALITIES_FIELD = "abnormalities";

    private static final String CANCER_ID_LABEL = "id";

    private static final String CANCER_LABEL_LABEL = "label";

    private static final String AGE_AT_DIAGNOSIS = "ageAtDiagnosis";

    private static final String NUMERIC_AGE_AT_DIAGNOSIS = "numericAgeAtDiagnosis";

    private static final String NOTES = "notes";

    private static final String AFFECTED = "affected";

    private static final String QUALIFIERS = "qualifiers";

    private static final String PRIMARY = "primary";

    private static final Map<String, String> NAME_TO_ID_MAP = buildNameToIdMap();

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public Object doInHibernate(final Session session) throws HibernateException, XWikiException
    {
        // Select all families.
        final Query query = session.createQuery("select distinct o.name from BaseObject o where o.className= '"
            + this.serializer.serialize(PEDIGREE_CLASS_REFERENCE) + "' and o.name <> 'PhenoTips.FamilyTemplate'");

        @SuppressWarnings("unchecked")
        List<String> docs = query.list();

        this.logger.debug("Found {} documents", docs.size());

        final XWikiContext context = getXWikiContext();
        final XWiki xwiki = context.getWiki();
        // Migrate each family.
        docs.forEach(docName -> migrateFamily(docName, session, context, xwiki));
        return null;
    }

    /**
     * Migrates the family's cancer data.
     *
     * @param docName the name of the family {@link XWikiDocument}
     * @param session the current {@link Session}
     * @param context the {@link XWikiContext}
     * @param xwiki the {@link XWiki}
     */
    private void migrateFamily(
        @Nonnull final String docName,
        @Nonnull final Session session,
        @Nonnull final XWikiContext context,
        @Nonnull final XWiki xwiki)
    {
        try {
            final XWikiDocument xDocument = xwiki.getDocument(this.resolver.resolve(docName), context);

            if (xDocument != null) {
                this.logger.debug("Updating pedigree for document {}.", docName);
                final BaseObject pedigreeXObject = xDocument.getXObject(PEDIGREE_CLASS_REFERENCE);
                if (pedigreeXObject != null) {
                    updatePedigree(xDocument, pedigreeXObject, context, session);
                }
            }
        } catch (final JSONException ex) {
            this.logger.error("Error updating JSON for document {} : [{}]", docName, ex.getMessage());
        } catch (final XWikiException ex) {
            this.logger.error("Error obtaining or saving data to document {}: [{}]", docName, ex.getMessage());
        } catch (final DataMigrationException ex) {
            this.logger.error("Error when saving the XWiki document {}: [{}]", docName, ex.getMessage());
        } catch (final Exception ex) {
            this.logger.error("Unexpected migration error on document {}: [{}]", docName, ex.getMessage());
        }
    }

    /**
     * Updates the family pedigree.
     *
     * @param xDocument the {@link XWikiDocument} to update
     * @param pedigreeXObject the pedigree {@link BaseObject}
     * @param context the {@link XWikiContext}
     * @param session the current {@link Session}
     * @throws DataMigrationException if the {@link XWikiHibernateStore} cannot be obtained
     * @throws XWikiException if the {@link XWikiDocument} cannot be saved
     * @throws JSONException if unable to update pedigree JSON
     */
    private void updatePedigree(
        @Nonnull final XWikiDocument xDocument,
        @Nonnull final BaseObject pedigreeXObject,
        @Nonnull final XWikiContext context,
        @Nonnull final Session session)
        throws DataMigrationException, XWikiException, JSONException
    {
        // Try to retrieve stored pedigree data.
        final String storedData = pedigreeXObject.getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        if (StringUtils.isNotBlank(storedData)) {
            // Try to construct a JSONObject from the storedData string.
            final JSONObject pedigree = new JSONObject(storedData);
            // Get the pedigree nodes.
            final JSONArray pedigreeNodes = pedigree.optJSONArray(PEDIGREE_GRAPH_KEY);
            // If there are pedigree nodes, for each node convert cancers to new format. Returns true if pedigree data
            // was modified.
            final boolean modifiedCancers = updatePedigreeNodes(pedigreeNodes);
            // Get the pedigree settings.
            final JSONObject pedigreeSettings = pedigree.optJSONObject(PEDIGREE_SETTINGS_FIELD);
            // If there are pedigree settings for cancers, update cancer names to identifiers. Returns true if settings
            // data was modified.
            final boolean modifiedSettings = updatePedigreeSettings(pedigreeSettings);
            // If pedigree data was modified, save data.
            if (modifiedCancers || modifiedSettings) {
                pedigreeXObject.set(PEDIGREECLASS_JSONDATA_KEY, pedigree.toString(), context);
                saveData(xDocument, session, context);
            }
        }
    }

    /**
     * Updates the data stored in {@code pedigreeNodes pedigree nodes} of a family.
     *
     * @param pedigreeNodes a {@link JSONArray} containing pedigree node data
     * @return true iff pedigree nodes were updated, false otherwise
     */
    private boolean updatePedigreeNodes(@Nullable final JSONArray pedigreeNodes)
    {
        return (pedigreeNodes != null && pedigreeNodes.length() != 0)
            ? StreamSupport.stream(pedigreeNodes.spliterator(), false)
                .map(this::updateCancers)
                .reduce((x, y) -> x || y)
                .orElse(false)
            : false;
    }

    /**
     * Updates the data stored in {@code pedigreeSettings pedigree settings} of a family.
     *
     * @param pedigreeSettings a {@link JSONObject} containing pedigree settings data
     * @return true iff pedigree settings were updated, false otherwise
     */
    private boolean updatePedigreeSettings(@Nullable final JSONObject pedigreeSettings)
    {
        if (pedigreeSettings != null && pedigreeSettings.length() != 0) {
            final JSONObject legendSettings = pedigreeSettings.optJSONObject(LEGEND_SETTINGS_FIELD);
            if (legendSettings == null || legendSettings.length() == 0) {
                return false;
            }

            final JSONObject abnormalities = legendSettings.optJSONObject(LEGEND_ABNORMALITIES_FIELD);
            return abnormalities != null && abnormalities.length() != 0 && updateCancersSettings(abnormalities);
        }
        return false;
    }

    /**
     * Updates {@code pedigreeColours pedigree colours} for cancers.
     *
     * @param abnormalities an {@link JSONObject} abnormalities settings object
     * @return true iff cancer settings were updated, false otherwise
     */
    private boolean updateCancersSettings(@Nonnull final JSONObject abnormalities)
    {
        final JSONObject cancers = abnormalities.optJSONObject(PEDIGREE_CANCERS_FIELD);
        if (cancers == null || cancers.length() == 0) {
            return false;
        }
        final JSONObject updatedCancers = new JSONObject();
        cancers.keys().forEachRemaining(name -> updatedCancers.put(translate(name), cancers.getJSONObject(name)));
        abnormalities.put(PEDIGREE_CANCERS_FIELD, updatedCancers);
        return true;
    }

    /**
     * Saves the updated data in {@code xDocument}.
     *
     * @param xDocument the XWikiDocument containing updated cancer data
     * @param session the {@link Session}
     * @param context the {@link XWikiContext}
     * @throws DataMigrationException if the {@link XWikiHibernateStore} cannot be obtained
     * @throws XWikiException if the {@link XWikiDocument} cannot be saved
     */
    private void saveData(
        @Nonnull final XWikiDocument xDocument,
        @Nonnull final Session session,
        @Nonnull final XWikiContext context)
        throws DataMigrationException, XWikiException
    {
        xDocument.setComment(this.getDescription());
        xDocument.setMinorEdit(true);
        // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
        // so we must clear the session cache first.
        session.clear();
        ((XWikiHibernateStore) getStore()).saveXWikiDoc(xDocument, context, false);
        session.flush();
    }

    /**
     * Converts the cancer data stored {@code node} to the updated format.
     *
     * @param node the pedigree node; should be a JSONObject
     * @return true iff any cancers were converted, false otherwise
     */
    private boolean updateCancers(@Nonnull final Object node)
    {
        final JSONObject nodeJSON = (JSONObject) node;
        final JSONObject properties = nodeJSON.optJSONObject(PEDIGREE_PROP_FIELD);
        if (properties == null) {
            return false;
        }
        final JSONObject cancers = properties.optJSONObject(PEDIGREE_CANCERS_FIELD);
        // If cancers is empty, still want to replace {} by [].
        if (cancers == null) {
            return false;
        }

        // Collect cancers data into a JSONArray. JSONArray has no putAll method :(.
        final JSONArray cancersArray = new JSONArray();
        cancers.keys().forEachRemaining(key -> collectIntoJSONArray(key, cancers.getJSONObject(key), cancersArray));
        properties.put(PEDIGREE_CANCERS_FIELD, cancersArray);
        return true;
    }

    /**
     * Using the provided {@code jsonObject} with a cancer data, construct a {@link JSONObject} with the updated data
     * format and add it to the {@code cancersJsonArray}. The new cancer data format:
     *
     * "cancers":[
     *   {
     *      "id":"KIDNEY",
     *      "label":"Kidney",
     *      "affected":true,
     *      "qualifiers":[
     *         {
     *            "ageAtDiagnosis":"29",
     *            "numericAgeAtDiagnosis":29,
     *            "laterality":"L",
     *            "primary":true,
     *            "notes":"XXX XXXXX"
     *         }
     *      ]
     *   }
     * ]
     *
     * @param cancerName the preferred name of the cancer
     * @param cancersJson the {@link JSONObject} containing cancer data
     * @param cancersJsonArray the {@link JSONArray} that contains the updated cancer data
     */
    private void collectIntoJSONArray(
        @Nonnull final String cancerName,
        @Nonnull final JSONObject cancersJson,
        @Nonnull final JSONArray cancersJsonArray)
    {
        if (cancersJson.length() != 0) {
            // Construct the qualifiers data from the provided jsonObject.
            final JSONObject qualifiers = new JSONObject();
            qualifiers.put(NUMERIC_AGE_AT_DIAGNOSIS, cancersJson.optInt(NUMERIC_AGE_AT_DIAGNOSIS, 0));
            qualifiers.put(AGE_AT_DIAGNOSIS, cancersJson.optString(AGE_AT_DIAGNOSIS, StringUtils.EMPTY));
            qualifiers.put(PRIMARY, true);
            qualifiers.put(NOTES, cancersJson.optString(NOTES, StringUtils.EMPTY));
            // Construct the updated cancer JSON.
            final JSONObject newCancerJson = new JSONObject();
            final String cancerId = translate(cancerName);
            newCancerJson.put(CANCER_ID_LABEL, cancerId);
            newCancerJson.put(CANCER_LABEL_LABEL, cancerName);
            newCancerJson.put(AFFECTED, cancersJson.getBoolean(AFFECTED));
            newCancerJson.put(QUALIFIERS, new JSONArray().put(qualifiers));
            // Add the cancer to the collection of updated cancers.
            cancersJsonArray.put(newCancerJson);
        }
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public String getDescription()
    {
        return "Migrate pedigree data (link cancers to their HPO identifiers)";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71506);
    }

    /**
     * Retrieves a cancer identifier, based on the provided cancer name. The cancer name should not be null.
     *
     * @param name the name of the cancer; must not be null
     * @return the identifier for the provided cancer {@code name}, or {@code name} if no matching identifier exists
     */
    @Nonnull
    private String translate(@Nonnull final String name)
    {
        return NAME_TO_ID_MAP.getOrDefault(name, name);
    }

    /**
     * Builds a map of currently stored cancer name, to its corresponding indexed vocabulary identifier.
     *
     * @return the requested cancer's identifier
     */
    private static Map<String, String> buildNameToIdMap()
    {
        final Map<String, String> nameToIdMap = new HashMap<>();
        nameToIdMap.put("Breast", "HP:0100013");
        nameToIdMap.put("Ovarian", "HP:0100615");
        nameToIdMap.put("Colon", "HP:0100273");
        nameToIdMap.put("Uterus", "HP:0010784");
        nameToIdMap.put("Prostate", "HP:0100787");
        nameToIdMap.put("Pancreatic", "HP:0002894");
        nameToIdMap.put("Melanoma", "HP:0012056");
        nameToIdMap.put("Kidney", "HP:0009726");
        nameToIdMap.put("Gastric", "HP:0006753");
        nameToIdMap.put("Lung", "HP:0100526");
        nameToIdMap.put("Brain", "HP:0030692");
        nameToIdMap.put("Oesophagus", "HP:0100751");
        nameToIdMap.put("Thyroid", "HP:0100031");
        nameToIdMap.put("Liver", "HP:0002896");
        nameToIdMap.put("Cervix", "HP:0030079");
        nameToIdMap.put("Myeloma", "HP:0006775");
        nameToIdMap.put("Leukemia", "HP:0001909");
        return nameToIdMap;
    }
}
