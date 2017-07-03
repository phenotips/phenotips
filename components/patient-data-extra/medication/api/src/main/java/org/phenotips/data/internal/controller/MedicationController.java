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

import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Medication;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.joda.time.MutablePeriod;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the patient's medication records.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component(roles = { PatientDataController.class })
@Named("medication")
@Singleton
public class MedicationController implements PatientDataController<Medication>
{
    /** The name of this patient data controller. */
    public static final String DATA_NAME = "medication";

    /** The name of the XProperty used for storing the years part of {@link Medication#getDuration()}. */
    public static final String DURATION_YEARS = "durationYears";

    /** The name of the XProperty used for storing the months part of {@link Medication#getDuration()}. */
    public static final String DURATION_MONTHS = "durationMonths";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** The current request context, needed when working with the XWiki old core. */
    @Inject
    private Provider<XWikiContext> xcontext;

    @Override
    public PatientData<Medication> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            List<BaseObject> data = doc.getXObjects(Medication.CLASS_REFERENCE);
            if (data == null || data.isEmpty()) {
                this.logger.debug("No medication data for patient [{}]", patient.getId());
                return null;
            }
            List<Medication> result = new LinkedList<>();
            for (BaseObject medication : data) {
                if (medication == null) {
                    continue;
                }
                MutablePeriod p = new MutablePeriod();
                p.setYears(medication.getIntValue(DURATION_YEARS));
                p.setMonths(medication.getIntValue(DURATION_MONTHS));
                result.add(new Medication(medication.getStringValue(Medication.NAME),
                    medication.getStringValue(Medication.GENERIC_NAME),
                    medication.getStringValue(Medication.DOSE),
                    medication.getStringValue(Medication.FREQUENCY),
                    p.toPeriod(),
                    medication.getStringValue(Medication.EFFECT),
                    medication.getLargeStringValue(Medication.NOTES)));
            }
            if (!result.isEmpty()) {
                return new IndexedPatientData<>(DATA_NAME, result);
            }
        } catch (Exception ex) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", ex.getMessage());
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
            final XWikiDocument docX = patient.getXDocument();
            final PatientData<Medication> medications = patient.getData(getName());

            if (medications == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    docX.removeXObjects(Medication.CLASS_REFERENCE);
                }
            } else {
                if (!medications.isIndexed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveMedications(docX, patient, medications, policy, this.xcontext.get());
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save medication data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Saves {@code medications} data according to the provided {@code policy}.
     *
     * @param docX the {@link XWikiDocument}
     * @param patient the {@link Patient} whose data is being updated
     * @param medications the medication {@link PatientData data} to update {@code patient} with
     * @param policy the {@link PatientWritePolicy} according to which data should be saved
     * @param context the {@link XWikiContext}
     */
    private void saveMedications(
        @Nonnull final XWikiDocument docX,
        @Nonnull final Patient patient,
        @Nonnull final PatientData<Medication> medications,
        @Nonnull final PatientWritePolicy policy,
        @Nonnull final XWikiContext context)
    {
        // A stream of medication objects to save.
        final Stream<Medication> medsStream;
        if (PatientWritePolicy.MERGE.equals(policy)) {
            final PatientData<Medication> storedMedications = load(patient);
            medsStream = storedMedications != null
                ? buildMergedMedicationsMap(medications, storedMedications).entrySet().stream()
                    .flatMap(mapEntry -> mapEntry.getValue().stream())
                : StreamSupport.stream(medications.spliterator(), false)
                    .filter(Objects::nonNull);
            docX.removeXObjects(Medication.CLASS_REFERENCE);
        } else {
            medsStream = StreamSupport.stream(medications.spliterator(), false)
                .filter(Objects::nonNull);
            docX.removeXObjects(Medication.CLASS_REFERENCE);
        }
        // Save each medication.
        medsStream.forEach(medication -> saveMedication(docX, medication, context));
    }

    /**
     * Saves the data specified in {@code medication}.
     *
     * @param docX the {@link XWikiDocument}
     * @param medication a {@link Medication} to save
     * @param context the {@link XWikiContext}
     */
    private void saveMedication(
        @Nonnull final XWikiDocument docX,
        @Nonnull final Medication medication,
        @Nonnull final XWikiContext context)
    {
        try {
            final BaseObject o = docX.newXObject(Medication.CLASS_REFERENCE, context);
            o.setStringValue(Medication.NAME, medication.getName());
            o.setStringValue(Medication.GENERIC_NAME, medication.getGenericName());
            o.setStringValue(Medication.DOSE, medication.getDose());
            o.setStringValue(Medication.FREQUENCY, medication.getFrequency());
            if (medication.getDuration() != null) {
                o.setIntValue(DURATION_YEARS, medication.getDuration().getYears());
                o.setIntValue(DURATION_MONTHS, medication.getDuration().getMonths());
            }
            if (medication.getEffect() != null) {
                o.setStringValue(Medication.EFFECT, medication.getEffect().toString());
            }
            o.setLargeStringValue(Medication.NOTES, medication.getNotes());
        } catch (final Exception ex) {
            this.logger.error("Failed to save medication data: [{}]", ex.getMessage());
        }
    }

    /**
     * Builds a map of medication name to list medications with that name. Will be a singleton list in most cases,
     * unless the medication name is unknown.
     *
     * @param medications medication {@link PatientData data} to add to existing data
     * @param storedMedications existing medication {@link PatientData data}
     * @return a map of medication name to list of corresponding medications
     */
    private Map<String, List<Medication>> buildMergedMedicationsMap(
        @Nonnull final PatientData<Medication> medications,
        @Nonnull final PatientData<Medication> storedMedications)
    {
        // A function to obtain the medication name. If the medication name is null, "unknown" will be returned.
        final Function<Medication, String> nameFx = medication -> medication.getName() == null
            ? "unknown"
            : medication.getName();
        return Stream.of(storedMedications, medications)
            .flatMap(s -> StreamSupport.stream(s.spliterator(), false))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(nameFx, Collections::singletonList, this::mergeMedications, LinkedHashMap::new));
    }

    /**
     * Resolves collisions in medication data. If the medication has a name specified, will always pick the new value;
     * if the name is unknown will keep all.
     *
     * @param storedMeds a list of medications stored in patient; will be a singleton list if medication name is known
     * @param newMeds a list of newly added medications; will be a singleton list
     * @return a singleton list of medications, if medication name is known, an accumulated list otherwise
     */
    private List<Medication> mergeMedications(
        @Nonnull final List<Medication> storedMeds,
        @Nonnull final List<Medication> newMeds)
    {
        // If a medication does not have a specified name, then can't compare between medications. Store all.
        // Otherwise, just store the new value.
        return storedMeds.get(0).getName() == null
            ? Stream.of(storedMeds, newMeds).flatMap(Collection::stream).collect(Collectors.toList())
            : newMeds;
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
        PatientData<Medication> data = patient.getData(DATA_NAME);
        if (data == null || !data.isIndexed() || data.size() <= 0) {
            return;
        }

        Iterator<Medication> it = data.iterator();
        JSONArray result = new JSONArray();
        while (it.hasNext()) {
            Medication datum = it.next();
            if (datum == null) {
                continue;
            }
            result.put(datum.toJSON());
        }
        if (result.length() > 0) {
            json.put(DATA_NAME, result);
        }
    }

    @Override
    public PatientData<Medication> readJSON(JSONObject json)
    {
        if (json == null || json.length() == 0) {
            return null;
        }
        JSONArray data = json.optJSONArray(DATA_NAME);
        if (data == null || data.length() == 0) {
            return null;
        }
        List<Medication> result = new LinkedList<>();
        for (int i = 0; i < data.length(); ++i) {
            JSONObject datum = data.getJSONObject(i);
            result.add(new Medication(datum));
        }
        return new IndexedPatientData<>(DATA_NAME, result);
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }
}
