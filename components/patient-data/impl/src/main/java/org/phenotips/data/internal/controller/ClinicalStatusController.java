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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

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
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public String getName()
    {
        return "clinicalStatus";
    }

    @Override
    public PatientData<String> load(Patient patient)
    {
        String unaffected = "unaffected";
        String affected = "affected";
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }
            int isNormal = data.getIntValue(unaffected);
            if (isNormal == 0) {
                return new SimpleValuePatientData<String>(getName(), affected);
            } else if (isNormal == 1) {
                return new SimpleValuePatientData<String>(getName(), unaffected);
            }
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        PatientData<String> data = patient.getData(getName());
        if (data == null) {
            return;
        }
        JSONObject container = json.getJSONObject(getName());

        if (container == null || container.isNullObject()) {
            // put() is placed here because we want to create the property iff at least one field is set/enabled
            json.put(getName(), new JSONObject());
            container = json.getJSONObject(getName());
        }
        container.put(data.getName(), data.getValue());
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void save(Patient patient)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }
}
