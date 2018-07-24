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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.SimpleValuePatientData;
import org.phenotips.data.internal.SolvedData;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ObjectPropertyReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Handles fields for solved patient records, including solved status, PubMed ID, gene symbol, and notes.
 *
 * @version $Id$
 * @since 1.2M2
 */
@Component(roles = { PatientDataController.class })
@Named("solved")
@Singleton
public class SolvedController implements PatientDataController<SolvedData>
{
    private static final String SOLVED_STRING = "solved";

    private static final String DATA_NAME = SOLVED_STRING;

    private static final String INTERNAL_PROPERTY_NAME = SOLVED_STRING;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    @Override
    public PatientData<SolvedData> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }

            String status = data.getStringValue(SolvedData.STATUS_PROPERTY_NAME);
            String notes = data.getStringValue(SolvedData.NOTES_PROPERTY_NAME);
            @SuppressWarnings("unchecked")
            List<String> patientPubmedIds = data.getListValue(SolvedData.PUBMED_ID_PROPERTY_NAME);

            SolvedData result = new SolvedData(status, notes, patientPubmedIds);

            return new SimpleValuePatientData<>(this.getName(), result);
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
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
            BaseObject xwikiDataObject = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE, true,
                this.contextProvider.get());
            PatientData<SolvedData> data = patient.getData(getName());

            if (data == null || data.getValue().isEmpty()) {
                // For replace policy, if no controller data is provided, everything that's stored should be removed.
                // otherwise - nothing happens
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    saveFieldValues(xwikiDataObject, null, policy);
                }
            } else {
                saveControllerData(xwikiDataObject, data.getValue(), patient, policy);
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save controller data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Saves {@code data}, according to the provided {@code policy}. For any controller extending this class,
     * {@link PatientWritePolicy#UPDATE} and {@link PatientWritePolicy#MERGE} are not equivalent.
     *
     * @param xwikiDataObject the XWiki {@link BaseObject data object}
     * @param data the {@link SolvedData} object containing data that needs to be saved, or null if policy is REPLCE and
     *            data is null
     * @param patient patient
     * @param policy the policy, according to which patient data should be saved
     */
    private void saveControllerData(
        @Nonnull final BaseObject xwikiDataObject,
        @Nonnull final SolvedData data,
        @Nonnull final Patient patient,
        @Nonnull final PatientWritePolicy policy)
    {
        if (PatientWritePolicy.MERGE.equals(policy)) {
            SolvedData mergedData = getMergedData(data, patient);
            this.saveFieldValues(xwikiDataObject, mergedData, policy);
        } else {
            this.saveFieldValues(xwikiDataObject, data, policy);
        }
    }

    private SolvedData getMergedData(@Nonnull SolvedData data, @Nonnull final Patient patient)
    {
        PatientData<SolvedData> savedData = load(patient);
        if (savedData == null) {
            return data;
        }
        SolvedData patientData = savedData.getValue();
        // remain case solved or update to solved
        if (data.isSolved() || StringUtils.isBlank(patientData.getStatus())) {
            patientData.setStatus(data.getStatus());
        }
        // merge notes
        String notes = data.getNotes();
        if (StringUtils.isNotBlank(notes)) {
            String patientNotes = patientData.getNotes();
            if (StringUtils.isNotBlank(patientNotes)) {
                patientNotes.concat("\n").concat(notes);
                patientData.setNotes(patientNotes);
            } else {
                patientData.setNotes(notes);
            }
        }
        // merge Pubmed IDs
        List<String> patientPubmedIds = patientData.getPubmedIds();
        List<String> newPubmedIds = data.getPubmedIds();
        if (!newPubmedIds.isEmpty()) {
            if (patientPubmedIds.isEmpty()) {
                patientData.setPubmedIds(newPubmedIds);
            } else {
                // remove duplicates first
                patientPubmedIds.retainAll(newPubmedIds);
                patientPubmedIds.addAll(newPubmedIds);
                patientData.setPubmedIds(patientPubmedIds);
            }
        }
        return patientData;
    }

    /**
     * Sets the {@code value} for a {@code property} field in {@code xwikiDataObject}.
     *
     * @param xwikiDataObject the {@link BaseObject} where data will be saved
     * @param data the {@link SolvedData} object containing data that needs to be saved, or null if policy is REPLCE and
     *            data is null
     * @param policy policy : UPDATE, RERPLACE, MERGE
     */
    private void saveFieldValues(@Nonnull final BaseObject xwikiDataObject, SolvedData data, PatientWritePolicy policy)
    {
        if (data == null) {
            xwikiDataObject.setDBStringListValue(SolvedData.PUBMED_ID_PROPERTY_NAME, null);
        } else {
            List<String> pubmedIds = data.getPubmedIds();
            if (!PatientWritePolicy.UPDATE.equals(policy) || pubmedIds != null) {
                xwikiDataObject.setDBStringListValue(SolvedData.PUBMED_ID_PROPERTY_NAME, pubmedIds);
            }
        }

        List<String> stringProperties = Arrays.asList(SolvedData.STATUS_PROPERTY_NAME, SolvedData.NOTES_PROPERTY_NAME);
        stringProperties.forEach(property -> this.setStringField(xwikiDataObject, data, property, policy));
    }

    private void setStringField(@Nonnull final BaseObject xwikiDataObject, SolvedData data, String property,
        PatientWritePolicy policy)
    {
        @SuppressWarnings("unchecked")
        BaseProperty<ObjectPropertyReference> field =
            (BaseProperty<ObjectPropertyReference>) xwikiDataObject.getField(property);
        if (field == null) {
            return;
        }
        if (data == null) {
            field.setValue(null);
        } else {
            String value = (SolvedData.STATUS_PROPERTY_NAME.equals(property)) ? data.getStatus() : data.getNotes();
            if (value != null || PatientWritePolicy.REPLACE.equals(policy)) {
                field.setValue(applyCast(value));
            }
        }
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        this.writeJSON(patient, json, null);
    }

    protected String getJsonPropertyName()
    {
        return INTERNAL_PROPERTY_NAME;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(DATA_NAME)) {
            return;
        }

        PatientData<SolvedData> data = patient.getData(getName());
        if (data == null) {
            if (selectedFieldNames != null && selectedFieldNames.contains(DATA_NAME)) {
                json.put(this.getJsonPropertyName(), new JSONObject());
            }
            return;
        }
        if (selectedFieldNames == null) {
            json.put(DATA_NAME, data.getValue().toJSON());
        } else {
            json.put(DATA_NAME, data.getValue().toJSON(selectedFieldNames));
        }
    }

    @Override
    public PatientData<SolvedData> readJSON(JSONObject json)
    {
        if (!json.has(this.getJsonPropertyName())) {
            // no data supported by this controller is present in provided JSON
            return null;
        }

        Object jsonBlockObject = json.get(this.getJsonPropertyName());
        if (!(jsonBlockObject instanceof JSONObject)) {
            return null;
        }
        JSONObject jsonBlock = (JSONObject) jsonBlockObject;
        SolvedData data = new SolvedData(jsonBlock);

        return new SimpleValuePatientData<>(this.getName(), data);
    }

    // Cast string statuses "0" and "1" to Integer representation before storing
    private Object applyCast(String value)
    {
        if (value == null) {
            return null;
        }
        if (SolvedData.STATUS_VALUES.contains(value)) {
            return Integer.parseInt(value);
        } else {
            return value;
        }
    }

}
