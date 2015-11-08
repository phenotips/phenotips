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
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ListProperty;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Implementation of patient data based on the XWiki data model, where patient data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
public class PhenoTipsPatient implements Patient
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

    private static final String PHENOTYPE_NEGATIVE_PROPERTY = "negative_phenotype";

    private static final String[] PHENOTYPE_PROPERTIES =
        new String[] { PHENOTYPE_POSITIVE_PROPERTY, PHENOTYPE_NEGATIVE_PROPERTY };

    private static final String DISORDER_PROPERTIES_OMIMID = "omim_id";

    private static final String[] DISORDER_PROPERTIES = new String[] { DISORDER_PROPERTIES_OMIMID };

    /** Logging helper object. */
    private Logger logger = LoggerFactory.getLogger(PhenoTipsPatient.class);

    /** @see #getDocument() */
    private DocumentReference document;

    /** @see #getReporter() */
    private DocumentReference reporter;

    /** @see #getFeatures() */
    private Set<Feature> features = new TreeSet<Feature>();

    /** @see #getDisorders() */
    private Set<Disorder> disorders = new TreeSet<Disorder>();

    /** The list of all the initialized data holders (PatientDataSerializer). */
    private Map<String, PatientDataController<?>> serializers = new HashMap<String, PatientDataController<?>>();

    /** Extra data that can be plugged into the patient record. */
    private Map<String, PatientData<?>> extraData = new TreeMap<String, PatientData<?>>();

    /**
     * Constructor that copies the data from an XDocument.
     *
     * @param doc the XDocument representing this patient in XWiki
     */
    public PhenoTipsPatient(XWikiDocument doc)
    {
        this.document = doc.getDocumentReference();
        this.reporter = doc.getCreatorReference();

        BaseObject data = doc.getXObject(CLASS_REFERENCE);
        if (data == null) {
            return;
        }

        try {
            loadFeatures(doc, data);
            loadDisorders(doc, data);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to access patient data for [{}]: {}", doc.getDocumentReference(), ex.getMessage());
        }

        // Read-only from now on
        this.features = Collections.unmodifiableSet(this.features);
        this.disorders = Collections.unmodifiableSet(this.disorders);

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
                if (serializers.containsKey(serializer.getName())) {
                    this.logger.warn("Overwriting patient data controller with the name [{}]", serializer.getName());
                }
                this.serializers.put(serializer.getName(), serializer);
            }
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to find component", e);
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

    private boolean isFieldIncluded(Collection<String> includedFieldNames, String fieldName)
    {
        return (includedFieldNames == null || includedFieldNames.contains(fieldName));
    }

    private boolean isFieldIncluded(Collection<String> includedFieldNames, String[] fieldNames)
    {
        if (includedFieldNames == null) {
            return true;
        }
        for (String fieldName : fieldNames) {
            if (isFieldIncluded(includedFieldNames, fieldName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getId()
    {
        return this.document.getName();
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
    public DocumentReference getDocument()
    {
        return this.document;
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
        //Patient data is lazy loaded on request. Note that calling toString() or toJSON() causes all data to be loaded.
        if (serializers.isEmpty()) {
            loadSerializers();
        }

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
    private JSONArray featuresToJSON()
    {
        JSONArray featuresJSON = new JSONArray();
        for (Feature phenotype : this.features) {
            if (StringUtils.isBlank(phenotype.getId())) {
                continue;
            }
            featuresJSON.add(phenotype.toJSON());
        }
        return featuresJSON;
    }

    private JSONArray nonStandardFeaturesToJSON()
    {
        JSONArray featuresJSON = new JSONArray();
        for (Feature phenotype : this.features) {
            if (StringUtils.isNotBlank(phenotype.getId())) {
                continue;
            }
            featuresJSON.add(phenotype.toJSON());
        }
        return featuresJSON;
    }

    /** creates & returns a new JSON array of all patient diseases (as JSON objects). */
    private JSONArray diseasesToJSON()
    {
        JSONArray diseasesJSON = new JSONArray();
        for (Disorder disease : this.disorders) {
            diseasesJSON.add(disease.toJSON());
        }
        return diseasesJSON;
    }

    @Override
    public JSONObject toJSON(Collection<String> onlyFieldNames)
    {
        JSONObject result = new JSONObject();

        if (isFieldIncluded(onlyFieldNames, JSON_KEY_ID)) {
            result.element(JSON_KEY_ID, getDocument().getName());
        }

        if (getReporter() != null && isFieldIncluded(onlyFieldNames, JSON_KEY_REPORTER)) {
            result.element(JSON_KEY_REPORTER, getReporter().getName());
        }

        if (!this.features.isEmpty() && isFieldIncluded(onlyFieldNames, PHENOTYPE_PROPERTIES)) {
            result.element(JSON_KEY_FEATURES, featuresToJSON());
            result.element(JSON_KEY_NON_STANDARD_FEATURES, nonStandardFeaturesToJSON());
        }

        if (!this.disorders.isEmpty() && isFieldIncluded(onlyFieldNames, DISORDER_PROPERTIES)) {
            result.element(JSON_KEY_DISORDERS, diseasesToJSON());
        }


        for (PatientDataController<?> serializer: this.serializers.values()) {
            serializer.writeJSON(this, result, onlyFieldNames);
        }

        return result;
    }

    private void updateFeaturesFromJSON(XWikiDocument doc, BaseObject data, XWikiContext context, JSONObject json)
    {
        try {
            JSONArray inputFeatures = json.optJSONArray(JSON_KEY_FEATURES);
            if (inputFeatures != null) {
                // keep this instance of PhenotipsPatient in sync with the document: reset features
                this.features = new TreeSet<Feature>();

                // new feature lists (for setting values in the Wiki document)
                List<String> positiveValues = new LinkedList<String>();
                List<String> negativeValues = new LinkedList<String>();

                for (int i = 0; i < inputFeatures.size(); i++) {
                    JSONObject featureInJSON = inputFeatures.optJSONObject(i);
                    if (featureInJSON == null) {
                        continue;
                    }

                    Feature phenotipsFeature = new PhenoTipsFeature(featureInJSON);
                    this.features.add(phenotipsFeature);

                    if (phenotipsFeature.isPresent()) {
                        positiveValues.add(phenotipsFeature.getValue());
                    } else {
                        negativeValues.add(phenotipsFeature.getValue());
                    }
                }

                // as in constructor: make unmodifiable
                this.features = Collections.unmodifiableSet(this.features);

                // update the values in the document (overwriting the old list, if any)
                data.set(PHENOTYPE_POSITIVE_PROPERTY, positiveValues, context);
                data.set(PHENOTYPE_NEGATIVE_PROPERTY, negativeValues, context);
                context.getWiki().saveDocument(doc, "Updated features from JSON", true, context);
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to update patient features from JSON [{}]: {}", ex.getMessage(), ex);
        }
    }

    private void updateDisordersFromJSON(XWikiDocument doc, BaseObject data, XWikiContext context, JSONObject json)
    {
        try {
            JSONArray inputDisorders = json.optJSONArray(JSON_KEY_DISORDERS);
            if (inputDisorders != null) {
                // keep this instance of PhenotipsPatient in sync with the document: reset disorders
                this.disorders = new TreeSet<Disorder>();

                // new disorders list (for setting values in the Wiki document)
                List<String> disorderValues = new LinkedList<String>();

                for (int i = 0; i < inputDisorders.size(); i++) {
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

                // update the values in the document (overwriting the old list, if any)
                data.set(DISORDER_PROPERTIES_OMIMID, disorderValues, context);
                context.getWiki().saveDocument(doc, "Updated disorders from JSON", true, context);
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to update patient disorders from JSON [{}]: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        try {
            // TODO: Check versions and throw if versions mismatch if necessary
            // TODO: Separate updateFromJSON and saveToDB? Move to PatientRepository?

            Execution execution = ComponentManagerRegistry.getContextComponentManager().getInstance(Execution.class);
            XWikiContext context = (XWikiContext) execution.getContext().getProperty("xwikicontext");

            DocumentAccessBridge documentAccessBridge =
                ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
            XWikiDocument doc = (XWikiDocument) documentAccessBridge.getDocument(getDocument());

            BaseObject data = doc.getXObject(CLASS_REFERENCE);
            if (data == null) {
                return;
            }

            updateFeaturesFromJSON(doc, data, context, json);

            updateDisordersFromJSON(doc, data, context, json);

            for (PatientDataController<?> serializer : this.serializers.values()) {
                try {
                    PatientData<?> patientData = serializer.readJSON(json);
                    if (patientData != null) {
                        this.extraData.put(patientData.getName(), patientData);
                        serializer.save(this);
                        this.logger.info("Successfully updated patient form JSON using serializer [{}]",
                            serializer.getName());
                    }
                } catch (UnsupportedOperationException ex) {
                    this.logger.info("Unable to update patient from JSON using serializer [{}]: not supported",
                        serializer.getName());
                } catch (Exception ex) {
                    this.logger.warn("Failed to update patient data from JSON using serializer [{}]: {}",
                        serializer.getName(), ex.getMessage(), ex);
                }
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to update patient data from JSON [{}]: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public String toString()
    {
        return toJSON().toString(2);
    }
}
