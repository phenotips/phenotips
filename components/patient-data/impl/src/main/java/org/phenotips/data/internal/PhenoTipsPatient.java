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
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.entities.internal.AbstractPrimaryEntity;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ListProperty;

/**
 * Implementation of patient data based on the XWiki data model, where patient data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
public class PhenoTipsPatient extends AbstractPrimaryEntity implements Patient
{
    /** The default template for creating a new patient. */
    public static final EntityReference TEMPLATE_REFERENCE = new EntityReference("PatientTemplate",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** used for generating JSON and reading from JSON. */
    protected static final String JSON_KEY_FEATURES = "features";

    protected static final String JSON_KEY_NON_STANDARD_FEATURES = "nonstandard_features";

    protected static final String JSON_KEY_DISORDERS = "disorders";

    protected static final String JSON_KEY_ID = "id";

    protected static final String JSON_KEY_REPORTER = "reporter";

    /** Known phenotype properties. */
    private static final String PHENOTYPE_POSITIVE_PROPERTY = "phenotype";

    private static final String NEGATIVE_PHENOTYPE_PREFIX = "negative_";

    private static final String PRENATAL_PHENOTYPE_PREFIX = "prenatal_";

    private static final String PRENATAL_PHENOTYPE_PROPERTY = PRENATAL_PHENOTYPE_PREFIX + PHENOTYPE_POSITIVE_PROPERTY;

    private static final String PHENOTYPE_NEGATIVE_PROPERTY = NEGATIVE_PHENOTYPE_PREFIX + PHENOTYPE_POSITIVE_PROPERTY;

    private static final String NEGATIVE_PRENATAL_PHENOTYPE_PROPERTY = NEGATIVE_PHENOTYPE_PREFIX
        + PRENATAL_PHENOTYPE_PROPERTY;

    private static final String[] PHENOTYPE_PROPERTIES = new String[] { PHENOTYPE_POSITIVE_PROPERTY,
        PHENOTYPE_NEGATIVE_PROPERTY, PRENATAL_PHENOTYPE_PROPERTY, NEGATIVE_PRENATAL_PHENOTYPE_PROPERTY };

    private static final String DISORDER_PROPERTIES_OMIMID = "omim_id";

    private static final String[] DISORDER_PROPERTIES = new String[] { DISORDER_PROPERTIES_OMIMID };

    /** Logging helper object. */
    private Logger logger = LoggerFactory.getLogger(PhenoTipsPatient.class);

    /** @see #getReporter() */
    private DocumentReference reporter;

    /** @see #getFeatures() */
    private Set<Feature> features = new TreeSet<>();

    /** @see #getDisorders() */
    private Set<Disorder> disorders = new TreeSet<>();

    /** The list of all the initialized data holders (PatientDataSerializer). */
    private Map<String, PatientDataController<?>> serializers = new TreeMap<>();

    /** Extra data that can be plugged into the patient record. */
    private Map<String, PatientData<?>> extraData = new TreeMap<>();

    /**
     * Constructor that copies the data from an XDocument.
     *
     * @param doc the XDocument representing this patient in XWiki
     */
    public PhenoTipsPatient(XWikiDocument doc)
    {
        super(doc);
        this.reporter = doc.getCreatorReference();

        BaseObject data = doc.getXObject(CLASS_REFERENCE);
        if (data == null) {
            return;
        }

        try {
            loadFeatures(doc, data);
            loadDisorders(doc, data);
            loadSerializers();
        } catch (XWikiException ex) {
            this.logger.warn("Failed to access patient data for [{}]: {}", doc.getDocumentReference(), ex.getMessage());
        }

        // Read-only from now on
        this.features = Collections.unmodifiableSet(this.features);
        this.disorders = Collections.unmodifiableSet(this.disorders);

    }

    @Override
    public EntityReference getType()
    {
        return CLASS_REFERENCE;
    }

    private void loadFeatures(XWikiDocument doc, BaseObject data)
    {
        @SuppressWarnings("unchecked")
        Collection<BaseProperty<EntityReference>> fields = data.getFieldList();
        for (BaseProperty<EntityReference> field : fields) {
            if (field == null || !field.getName().matches("(?!extended_)(.*_)?phenotype")
                || !ListProperty.class.isInstance(field)) {
                continue;
            }
            ListProperty values = (ListProperty) field;
            for (String value : values.getList()) {
                if (StringUtils.isNotBlank(value)) {
                    this.features.add(new PhenoTipsFeature(doc, values, value));
                }
            }
        }
    }

    private void loadDisorders(XWikiDocument doc, BaseObject data) throws XWikiException
    {
        for (String property : DISORDER_PROPERTIES) {
            ListProperty values = (ListProperty) data.get(property);
            if (values != null) {
                for (String value : values.getList()) {
                    if (StringUtils.isNotBlank(value)) {
                        this.disorders.add(new PhenoTipsDisorder(values, value));
                    }
                }
            }
        }
    }

    private void loadSerializers()
    {
        try {
            List<PatientDataController<?>> availableSerializers = ComponentManagerRegistry
                .getContextComponentManager()
                .getInstanceList(PatientDataController.class);
            for (PatientDataController<?> serializer : availableSerializers) {
                if (this.serializers.containsKey(serializer.getName())) {
                    this.logger.warn("Overwriting patient data controller with the name [{}]", serializer.getName());
                }
                this.serializers.put(serializer.getName(), serializer);
            }
        } catch (ComponentLookupException ex) {
            this.logger.error("Failed to lookup serializers", ex);
        }
    }

    /**
     * Looks up data controller with the appropriate name and places the controller data in the extraData map.
     */
    private void readPatientData(String name)
    {
        PatientDataController<?> serializer = this.serializers.get(name);
        if (serializer != null) {
            PatientData<?> data = serializer.load(this);
            if (data != null) {
                this.extraData.put(data.getName(), data);
            }
        }
    }

    private boolean isFieldIncluded(Collection<String> selectedFields, String fieldName)
    {
        return (selectedFields == null || selectedFields.contains(fieldName));
    }

    private boolean isFieldIncluded(Collection<String> selectedFields, String[] fieldNames)
    {
        if (selectedFields == null) {
            return true;
        }
        for (String fieldName : fieldNames) {
            if (isFieldIncluded(selectedFields, fieldName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFieldSuffixIncluded(Collection<String> selectedFields, String fieldSuffix)
    {
        if (selectedFields == null) {
            return true;
        }
        for (String fieldName : selectedFields) {
            if (StringUtils.endsWith(fieldName, fieldSuffix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getExternalId()
    {
        try {
            return this.<String>getData("identifiers").get("external_id");
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public DocumentReference getReporter()
    {
        return this.reporter;
    }

    @Override
    public Set<Feature> getFeatures()
    {
        return this.features;
    }

    @Override
    public Set<Disorder> getDisorders()
    {
        return this.disorders;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> PatientData<T> getData(String name)
    {
        if (!this.extraData.containsKey(name)) {
            this.readPatientData(name);
        }
        return (PatientData<T>) this.extraData.get(name);
    }

    @Override
    public JSONObject toJSON()
    {
        return toJSON(null);
    }

    /** creates & returns a new JSON array of all patient features (as JSON objects). */
    private JSONArray featuresToJSON(Collection<String> selectedFields)
    {
        JSONArray featuresJSON = new JSONArray();
        for (Feature phenotype : this.features) {
            if (StringUtils.isBlank(phenotype.getId()) || !isFieldIncluded(selectedFields, phenotype.getType())) {
                continue;
            }
            JSONObject featureJSON = phenotype.toJSON();
            if (featureJSON != null) {
                featuresJSON.put(featureJSON);
            }
        }
        return featuresJSON;
    }

    private JSONArray nonStandardFeaturesToJSON(Collection<String> selectedFields)
    {
        JSONArray featuresJSON = new JSONArray();
        for (Feature phenotype : this.features) {
            if (StringUtils.isNotBlank(phenotype.getId()) || !isFieldIncluded(selectedFields, phenotype.getType())) {
                continue;
            }
            JSONObject featureJSON = phenotype.toJSON();
            if (featureJSON != null) {
                featuresJSON.put(featureJSON);
            }
        }
        return featuresJSON;
    }

    /** creates & returns a new JSON array of all patient diseases (as JSON objects). */
    private JSONArray diseasesToJSON()
    {
        JSONArray diseasesJSON = new JSONArray();
        for (Disorder disease : this.disorders) {
            JSONObject diseaseJSON = disease.toJSON();
            if (diseaseJSON != null) {
                diseasesJSON.put(diseaseJSON);
            }
        }
        return diseasesJSON;
    }

    @Override
    public JSONObject toJSON(Collection<String> selectedFields)
    {
        JSONObject result = new JSONObject();

        if (isFieldIncluded(selectedFields, JSON_KEY_ID)) {
            result.put(JSON_KEY_ID, getDocument().getName());
        }

        if (getReporter() != null && isFieldIncluded(selectedFields, JSON_KEY_REPORTER)) {
            result.put(JSON_KEY_REPORTER, getReporter().getName());
        }

        if (isFieldSuffixIncluded(selectedFields, PHENOTYPE_POSITIVE_PROPERTY)) {
            result.put(JSON_KEY_FEATURES, featuresToJSON(selectedFields));
            result.put(JSON_KEY_NON_STANDARD_FEATURES, nonStandardFeaturesToJSON(selectedFields));
        }

        if (isFieldIncluded(selectedFields, DISORDER_PROPERTIES)) {
            result.put(JSON_KEY_DISORDERS, diseasesToJSON());
        }

        for (PatientDataController<?> serializer : this.serializers.values()) {
            serializer.writeJSON(this, result, selectedFields);
        }

        return result;
    }

    private void updateFeaturesFromJSON(XWikiDocument doc, BaseObject data, XWikiContext context,
        JSONObject json)
    {
        try {
            if (!json.has(JSON_KEY_FEATURES) && !json.has(JSON_KEY_NON_STANDARD_FEATURES)) {
                return;
            }
            JSONArray jsonFeatures =
                joinArrays(json.optJSONArray(JSON_KEY_FEATURES), json.optJSONArray(JSON_KEY_NON_STANDARD_FEATURES));

            // keep this instance of PhenotipsPatient in sync with the document: reset features
            this.features = new TreeSet<>();

            // new feature lists (for setting values in the Wiki document)
            Map<String, List<String>> featuresMap = new TreeMap<>();

            for (int i = 0; i < jsonFeatures.length(); i++) {
                JSONObject featureInJSON = jsonFeatures.optJSONObject(i);
                if (featureInJSON == null) {
                    continue;
                }

                Feature phenotipsFeature = new PhenoTipsFeature(featureInJSON);
                this.features.add(phenotipsFeature);
                String featureType = phenotipsFeature.getType();
                if (!phenotipsFeature.isPresent()) {
                    featureType = NEGATIVE_PHENOTYPE_PREFIX + featureType;
                }

                if (featuresMap.keySet().contains(featureType)) {
                    featuresMap.get(featureType).add(phenotipsFeature.getValue());
                } else {
                    List<String> newFeatureType = new LinkedList<>();
                    newFeatureType.add(phenotipsFeature.getValue());
                    featuresMap.put(featureType, newFeatureType);
                }
            }

            // as in constructor: make unmodifiable
            this.features = Collections.unmodifiableSet(this.features);

            // to reset values in the document null them first
            for (String type : PHENOTYPE_PROPERTIES) {
                data.set(type, null, context);
            }

            for (String type : featuresMap.keySet()) {
                data.set(type, featuresMap.get(type), context);
            }

            // update features' metadata objects in document
            updateMetaData(doc, context);

            // update features' categories objects in document
            updateCategories(doc, context);
        } catch (Exception ex) {
            this.logger.error("Failed to update patient features from JSON [{}]: {}", ex.getMessage(), ex);
        }
    }

    private void updateMetaData(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        doc.removeXObjects(FeatureMetadatum.CLASS_REFERENCE);
        for (Feature feature : this.features) {
            @SuppressWarnings("unchecked")
            Map<String, FeatureMetadatum> metadataMap = (Map<String, FeatureMetadatum>) feature.getMetadata();
            if (metadataMap.isEmpty() && feature.getNotes().isEmpty()) {
                continue;
            }

            BaseObject metaObject = doc.newXObject(FeatureMetadatum.CLASS_REFERENCE, context);
            metaObject.set(PhenoTipsFeature.META_PROPERTY_NAME, feature.getPropertyName(),
                context);
            metaObject.set(PhenoTipsFeature.META_PROPERTY_VALUE, feature.getValue(), context);
            for (String type : metadataMap.keySet()) {
                PhenoTipsFeatureMetadatum metadatum = (PhenoTipsFeatureMetadatum) metadataMap.get(type);
                metaObject.set(type, metadatum.getId(), context);
            }
            metaObject.set("comments", feature.getNotes(), context);
        }
    }

    private void updateCategories(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        doc.removeXObjects(PhenoTipsFeature.CATEGORY_CLASS_REFERENCE);
        for (Feature feature : this.features) {
            List<String> categories = feature.getCategories();
            if (categories.isEmpty()) {
                continue;
            }

            BaseObject categoriesObject = doc.newXObject(PhenoTipsFeature.CATEGORY_CLASS_REFERENCE, context);
            categoriesObject.set(PhenoTipsFeature.META_PROPERTY_NAME, feature.getPropertyName(),
                context);
            categoriesObject.set(PhenoTipsFeature.META_PROPERTY_VALUE, feature.getValue(), context);
            categoriesObject.set(PhenoTipsFeature.META_PROPERTY_CATEGORIES, categories, context);
        }
    }

    private JSONArray joinArrays(JSONArray jsonOne, JSONArray jsonTwo)
    {
        JSONArray result = new JSONArray();
        if (jsonOne == null && jsonTwo != null) {
            result = jsonTwo;
        } else if (jsonOne != null) {
            result = jsonOne;
            if (jsonTwo != null && jsonTwo.length() > 0) {
                for (int i = 0; i < jsonTwo.length(); i++) {
                    result.put(jsonTwo.get(i));
                }
            }
        }
        return result;
    }

    private void updateDisordersFromJSON(XWikiDocument doc, BaseObject data, XWikiContext context, JSONObject json)
    {
        try {
            if (!json.has(JSON_KEY_DISORDERS)) {
                return;
            }
            JSONArray inputDisorders = json.optJSONArray(JSON_KEY_DISORDERS);
            if (inputDisorders != null) {
                // keep this instance of PhenotipsPatient in sync with the document: reset disorders
                this.disorders = new TreeSet<>();

                // new disorders list (for setting values in the Wiki document)
                List<String> disorderValues = new LinkedList<>();

                for (int i = 0; i < inputDisorders.length(); i++) {
                    JSONObject disorderJSON = inputDisorders.optJSONObject(i);
                    if (disorderJSON == null) {
                        continue;
                    }

                    Disorder phenotipsDisorder = new PhenoTipsDisorder(disorderJSON);
                    this.disorders.add(phenotipsDisorder);

                    disorderValues.add(phenotipsDisorder.getValue());
                }

                // as in constructor: make unmofidiable
                this.disorders = Collections.unmodifiableSet(this.disorders);

                data.set(DISORDER_PROPERTIES_OMIMID, null, context);
                // update the values in the document (overwriting the old list, if any)
                data.set(DISORDER_PROPERTIES_OMIMID, disorderValues, context);
            }
        } catch (Exception ex) {
            this.logger.error("Failed to update patient disorders from JSON [{}]: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        if (json.length() == 0) {
            return;
        }

        try {
            // TODO: Check versions and throw if versions mismatch if necessary

            XWikiContext context = getXContext();

            BaseObject data = this.document.getXObject(CLASS_REFERENCE);
            if (data == null) {
                return;
            }

            updateFeaturesFromJSON(this.document, data, context, json);
            updateDisordersFromJSON(this.document, data, context, json);

            for (PatientDataController<?> serializer : this.serializers.values()) {
                try {
                    PatientData<?> patientData = serializer.readJSON(json);
                    if (patientData != null) {
                        this.extraData.put(patientData.getName(), patientData);
                        serializer.save(this, this.document);
                        this.logger.info("Successfully updated patient form JSON using serializer [{}]",
                            serializer.getName());
                    }
                } catch (UnsupportedOperationException ex) {
                    this.logger.debug("Unable to update patient from JSON using serializer [{}]: not supported",
                        serializer.getName());
                } catch (Exception ex) {
                    this.logger.error("Failed to update patient data from JSON using serializer [{}]: {}",
                        serializer.getName(), ex.getMessage(), ex);
                }
            }

            context.getWiki().saveDocument(this.document, "Updated from JSON", true, context);
        } catch (Exception ex) {
            this.logger.error("Failed to update patient data from JSON [{}]: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public String toString()
    {
        return toJSON().toString(2);
    }
}
