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

import org.phenotips.Constants;
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;
import org.xwiki.context.Execution;

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

    /** Known phenotype properties. */
    private static final String PHENOTYPE_POSITIVE_PROPERTY = "phenotype";
    private static final String PHENOTYPE_NEGATIVE_PROPERTY = "negative_phenotype";
    private static final String[] PHENOTYPE_PROPERTIES = new String[]{PHENOTYPE_POSITIVE_PROPERTY, PHENOTYPE_NEGATIVE_PROPERTY};

    private static final String DISEASE_PROPERTIES_OMIMID = "omim_id";
    private static final String[] DISEASE_PROPERTIES = new String[]{DISEASE_PROPERTIES_OMIMID};

    /** used for generating JSON and reading from JSON */
    private static final String JSON_KEY_FEATURES  = "features";
    private static final String JSON_KEY_DISORDERS = "disorders";

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
    private List<PatientDataController<?>> serializers;

    /** Extra data that can be plugged into the patient record. */
    private Map<String, PatientData<?>> extraData = new HashMap<String, PatientData<?>>();

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

        loadSerializers();
        readPatientData();

        try {
            for (String property : PHENOTYPE_PROPERTIES) {
                DBStringListProperty values = (DBStringListProperty) data.get(property);
                if (values != null) {
                    for (String value : values.getList()) {
                        if (StringUtils.isNotBlank(value)) {
                            this.features.add(new PhenoTipsFeature(doc, values, value));
                        }
                    }
                }
            }
            for (String property : DISEASE_PROPERTIES) {
                DBStringListProperty values = (DBStringListProperty) data.get(property);
                if (values != null) {
                    for (String value : values.getList()) {
                        if (StringUtils.isNotBlank(value)) {
                            this.disorders.add(new PhenoTipsDisorder(values, value));
                        }
                    }
                }
            }
        } catch (XWikiException ex) {
            this.logger.warn("Failed to access patient data for [{}]: {}", doc.getDocumentReference(), ex.getMessage(),
                ex);
        }
        // Readonly from now on
        this.features = Collections.unmodifiableSet(this.features);
        this.disorders = Collections.unmodifiableSet(this.disorders);
    }

    private void loadSerializers()
    {
        try {
            this.serializers =
                ComponentManagerRegistry.getContextComponentManager().getInstanceList(PatientDataController.class);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to find component", e);
        }
    }

    /**
     * Loops through all the available serializers and passes each a document reference.
     */
    private void readPatientData()
    {
        for (PatientDataController<?> serializer : this.serializers) {
            PatientData<?> data = serializer.load(this);
            this.extraData.put(data.getName(), data);
        }
    }

    private boolean isFieldIncluded(Collection<String> includedFieldNames, String fieldName)
    {
        return (includedFieldNames == null || includedFieldNames.contains(fieldName));
    }

    private boolean isFieldIncluded(Collection<String> includedFieldNames, String[] fieldNames)
    {
        return (includedFieldNames == null || includedFieldNames.containsAll(Arrays.asList(fieldNames)));
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
        return (PatientData<T>) this.extraData.get(name);
    }

    @Override
    public JSONObject toJSON()
    {
        return toJSON(null);
    }

    @Override
    public JSONObject toJSON(Collection<String> onlyFieldNames)
    {
        JSONObject result = new JSONObject();

        if (isFieldIncluded(onlyFieldNames, "id")) {
            result.element("id", getDocument().getName());
        }

        if (getReporter() != null && isFieldIncluded(onlyFieldNames, "reporter")) {
            result.element("reporter", getReporter().getName());
        }

        if (!this.features.isEmpty() && isFieldIncluded(onlyFieldNames, PHENOTYPE_PROPERTIES)) {
            JSONArray featuresJSON = new JSONArray();
            for (Feature phenotype : this.features) {
                featuresJSON.add(phenotype.toJSON());
            }
            result.element(JSON_KEY_FEATURES, featuresJSON);
        }

        if (!this.disorders.isEmpty() && isFieldIncluded(onlyFieldNames, DISEASE_PROPERTIES)) {
            JSONArray diseasesJSON = new JSONArray();
            for (Disorder disease : this.disorders) {
                diseasesJSON.add(disease.toJSON());
            }
            result.element(JSON_KEY_DISORDERS, diseasesJSON);
        }

        for (PatientDataController<?> serializer : this.serializers) {
            serializer.writeJSON(this, result, onlyFieldNames);
        }

        return result;
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        try {
            // TODO: check versions and throw if versions mismatch if necessary
            // TODO: separe updateFrom JSON and saveToDB ? Move to PatientRepository?

            Execution execution = ComponentManagerRegistry.getContextComponentManager().getInstance(Execution.class);
            XWikiContext context = (XWikiContext) execution.getContext().getProperty("xwikicontext");

            DocumentAccessBridge documentAccessBridge = ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
            XWikiDocument doc = (XWikiDocument) documentAccessBridge.getDocument(getDocument());

            BaseObject data = doc.getXObject(CLASS_REFERENCE);
            if (data == null) {
                return;
            }

            JSONArray features = json.optJSONArray(JSON_KEY_FEATURES);
            if (features != null) {
                this.features = new TreeSet<Feature>();

                DBStringListProperty positiveValues = (DBStringListProperty) data.get(PHENOTYPE_POSITIVE_PROPERTY);
                DBStringListProperty negativeValues = (DBStringListProperty) data.get(PHENOTYPE_NEGATIVE_PROPERTY);

                for (int i = 0; i < features.size(); i++) {
                    JSONObject featureInJSON = features.optJSONObject(i);

                    Feature phenotipsFeature = new PhenoTipsFeature(featureInJSON);
                    this.features.add(phenotipsFeature);

                    if (phenotipsFeature.isPresent()) {
                        if (positiveValues == null) {
                            positiveValues = new DBStringListProperty(); // only create if at least one positive disorder
                            data.put(PHENOTYPE_POSITIVE_PROPERTY, positiveValues);
                        }
                        positiveValues.getList().add(phenotipsFeature.getValue());
                    }
                    else {
                        if (negativeValues == null) {
                            negativeValues = new DBStringListProperty(); // only create if at least one negative disorder
                            data.put(PHENOTYPE_NEGATIVE_PROPERTY, negativeValues);
                        }
                        negativeValues.getList().add(phenotipsFeature.getValue());
                    }
                }
                this.features = Collections.unmodifiableSet(this.features);  // same as in constructor
                context.getWiki().saveDocument(doc, "Updated features from JSON", true, context);
            }

            JSONArray disorders = json.optJSONArray(JSON_KEY_DISORDERS);
            if (disorders != null) {
                this.disorders = new TreeSet<Disorder>();

                DBStringListProperty disorderValues = (DBStringListProperty) data.get(DISEASE_PROPERTIES_OMIMID);

                for (int i = 0; i < disorders.size(); i++) {
                    JSONObject disorderJSON = disorders.optJSONObject(i);

                    Disorder phenotipsDisorder = new PhenoTipsDisorder(disorderJSON);
                    this.disorders.add(phenotipsDisorder);

                        if (disorderValues == null) {
                            disorderValues = new DBStringListProperty();
                            data.put(DISEASE_PROPERTIES_OMIMID, disorderValues);
                        }
                        disorderValues.getList().add(phenotipsDisorder.getValue());
                }
                this.disorders = Collections.unmodifiableSet(this.disorders);  // same as in constructor
                context.getWiki().saveDocument(doc, "Updated disorders from JSON", true, context);
            }

            for (PatientDataController<?> serializer : this.serializers) {
                try {
                    PatientData<?> patientData = serializer.readJSON(json);
                    if (patientData != null) {
                        this.extraData.put(patientData.getName(), patientData);
                        serializer.save(this);
                        this.logger.warn("Successfully updated patient form JSON using serializer [{}]", serializer.getName());
                    }
                } catch (UnsupportedOperationException ex) {
                    this.logger.warn("Unable to update patient from JSON using serializer [{}] - [{}]: {}", serializer.getName(), ex.getMessage(), ex);
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
