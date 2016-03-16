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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the patient's date of birth and the exam date.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("identifiers")
@Singleton
public class IdentifiersController implements PatientDataController<String>
{
    /**
     * Section name.
     */
    public static final String DATA_NAME = "identifiers";

    private static final List<? extends String> IDS = Arrays.asList("external_id", "family_id", "birth_number");

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Provides access to the current execution context. */
    @Inject
    private Execution execution;

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            Map<String, String> result = new LinkedHashMap<String, String>();
            for (String idProperty : IDS) {
                result.put(idProperty, data.getStringValue(idProperty));
            }
            return new DictionaryPatientData<>(DATA_NAME, result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            PatientData<String> identifiers = patient.<String>getData(DATA_NAME);
            if (!identifiers.isNamed()) {
                return;
            }
            for (String idProperty : IDS) {
                String externalId = identifiers.get(idProperty);
                data.setStringValue(idProperty, externalId);
            }

            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            context.getWiki().saveDocument(doc, "Updated identifiers from JSON", true, context);
        } catch (Exception e) {
            this.logger.error("Failed to save identifiers: [{}]", e.getMessage());
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

        PatientData<String> patientData = patient.<String>getData(DATA_NAME);
        if (patientData != null && patientData.isNamed()) {
            Iterator<Entry<String, String>> values = patientData.dictionaryIterator();

            while (values.hasNext()) {
                Entry<String, String> datum = values.next();
                if (StringUtils.isNotBlank(datum.getValue())
                        && (selectedFieldNames == null
                            || selectedFieldNames.contains(datum.getKey()))) {
                    json.put(datum.getKey(), datum.getValue());
                }
            }
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {

        Map<String, String> result = new LinkedHashMap<String, String>();
        for (String idProperty : IDS) {
            String externalId = json.getString(idProperty);
            result.put(idProperty, externalId);
        }
        return new DictionaryPatientData<String>(DATA_NAME, result);
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }
}
