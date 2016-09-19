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

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PhenoTipsDate;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the patient's date of birth and death and the exam date.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("dates")
@Singleton
public class DatesController implements PatientDataController<PhenoTipsDate>
{
    // field names as stored in the patient document
    protected static final String PATIENT_DATEOFDEATH_FIELDNAME = "date_of_death";

    protected static final String PATIENT_DATEOFBIRTH_FIELDNAME = "date_of_birth";

    protected static final String PATIENT_EXAMDATE_FIELDNAME = "exam_date";

    // (optional) name of the "helper" field which stores the date as entered by the user (as temporarily? used by PT)
    protected static final Map<String, String> CORRESPONDING_ASENTERED_FIELDNAMES =
        Collections.unmodifiableMap(MapUtils.putAll(new HashMap<String, String>(), new String[][] {
            { PATIENT_DATEOFDEATH_FIELDNAME, "date_of_death_entered" },
            { PATIENT_DATEOFBIRTH_FIELDNAME, "date_of_birth_entered" }
        }));

    // field names as used in imported/exported JSON (same as above as of right now, but potentially different)
    protected static final String JSON_DATEOFDEATH_FIELDNAME = PATIENT_DATEOFDEATH_FIELDNAME;

    protected static final String JSON_DATEOFBIRTH_FIELDNAME = PATIENT_DATEOFBIRTH_FIELDNAME;

    protected static final String JSON_EXAMDATE_FIELDNAME = PATIENT_EXAMDATE_FIELDNAME;

    // 1-to-1 mapping between PT and JSON field names. The reverse is computed from the same mapping.
    // Only the fields listed here will ever be read from the document by the controller.
    protected static final Map<String, String> PHENOTIPS_TO_JSON_FIELDNAMES =
        Collections.unmodifiableMap(MapUtils.putAll(new LinkedHashMap<String, String>(), new String[][] {
            { PATIENT_DATEOFDEATH_FIELDNAME, JSON_DATEOFDEATH_FIELDNAME },
            { PATIENT_DATEOFBIRTH_FIELDNAME, JSON_DATEOFBIRTH_FIELDNAME },
            { PATIENT_EXAMDATE_FIELDNAME, JSON_EXAMDATE_FIELDNAME }
        }));

    // controlling/enabling field name - should be present in selectedFieldNames as passed to writeJSON()/readJSON()
    // in order to include the corresponding data in the export or use it during import
    protected static final Map<String, String> CONTROLLING_FIELDNAMES =
        Collections.unmodifiableMap(MapUtils.putAll(new HashMap<String, String>(), new String[][] {
            { PATIENT_DATEOFDEATH_FIELDNAME, PATIENT_DATEOFDEATH_FIELDNAME },
            { PATIENT_DATEOFBIRTH_FIELDNAME, PATIENT_DATEOFBIRTH_FIELDNAME },
            { PATIENT_EXAMDATE_FIELDNAME, PATIENT_EXAMDATE_FIELDNAME }
        }));

