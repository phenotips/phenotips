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
import org.phenotips.entities.internal.AbstractPrimaryEntity;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

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

    protected static final String JSON_KEY_ID = "id";

    protected static final String JSON_KEY_REPORTER = "reporter";

    /** Logging helper object. */
    private Logger logger = LoggerFactory.getLogger(PhenoTipsPatient.class);

    /** @see #getReporter() */
    private DocumentReference reporter;

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

        loadSerializers();
    }

    @Override
    public EntityReference getType()
    {
        return CLASS_REFERENCE;
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
        Set<Feature> features = new TreeSet<>();
        PatientData<Feature> data = getData("features");
        if (data != null) {
            Iterator<Feature> iterator = data.iterator();
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                features.add(feature);
            }
        }
        return features;
    }

    @Override
    public Set<Disorder> getDisorders()
    {
        Set<Disorder> disorders = new TreeSet<>();
        PatientData<Disorder> data = getData("disorders");
        if (data != null) {
            Iterator<Disorder> iterator = data.iterator();
            while (iterator.hasNext()) {
                Disorder disorder = iterator.next();
                disorders.add(disorder);
            }
        }
        return disorders;
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

        for (PatientDataController<?> serializer : this.serializers.values()) {
            serializer.writeJSON(this, result, selectedFields);
        }

        return result;
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
