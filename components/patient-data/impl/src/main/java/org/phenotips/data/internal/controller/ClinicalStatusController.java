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
import org.xwiki.model.reference.ObjectPropertyReference;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONObject;
import org.apache.commons.codec.binary.StringUtils;
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
    private final static String UNAFFECTED = "unaffected";

    private final static String AFFECTED = "affected";

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
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            int isNormal = data.getIntValue(UNAFFECTED);
            if (isNormal == 0) {
                return new SimpleValuePatientData<String>(getName(), AFFECTED);
            } else if (isNormal == 1) {
                return new SimpleValuePatientData<String>(getName(), UNAFFECTED);
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
        if (selectedFieldNames != null && !selectedFieldNames.contains(getName())) {
            return;
        }
        PatientData<String> data = patient.getData(getName());
        if (data == null) {
            return;
        }
        JSONObject container = json.optJSONObject(getName());

        if (container == null) {
            // put() is placed here because we want to create the property iff at least one field is set/enabled
            json.put(getName(), new JSONObject());
            container = json.optJSONObject(getName());
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
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseProperty<ObjectPropertyReference> isNormal =
                (BaseProperty<ObjectPropertyReference>) doc.getXObject(Patient.CLASS_REFERENCE).getField(UNAFFECTED);
            PatientData<String> data = patient.getData(this.getName());
            if (isNormal == null || data == null) {
                return;
            }
            if (StringUtils.equals(data.getValue(), AFFECTED)) {
                isNormal.setValue(0);
            } else if (StringUtils.equals(data.getValue(), UNAFFECTED)) {
                isNormal.setValue(1);
            }
        } catch (Exception e) {
            this.logger.error("Could not load patient document or some unknown error has occurred", e.getMessage());
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        JSONObject data = json.optJSONObject(this.getName());
        if (data == null) {
            return null;
        }
        String status = data.optString(this.getName());
        if (StringUtils.equals(status, AFFECTED)) {
            return new SimpleValuePatientData<String>(this.getName(), AFFECTED);
        } else if (StringUtils.equals(status, UNAFFECTED)) {
            return new SimpleValuePatientData<String>(this.getName(), UNAFFECTED);
        }
        return null;
    }
}
