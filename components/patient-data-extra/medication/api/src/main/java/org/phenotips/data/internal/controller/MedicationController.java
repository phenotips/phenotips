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

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
            XWikiDocument doc = patient.getDocument();
            List<BaseObject> data = doc.getXObjects(Medication.CLASS_REFERENCE);
            if (data == null || data.isEmpty()) {
                this.logger.debug("No medication data for patient [{}]", patient.getDocumentReference());
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
                return new IndexedPatientData<Medication>(DATA_NAME, result);
            }
        } catch (Exception ex) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", ex.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient, DocumentModelBridge doc)
    {
        try {
            PatientData<Medication> data = patient.getData(DATA_NAME);
            if (data == null || !data.isIndexed()) {
                return;
            }
            ((XWikiDocument) doc).removeXObjects(Medication.CLASS_REFERENCE);
            XWikiContext context = this.xcontext.get();
            for (Medication m : data) {
                if (m == null) {
                    continue;
                }
                BaseObject o = ((XWikiDocument) doc).newXObject(Medication.CLASS_REFERENCE, context);
                o.setStringValue(Medication.NAME, m.getName());
                o.setStringValue(Medication.GENERIC_NAME, m.getGenericName());
                o.setStringValue(Medication.DOSE, m.getDose());
                o.setStringValue(Medication.FREQUENCY, m.getFrequency());
                if (m.getDuration() != null) {
                    o.setIntValue(DURATION_YEARS, m.getDuration().getYears());
                    o.setIntValue(DURATION_MONTHS, m.getDuration().getMonths());
                }
                if (m.getEffect() != null) {
                    o.setStringValue(Medication.EFFECT, m.getEffect().toString());
                }
                o.setLargeStringValue(Medication.NOTES, m.getNotes());
            }
        } catch (Exception ex) {
            this.logger.error("Failed to save medication data: [{}]", ex.getMessage());
        }
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
