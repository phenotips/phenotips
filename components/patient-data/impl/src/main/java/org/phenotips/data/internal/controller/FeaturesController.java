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
package org.phenotips.data.internal.controller;

import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.internal.PhenoTipsFeature;
import org.phenotips.data.internal.PhenoTipsFeatureMetadatum;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ListProperty;

/**
 * Handles the patients features.
 *
 * @version $Id$
 * @since 1.3RC1
 */
@Component(roles = { PatientDataController.class })
@Named("features")
@Singleton
public class FeaturesController extends AbstractComplexController<Feature>
{
    /** used for generating JSON and reading from JSON. */
    private static final String JSON_KEY_FEATURES = "features";

    private static final String JSON_KEY_NON_STANDARD_FEATURES = "nonstandard_features";

    private static final String CONTROLLER_NAME = JSON_KEY_FEATURES;

    /** Known phenotype properties. */
    private static final String PHENOTYPE_POSITIVE_PROPERTY = "phenotype";

    private static final String PRENATAL_PHENOTYPE_PREFIX = "prenatal_";

    private static final String PRENATAL_PHENOTYPE_PROPERTY = PRENATAL_PHENOTYPE_PREFIX + PHENOTYPE_POSITIVE_PROPERTY;

    private static final String PHENOTYPE_NEGATIVE_PROPERTY = PhenoTipsFeature.NEGATIVE_PHENOTYPE_PREFIX
        + PHENOTYPE_POSITIVE_PROPERTY;

    private static final String NEGATIVE_PRENATAL_PHENOTYPE_PROPERTY = PhenoTipsFeature.NEGATIVE_PHENOTYPE_PREFIX
        + PRENATAL_PHENOTYPE_PROPERTY;

    private static final String[] PHENOTYPE_PROPERTIES = new String[] { PHENOTYPE_POSITIVE_PROPERTY,
    PHENOTYPE_NEGATIVE_PROPERTY, PRENATAL_PHENOTYPE_PROPERTY, NEGATIVE_PRENATAL_PHENOTYPE_PROPERTY };

    @Inject
    private Logger logger;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected String getJsonPropertyName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected List<String> getProperties()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getBooleanFields()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getCodeFields()
    {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public IndexedPatientData<Feature> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }

            List<Feature> features = new ArrayList<>();