    protected static final String DATA_NAME = "dates";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public PatientData<PhenoTipsDate> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            Map<String, PhenoTipsDate> result = new LinkedHashMap<>();
            for (String propertyName : getPatientDocumentProperties()) {
                String dateAsEntered = CORRESPONDING_ASENTERED_FIELDNAMES.containsKey(propertyName)
                    ? data.getStringValue(CORRESPONDING_ASENTERED_FIELDNAMES.get(propertyName))
                    : null;
                if (!StringUtils.isEmpty(dateAsEntered)) {
                    // if "date as entered" is present, use it and disregard the date field itself.
                    try {
                        JSONObject dateAsEnteredJSON = new JSONObject(dateAsEntered);
                        result.put(propertyName, new PhenoTipsDate(dateAsEnteredJSON));
                    } catch (Exception ex) {
                        this.logger.error("Could not process date-as-entered for {}: [{}]",
                            propertyName, ex.getMessage());
                    }
                } else {
                    // otherwise use the date field
                    Date date = data.getDateValue(propertyName);
                    if (date != null) {
                        result.put(propertyName, new PhenoTipsDate(date));
                    }
                }
            }
            return new DictionaryPatientData<>(DATA_NAME, result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading [{}]", e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient, DocumentModelBridge doc)
    {
        BaseObject data = ((XWikiDocument) doc).getXObject(Patient.CLASS_REFERENCE);
        if (data == null) {
            throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
        }

        PatientData<PhenoTipsDate> dates = patient.getData(DATA_NAME);
        if (!dates.isNamed()) {
            throw new IllegalArgumentException(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
        }
        for (String propertyName : this.getPatientDocumentProperties()) {
            if (dates.containsKey(propertyName)) {
                PhenoTipsDate date = dates.get(propertyName);
                // note: `date` may be null if data is missing
                if (CORRESPONDING_ASENTERED_FIELDNAMES.containsKey(propertyName)) {
                    data.setStringValue(CORRESPONDING_ASENTERED_FIELDNAMES.get(propertyName),
                        (date == null ? "" : date.toString()));
                }
                // if date is not a valid/complete date, toEarliestPossibleISODate() will return null
                // and date will be effectively "unset"
                data.setDateValue(propertyName, (date == null ? null : date.toEarliestPossibleISODate()));
            }
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
        PatientData<PhenoTipsDate> datesData = patient.getData(DATA_NAME);
        boolean noData = (datesData == null || !datesData.isNamed());

        for (String propertyName : this.getPatientDocumentProperties()) {
            PhenoTipsDate data = noData ? null : datesData.get(propertyName);
            // note: get(propertyName) may return null

            // no specific set of fields was explicitly requested and no data => do not addd anything
            if (selectedFieldNames == null && data == null) {
                continue;
            }

            // only a subset of fields was explicitly requested, and this field is not included
            if (selectedFieldNames != null && !selectedFieldNames.contains(getControllingFieldName(propertyName))) {
                continue;
            }

            // in all other cases need to add the property to JSON, either with empoty value or
            // with proper value
            json.put(getJSONFieldName(propertyName), ((data == null) ? "" : data.toJSON()));
        }
    }

    @Override
    public PatientData<PhenoTipsDate> readJSON(JSONObject json)
    {
        Map<String, PhenoTipsDate> result = new LinkedHashMap<>();

        // Some dates (birth/death) are internally stored as a string-object pair, one for the date
        // which should always be a correct date string, and one for the "date as entered", which is
        // an string representing a JSON object supporting "fuzzy" and incomplete dates.
        //
        // For now the following formats are supported for all dates in the incoming JSON:
        // 1) an object with {"decade", "year", "month", "day"} fields
        // 2) a date string, in either
        // a) ISO format (e.g. 2001-01-23 or 1999-03)
        // b) ISO with missing trailing parts (e.g. 1999-11)
        // c) ISO with a decades instead of a year (e.g. 1990s-11-21 or 1990s-01)
        //
        // In the first case (an object with separate year-month-etc fields) the "date as entered" (if present)
        // will be set to exactly the imported value, but the date-as-ISO string will be set to:
        // 1) in case of a valid exact date - the corresponding ISO string
        // 2) in case of a fuzzy date with only a decade or a missing month/day - to the earliest possible date
        // 3) in case of an incomplete date without a year - will not be set

        // TODO: review once internal date format is changed

        for (String property : this.getPatientDocumentProperties()) {
            String jsonFieldName = getJSONFieldName(property);
            if (json.has(jsonFieldName)) {
                // the value in the json may be either a string or a JSON object
                JSONObject dateAsObject = json.optJSONObject(jsonFieldName);
                if (dateAsObject != null) {
                    result.put(property, new PhenoTipsDate(dateAsObject));
                } else {
                    result.put(property, new PhenoTipsDate(json.optString(jsonFieldName, null)));
                }
            }
        }
        if (result.isEmpty()) {
            return null;
        } else {
            return new DictionaryPatientData<>(DATA_NAME, result);
        }
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    protected Set<String> getPatientDocumentProperties()
    {
        return PHENOTIPS_TO_JSON_FIELDNAMES.keySet();
    }

    protected String getJSONFieldName(String phenotipsFieldName)
    {
        String fieldName = PHENOTIPS_TO_JSON_FIELDNAMES.get(phenotipsFieldName);
        if (fieldName == null) {
            throw new NullPointerException("Phenotips field name has no corresponding JSON field name");
        }
        return fieldName;
    }

    protected String getControllingFieldName(String phenotipsFieldName)
    {
        String fieldName = CONTROLLING_FIELDNAMES.get(phenotipsFieldName);
        if (fieldName == null) {
            throw new NullPointerException("Phenotips field name has no corresponding controlling field name");
        }
        return fieldName;
    }

    protected String getPhenotipsFieldName(String jsonFieldName)
    {
        for (String ptFieldName : PHENOTIPS_TO_JSON_FIELDNAMES.keySet()) {
            if (jsonFieldName.equalsIgnoreCase(PHENOTIPS_TO_JSON_FIELDNAMES.get(ptFieldName))) {
                return ptFieldName;
            }
        }
        throw new NullPointerException("JSON field name has no corresponding Phenotips field name");
    }
}
