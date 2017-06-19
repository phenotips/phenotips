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

import org.phenotips.Constants;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles allergies information.
 *
 * @version $Id$
 */
@Component(roles = { PatientDataController.class })
@Named("allergies")
@Singleton
public class AllergiesController implements PatientDataController<String>
{
    static final EntityReference CLASS_REFERENCE =
        new EntityReference("AllergiesDataClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String DATA_NAME = "allergies";

    private static final String NKDA = "NKDA";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontext;

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(CLASS_REFERENCE);
            if (data == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<String> allergies = data.getListValue(DATA_NAME);
            int nkda = data.getIntValue(NKDA);

            List<String> result = new ArrayList<>(CollectionUtils.size(allergies) + 1);

            if (nkda == 1) {
                result.add(NKDA);
            }

            if (!CollectionUtils.isEmpty(allergies)) {
                result.addAll(allergies);
            }

            return new IndexedPatientData<>(DATA_NAME, result);
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
            final BaseObject xobject = patient.getXDocument().getXObject(CLASS_REFERENCE, true, this.xcontext.get());
            if (xobject == null) {
                return;
            }
            final PatientData<String> data = patient.getData(DATA_NAME);
            if (data == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    writeToPatientDoc(xobject, false, null);
                }
            } else {
                if (!data.isIndexed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveAllergiesData(patient, xobject, data, policy);
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save controller data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Saves the provided allergies {@code data}, according to the selected {@code policy}.
     *
     * @param patient the {@link Patient} being modified
     * @param xobject the {@link BaseObject}
     * @param data the {@link PatientData} to update {@code patient} with
     * @param policy the {@link PatientWritePolicy} for saving patient data
     */
    private void saveAllergiesData(
        @Nonnull final Patient patient,
        @Nonnull final BaseObject xobject,
        @Nonnull final PatientData<String> data,
        @Nonnull final PatientWritePolicy policy)
    {
        final PatientData<String> storedData = PatientWritePolicy.MERGE.equals(policy) ? load(patient) : null;
        final List<String> allergies = buildMergedAllergiesList(storedData, data);
        final boolean nkda = allergies.remove(NKDA);
        writeToPatientDoc(xobject, nkda, allergies.isEmpty() ? null : allergies);
    }

    /**
     * Returns a list of allergies, merging {@code storedAllergies stored allergies}, if any, and {@code allergies}.
     *
     * @param storedAllergies {@link PatientData} already stored in patient
     * @param allergies {@link PatientData} to save for patient
     * @return a merged list of allergies
     */
    private List<String> buildMergedAllergiesList(
        @Nullable final PatientData<String> storedAllergies,
        @Nonnull final PatientData<String> allergies)
    {
        // If there are no allergies stored, then just return a list of new allergies.
        if (storedAllergies == null || storedAllergies.size() == 0) {
            return StreamSupport.stream(allergies.spliterator(), false)
                .collect(Collectors.toList());
        }
        // There are some stored allergies, merge them.
        final Set<String> allergyValues = Stream.of(storedAllergies, allergies)
            .flatMap(s -> StreamSupport.stream(s.spliterator(), false))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return new ArrayList<>(allergyValues);
    }

    /**
     * Updates {@code xobject} with provided {@code nkda} and {@code allergies} data.
     *
     * @param xobject the {@link BaseObject}
     * @param nkda true iff no known drug allergies
     * @param allergies the list of allergies; may be null
     */
    private void writeToPatientDoc(
        @Nonnull final BaseObject xobject,
        final boolean nkda,
        @Nullable final List<String> allergies)
    {
        xobject.setIntValue(NKDA, nkda ? 1 : 0);
        xobject.setDBStringListValue(DATA_NAME, allergies);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(DATA_NAME)) {
            return;
        }

        PatientData<String> allergiesData = patient.getData(DATA_NAME);
        JSONArray allergiesJsonArray = new JSONArray();
        if (allergiesData == null || !allergiesData.isIndexed() || allergiesData.size() == 0) {
            if (selectedFieldNames != null && selectedFieldNames.contains(DATA_NAME)) {
                json.put(DATA_NAME, allergiesJsonArray);
            }
            return;
        }

        for (String allergy : allergiesData) {
            allergiesJsonArray.put(allergy);
        }
        json.put(DATA_NAME, allergiesJsonArray);
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        if (!json.has(DATA_NAME)) {
            return null;
        }
        List<String> result = new ArrayList<>();

        Object allergiesValue = json.get(DATA_NAME);
        if (allergiesValue instanceof JSONArray) {
            JSONArray allergiesJsonArray = (JSONArray) allergiesValue;
            for (Object allergy : allergiesJsonArray) {
                result.add(String.valueOf(allergy));
            }
        }

        return new IndexedPatientData<>(DATA_NAME, result);
    }
}