            Collection<BaseProperty<EntityReference>> fields = data.getFieldList();
            for (BaseProperty<EntityReference> field : fields) {
                if (field == null || !field.getName().matches("(?!extended_)(.*_)?phenotype")
                    || !ListProperty.class.isInstance(field)) {
                    continue;
                }
                ListProperty values = (ListProperty) field;
                for (String value : values.getList()) {
                    if (StringUtils.isNotBlank(value)) {
                        features.add(new PhenoTipsFeature(doc, values, value));
                    }
                }
            }
            if (features.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<>(getName(), features);
            }
        } catch (Exception e) {
            this.logger.error("Failed to access patient data for [{}]: {}", patient.getDocument(), e.getMessage());
        }
        return null;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !isFieldSuffixIncluded(selectedFieldNames, PHENOTYPE_POSITIVE_PROPERTY)) {
            return;
        }

        PatientData<Feature> data = patient.getData(getName());
        json.put(JSON_KEY_FEATURES, featuresToJSON(data, selectedFieldNames));
        json.put(JSON_KEY_NON_STANDARD_FEATURES, nonStandardFeaturesToJSON(data, selectedFieldNames));
    }

    /** creates & returns a new JSON array of all patient features (as JSON objects). */
    private JSONArray featuresToJSON(PatientData<Feature> data, Collection<String> selectedFields)
    {
        JSONArray featuresJSON = new JSONArray();
        if (data != null) {
            Iterator<Feature> iterator = data.iterator();
            while (iterator.hasNext()) {
                Feature phenotype = iterator.next();
                if (StringUtils.isBlank(phenotype.getId()) || !isFieldIncluded(selectedFields, phenotype.getType())) {
                    continue;
                }
                JSONObject featureJSON = phenotype.toJSON();
                if (featureJSON != null) {
                    featuresJSON.put(featureJSON);
                }
            }
        }
        return featuresJSON;
    }

    private JSONArray nonStandardFeaturesToJSON(PatientData<Feature> data, Collection<String> selectedFields)
    {
        JSONArray featuresJSON = new JSONArray();
        if (data != null) {
            Iterator<Feature> iterator = data.iterator();
            while (iterator.hasNext()) {
                Feature phenotype = iterator.next();
                if (StringUtils.isNotBlank(phenotype.getId()) || !isFieldIncluded(selectedFields, phenotype.getType())) {
                    continue;
                }
                JSONObject featureJSON = phenotype.toJSON();
                if (featureJSON != null) {
                    featuresJSON.put(featureJSON);
                }
            }
        }
        return featuresJSON;
    }

    private boolean isFieldIncluded(Collection<String> selectedFields, String fieldName)
    {
        return (selectedFields == null || selectedFields.contains(fieldName));
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
    public PatientData<Feature> readJSON(JSONObject json)
    {
        if (json == null || !json.has(JSON_KEY_FEATURES) || !json.has(JSON_KEY_NON_STANDARD_FEATURES)) {
            return null;
        }

        try {
            JSONArray jsonFeatures =
                joinArrays(json.optJSONArray(JSON_KEY_FEATURES), json.optJSONArray(JSON_KEY_NON_STANDARD_FEATURES));

            // keep this instance of PhenotipsPatient in sync with the document: reset features
            List<Feature> features = new ArrayList<>();

            for (int i = 0; i < jsonFeatures.length(); i++) {
                JSONObject featureInJSON = jsonFeatures.optJSONObject(i);
                if (featureInJSON == null) {
                    continue;
                }

                Feature phenotipsFeature = new PhenoTipsFeature(featureInJSON);
                features.add(phenotipsFeature);
            }
            return new IndexedPatientData<>(getName(), features);
        } catch (Exception e) {
            this.logger.error("Failed to update patient features from JSON: [{}]", e.getMessage(), e);
            return null;
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

    @Override
    public void save(Patient patient, DocumentModelBridge doc)
    {
        PatientData<Feature> features = patient.getData(this.getName());
        if (features == null || !features.isIndexed()) {
            return;
        }

        if (doc == null) {
            throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
        }
        XWikiDocument docX = (XWikiDocument) doc;
        BaseObject data = docX.getXObject(Patient.CLASS_REFERENCE);
        XWikiContext context = this.xcontextProvider.get();

        // new feature lists (for setting values in the Wiki document)
        Map<String, List<String>> featuresMap = new TreeMap<>();
        Iterator<Feature> iterator = features.iterator();
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
            String featureType = feature.getType();
            if (!feature.isPresent()) {
                featureType = PhenoTipsFeature.NEGATIVE_PHENOTYPE_PREFIX + featureType;
            }
            if (featuresMap.keySet().contains(featureType)) {
                featuresMap.get(featureType).add(feature.getValue());
            } else {
                List<String> newFeatureType = new LinkedList<>();
                newFeatureType.add(feature.getValue());
                featuresMap.put(featureType, newFeatureType);
            }
        }

        // to reset values in the document null them first
        for (String type : PHENOTYPE_PROPERTIES) {
            data.set(type, null, context);
        }

        for (String type : featuresMap.keySet()) {
            data.set(type, featuresMap.get(type), context);
        }

        try {
            // update features' metadata objects in document
            updateMetaData(features, docX, context);

            // update features' categories objects in document
            updateCategories(features, docX, context);
        } catch (Exception e) {
            this.logger.error("Failed to update phenotypes: [{}]", e.getMessage());
        }
    }

    private void updateMetaData(PatientData<Feature> features, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        doc.removeXObjects(FeatureMetadatum.CLASS_REFERENCE);
        Iterator<Feature> iterator = features.iterator();
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
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

    private void updateCategories(PatientData<Feature> features, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        doc.removeXObjects(PhenoTipsFeature.CATEGORY_CLASS_REFERENCE);
        Iterator<Feature> iterator = features.iterator();
        while (iterator.hasNext()) {
            Feature feature = iterator.next();
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

}
