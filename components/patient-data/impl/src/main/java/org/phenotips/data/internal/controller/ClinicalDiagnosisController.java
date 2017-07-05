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

import org.phenotips.data.Disorder;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.internal.PhenoTipsDisorder;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;

/**
 * Handles the patient's working clinical diagnosis.
 *
 * @version $Id$
 * @since 1.3
 */
@Component(roles = { PatientDataController.class })
@Named("clinical-diagnosis")
@Singleton
public class ClinicalDiagnosisController implements PatientDataController<Disorder>
{
    protected static final String JSON_KEY_CLINICAL_DIAGNOSIS = "clinical-diagnosis";

    private static final String CONTROLLER_NAME = JSON_KEY_CLINICAL_DIAGNOSIS;

    private static final String DIAGNOSIS_PROPERTY = "clinical_diagnosis";

    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    public IndexedPatientData<Disorder> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }

            List<Disorder> disorders = new ArrayList<>();

            ListProperty values = (ListProperty) data.get(DIAGNOSIS_PROPERTY);
            if (values != null) {
                for (String value : values.getList()) {
                    if (StringUtils.isNotBlank(value)) {
                        disorders.add(new PhenoTipsDisorder(values, value));
                    }
                }
            }
            if (disorders.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<>(getName(), disorders);
            }
        } catch (Exception e) {
            this.logger.error("Failed to access patient data for [{}]: {}", patient.getDocument(), e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient, DocumentModelBridge doc)
    {
        if (doc == null) {
            throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
        }
        PatientData<Disorder> disorders = patient.getData(this.getName());
        if (disorders == null || !disorders.isIndexed()) {
            return;
        }

        XWikiDocument docX = (XWikiDocument) doc;
        BaseObject data = docX.getXObject(Patient.CLASS_REFERENCE);

        // new disorders list (for setting values in the Wiki document)
        List<String> disorderValues = new LinkedList<>();

        Iterator<Disorder> iterator = disorders.iterator();
        while (iterator.hasNext()) {
            Disorder disorder = iterator.next();
            disorderValues.add(disorder.getValue());
        }

        // update the values in the document (overwriting the old list, if any)
        data.set(DIAGNOSIS_PROPERTY, disorderValues, this.xcontextProvider.get());
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(DIAGNOSIS_PROPERTY)) {
            return;
        }

        PatientData<Disorder> data = patient.getData(getName());
        json.put(JSON_KEY_CLINICAL_DIAGNOSIS, diseasesToJSON(data));
    }

    /** creates & returns a new JSON array of all patient clinical diseases (as JSON objects). */
    private JSONArray diseasesToJSON(PatientData<Disorder> data)
    {
        JSONArray diseasesJSON = new JSONArray();
        if (data != null) {
            for (Disorder disease : data) {
                diseasesJSON.put(disease.toJSON());
            }
        }
        return diseasesJSON;
    }

    @Override
    public PatientData<Disorder> readJSON(JSONObject json)
    {
        if (json == null || !json.has(JSON_KEY_CLINICAL_DIAGNOSIS)) {
            return null;
        }

        JSONArray inputDisorders = json.optJSONArray(JSON_KEY_CLINICAL_DIAGNOSIS);
        if (inputDisorders == null) {
            return null;
        }

        List<Disorder> disorders = new ArrayList<>();
        for (int i = 0; i < inputDisorders.length(); i++) {
            JSONObject disorderJSON = inputDisorders.optJSONObject(i);
            if (disorderJSON == null) {
                continue;
            }

            Disorder phenotipsDisorder = new PhenoTipsDisorder(disorderJSON);
            disorders.add(phenotipsDisorder);
        }
        return new IndexedPatientData<>(getName(), disorders);
    }
}
