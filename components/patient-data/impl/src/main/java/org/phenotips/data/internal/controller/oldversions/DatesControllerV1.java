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
package org.phenotips.data.internal.controller.oldversions;

import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PhenoTipsDate;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.MapUtils;
import org.json.JSONObject;

/**
 * Handles serializing patient's date of birth and death and the exam date when data should be serialized in pre-1.3M2
 * JSON format (e.g. for pushing to servers older than 1.3M2 and thus only supporting push protocols before version 1.2)
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component(roles = { PatientDataController.class })
@Named("datesv1")
@Singleton
public class DatesControllerV1 implements PatientDataController<PhenoTipsDate>
{
    // field names as stored in the patient document
    protected static final String PATIENT_DATEOFDEATH_FIELDNAME = "date_of_death";

    protected static final String PATIENT_DATEOFBIRTH_FIELDNAME = "date_of_birth";

    protected static final String PATIENT_EXAMDATE_FIELDNAME = "exam_date";

    // controlling/enabling field name - should be present in selectedFieldNames as passed to writeJSON()/readJSON()
    // in order to include the corresponding data in the export or use it during import
    protected static final Map<String, String> CONTROLLING_FIELDNAMES =
        Collections.unmodifiableMap(MapUtils.putAll(new HashMap<String, String>(), new String[][] {
            { PATIENT_DATEOFDEATH_FIELDNAME, "date_of_death_v1" },
            { PATIENT_DATEOFBIRTH_FIELDNAME, "date_of_birth_v1" },
            { PATIENT_EXAMDATE_FIELDNAME, "exam_date_v1" }
        }));

    // field names as used in imported/exported JSON (same as above as of right now, but potentially different)
    protected static final String JSON_DATEOFDEATH_FIELDNAME = PATIENT_DATEOFDEATH_FIELDNAME;

    protected static final String JSON_DATEOFBIRTH_FIELDNAME = PATIENT_DATEOFBIRTH_FIELDNAME;

    protected static final String JSON_EXAMDATE_FIELDNAME = PATIENT_EXAMDATE_FIELDNAME;

    // 1-to-1 mapping between PT and JSON field names. The reverse is computed from the same mapping.
    // Only the fields lisetd here will ever be read from the document by the controller.
    protected static final Map<String, String> PHENOTIPS_TO_JSON_FIELDNAMES =
        Collections.unmodifiableMap(MapUtils.putAll(new LinkedHashMap<String, String>(), new String[][] {
            { PATIENT_DATEOFDEATH_FIELDNAME, JSON_DATEOFDEATH_FIELDNAME },
            { PATIENT_DATEOFBIRTH_FIELDNAME, JSON_DATEOFBIRTH_FIELDNAME },
            { PATIENT_EXAMDATE_FIELDNAME, JSON_EXAMDATE_FIELDNAME }
        }));

    // name of the data (key in the data map) as stored in the patient object
    private static final String DATA_NAME = "dates";

    private static final String CONTROLLER_NAME = "dates_v1";

    @Inject
    private RecordConfigurationManager configurationManager;

    @Override
    public PatientData<PhenoTipsDate> load(Patient patient)
    {
        // Explicitly do nothing.
        //
        // This controller is only used for serializing data (writeJSON) in old format,
        // and uses data loaded into Patient by the "regular" DateController
        return null;
    }

    @Override
    public void save(Patient patient, DocumentModelBridge doc)
    {
        // Explicitly do nothing.
        //
        // No exceptions are thrown because (in the current implementation) every controller's
        // save() method will be called (even though in this case we probably should not)
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        // this controller is only activated if a deprecated field is explicitly requested
        if (selectedFieldNames == null) {
            return;
        }

        DateFormat dateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

        PatientData<PhenoTipsDate> datesData = patient.getData(DATA_NAME);
        boolean noData = (datesData == null || !datesData.isNamed());

        for (String propertyName : this.getPatientDocumentProperties()) {

            // only a subset of fields was explicitly requested, and this field is not included
            if (!selectedFieldNames.contains(getControllingFieldName(propertyName))) {
                continue;
            }

            PhenoTipsDate data = noData ? null : datesData.get(propertyName);

            json.put(getJSONFieldName(propertyName),
                ((data == null) ? "" : dateFormat.format(data.toEarliestPossibleISODate())));
        }
    }

    @Override
    public PatientData<PhenoTipsDate> readJSON(JSONObject json)
    {
        // Explicitly do nothing.
        //
        // This controller is only used for serializing data (writeJSON) in old format,
        // in the current implementation new controller can read data in the old format.
        return null;
    }

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
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
}
