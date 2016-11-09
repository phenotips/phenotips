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
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ObjectPropertyReference;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Has only one field corresponding to whether the patient is affected, normal, or pre-symptomatic.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component(roles = { PatientDataController.class })
@Named("clinicalStatus")
@Singleton
public class ClinicalStatusController implements PatientDataController<String>
{
    private static final String CLINICAL_STATUS = "clinicalStatus";

    private static final String UNAFFECTED_STRING = "unaffected";

    private static final String PATIENT_DOCUMENT_FIELDNAME = UNAFFECTED_STRING;

    private static final String CONTROLLING_FIELDNAME = UNAFFECTED_STRING;

    private static final String JSON_FIELDNAME = CLINICAL_STATUS;

    private static final String VALUE_UNAFFECTED = UNAFFECTED_STRING;

    private static final String VALUE_AFFECTED = "affected";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return CLINICAL_STATUS;
    }

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            int isNormal = data.getIntValue(PATIENT_DOCUMENT_FIELDNAME);
            if (isNormal == 0) {
                return new SimpleValuePatientData<>(getName(), VALUE_AFFECTED);
            } else if (isNormal == 1) {
                return new SimpleValuePatientData<>(getName(), VALUE_UNAFFECTED);
            }
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(CONTROLLING_FIELDNAME)) {
            return;
        }
        PatientData<String> data = patient.getData(getName());
        if (data == null) {
            if (selectedFieldNames != null && selectedFieldNames.contains(CONTROLLING_FIELDNAME)) {
                json.put(getName(), VALUE_AFFECTED);
            }
            return;
        }

        json.put(JSON_FIELDNAME, data.getValue());
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void save(Patient patient)
    {
        BaseProperty<ObjectPropertyReference> isNormal =
            (BaseProperty<ObjectPropertyReference>) patient.getXDocument().getXObject(Patient.CLASS_REFERENCE).getField(
                PATIENT_DOCUMENT_FIELDNAME);
        PatientData<String> data = patient.getData(this.getName());
        if (isNormal == null || data == null) {
            return;
        }
        if (StringUtils.equals(data.getValue(), VALUE_AFFECTED)) {
            isNormal.setValue(0);
        } else if (StringUtils.equals(data.getValue(), VALUE_UNAFFECTED)) {
            isNormal.setValue(1);
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        String status = json.optString(JSON_FIELDNAME, null);
        if (StringUtils.equals(status, VALUE_AFFECTED)) {
            return new SimpleValuePatientData<>(this.getName(), VALUE_AFFECTED);
        } else if (StringUtils.equals(status, VALUE_UNAFFECTED)) {
            return new SimpleValuePatientData<>(this.getName(), VALUE_UNAFFECTED);
        }
        return null;
    }
}
