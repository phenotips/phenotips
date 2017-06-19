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
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.internal.PhenoTipsFeature;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    /**
     * used for generating JSON and reading from JSON.
     */
    private static final String JSON_KEY_FEATURES = "features";

    private static final String JSON_KEY_NON_STANDARD_FEATURES = "nonstandard_features";

    private static final String CONTROLLER_NAME = JSON_KEY_FEATURES;

    /**
     * Known phenotype properties.
     */
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

    /**
     * Provides access to the current execution context.
     */
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
            BaseObject data = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }

            List<Feature> features = new ArrayList<>();

            Collection<BaseProperty<EntityReference>> fields = data.getFieldList();
            for (BaseProperty<EntityReference> field : fields) {
                if (field == null || !field.getName().matches("(?!extended_)(.*_)?phenotype")
                    || !ListProperty.class.isInstance(field))
                {
                    continue;
                }
                ListProperty values = (ListProperty) field;
                for (String value : values.getList()) {
                    if (StringUtils.isNotBlank(value)) {
                        features.add(new PhenoTipsFeature(patient.getXDocument(), values, value));
                    }
                }
            }
            if (features.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<>(getName(), features);
            }
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
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

    /**
     * creates & returns a new JSON array of all patient features (as JSON objects).
     */
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
                if (StringUtils.isNotBlank(phenotype.getId())
                    || !isFieldIncluded(selectedFields, phenotype.getType()))
                {
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
        if (json == null || !json.has(JSON_KEY_FEATURES) && !json.has(JSON_KEY_NON_STANDARD_FEATURES)) {
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
    public void save(Patient patient)
    {
        save(patient, PatientWritePolicy.UPDATE);
    }

    @Override
    public void save(@Nonnull final Patient patient, @Nonnull final PatientWritePolicy policy)
    {
        try {
            final XWikiDocument docX = patient.getXDocument();
            final XWikiContext context = this.xcontextProvider.get();
            final BaseObject dataHolder = docX.getXObject(Patient.CLASS_REFERENCE, true, context);
            final PatientData<Feature> features = patient.getData(getName());
            if (features == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    clearFeatureData(docX, dataHolder, context);
                }
            } else {
                if (!features.isIndexed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveFeatures(docX, dataHolder, patient, features, policy, context);
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save features data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Saves specified {@code features} for {@code patient} according to the provided {@code policy}.
     *
     * @param docX the {@link XWikiDocument} object for the {@code patient}
     * @param dataHolder the {@link BaseObject} for writing features
     * @param patient the {@link Patient} of interest
     * @param features a {@link PatientData} object containing feature data to be saved
     * @param policy the {@link PatientWritePolicy} according to which data will be saved
     * @param context the {@link XWikiContext}
     */
    private void saveFeatures(
        @Nonnull final XWikiDocument docX,
        @Nonnull final BaseObject dataHolder,
        @Nonnull final Patient patient,
        @Nonnull final PatientData<Feature> features,
        @Nonnull final PatientWritePolicy policy,
        @Nonnull final XWikiContext context)
    {
        final Stream<Feature> featureStream = PatientWritePolicy.MERGE.equals(policy)
            ? buildFeaturesStream(load(patient), features)
            : StreamSupport.stream(features.spliterator(), false);

        clearFeatureData(docX, dataHolder, context);

        featureStream
            .map(feature -> saveFeatureData(docX, feature, context))
            .collect(Collectors.groupingBy(this::getFeatureType,
                Collectors.mapping(Feature::getValue, Collectors.toList()))
            ).forEach(
                (type, ids) -> dataHolder.set(type, ids, context)
            );
    }

    /**
     * Gets the type from the provided {@code feature}.
     *
     * @param feature the {@link Feature} object of interest
     * @return type as string
     */
    private String getFeatureType(@Nonnull final Feature feature)
    {
        return feature.isPresent() ? feature.getType() : this.addNegativePrefix(feature);
    }

    /**
     * Builds a map of {@link Feature} ID to {@link Feature} from {@code storedFeatures stored feature} and newly
     * entered {@code features} data.
     *
     * @param storedFeatures stored {@link PatientData} features data
     * @param features newly added {@link PatientData} features data
     * @return a merged map of feature ID to {@link Feature}
     */
    private Stream<Feature> buildFeaturesStream(
        @Nullable final PatientData<Feature> storedFeatures,
        @Nonnull final PatientData<Feature> features)
    {
        return storedFeatures == null
            ? StreamSupport.stream(features.spliterator(), false)
            : Stream.of(storedFeatures, features)
                .flatMap(s -> StreamSupport.stream(s.spliterator(), false))
                .collect(
                    Collectors.toMap(Feature::getValue, Function.identity(), this::mergeFeatures, LinkedHashMap::new))
                .values()
                .stream();
    }

    /**
     * Clears any stored feature data.
     *
     * @param docX the {@link XWikiDocument}
     * @param dataHolder the {@link BaseObject} holding features data
     * @param context the {@link XWikiContext}
     */
    private void clearFeatureData(
        @Nonnull final XWikiDocument docX,
        @Nonnull final BaseObject dataHolder,
        @Nonnull final XWikiContext context)
    {
        Arrays.stream(PHENOTYPE_PROPERTIES).forEach(type -> dataHolder.set(type, null, context));
        docX.removeXObjects(FeatureMetadatum.CLASS_REFERENCE);
        docX.removeXObjects(PhenoTipsFeature.CATEGORY_CLASS_REFERENCE);
    }

    /**
     * Resolves collisions between features with the same ID, by selecting the newly entered feature.
     *
     * @param oldFeature the {@link Feature} already stored
     * @param newFeature the new {@link Feature}
     * @return the merged {@link Feature} data; in this case this will be new new {@link Feature}
     */
    private Feature mergeFeatures(@Nonnull final Feature oldFeature, @Nonnull final Feature newFeature)
    {
        return newFeature;
    }

    /**
     * Adds the negative phenotype prefix to {@code feature} type.
     *
     * @param feature the {@link Feature} object of interest
     * @return a string of {@link Feature#getType()} prefixed by the negative phenotype prefix
     */
    private String addNegativePrefix(@Nonnull final Feature feature)
    {
        return PhenoTipsFeature.NEGATIVE_PHENOTYPE_PREFIX + feature.getType();
    }

    /**
     * Saves data for provided {@code feature}.
     *
     * @param doc the {@link XWikiDocument} object for the patient
     * @param feature the {@link Feature} of interest
     * @param context the {@link XWikiContext}
     */
    private Feature saveFeatureData(
        @Nonnull final XWikiDocument doc,
        @Nonnull final Feature feature,
        @Nonnull final XWikiContext context)
    {
        try {
            updateMetaData(doc, feature, context);
            updateCategories(doc, feature, context);
        } catch (final Exception e) {
            this.logger.error("Failed to update phenotypes: [{}]", e.getMessage());
        }
        return feature;
    }

    /**
     * Updates metadata for a {@code feature}.
     *
     * @param doc the {@link XWikiDocument} object for the patient
     * @param feature the {@link Feature} of interest
     * @param context the {@link XWikiContext}
     * @throws XWikiException if meta data cannot be updated
     */
    private void updateMetaData(
        @Nonnull final XWikiDocument doc,
        @Nonnull final Feature feature,
        @Nonnull final XWikiContext context) throws XWikiException
    {
        @SuppressWarnings("unchecked") final Map<String, FeatureMetadatum> metadata =
            (Map<String, FeatureMetadatum>) feature.getMetadata();

        if (!metadata.isEmpty() || !feature.getNotes().isEmpty()) {
            final BaseObject metaObject = doc.newXObject(FeatureMetadatum.CLASS_REFERENCE, context);
            metaObject.set(PhenoTipsFeature.META_PROPERTY_NAME, feature.getPropertyName(),
                context);
            metaObject.set(PhenoTipsFeature.META_PROPERTY_VALUE, feature.getValue(), context);
            metadata.forEach((type, metadatum) -> metaObject.set(type, metadatum.getId(), context));
            metaObject.set("comments", feature.getNotes(), context);
        }
    }

    /**
     * Updates categories for a {@code feature}.
     *
     * @param doc the {@link XWikiDocument} object for the patient
     * @param feature the {@link Feature} of interest
     * @param context the {@link XWikiContext}
     * @throws XWikiException if categories cannot be updated
     */
    private void updateCategories(
        @Nonnull final XWikiDocument doc,
        @Nonnull final Feature feature,
        @Nonnull final XWikiContext context) throws XWikiException
    {
        final List<String> categories = feature.getCategories();
        if (!categories.isEmpty()) {
            final BaseObject categoriesObject = doc.newXObject(PhenoTipsFeature.CATEGORY_CLASS_REFERENCE, context);
            categoriesObject.set(PhenoTipsFeature.META_PROPERTY_NAME, feature.getPropertyName(), context);
            categoriesObject.set(PhenoTipsFeature.META_PROPERTY_VALUE, feature.getValue(), context);
            categoriesObject.set(PhenoTipsFeature.META_PROPERTY_CATEGORIES, categories, context);
        }
    }
}
