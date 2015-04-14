/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.internal.controller;

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

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
    private static final String DATA_NAME = "identifiers";

    private static final String EXTERNAL_IDENTIFIER_PROPERTY_NAME = "external_id";

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
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }
            Map<String, String> result = new LinkedHashMap<String, String>();
            result.put(EXTERNAL_IDENTIFIER_PROPERTY_NAME, data.getStringValue(EXTERNAL_IDENTIFIER_PROPERTY_NAME));
            return new DictionaryPatientData<>(DATA_NAME, result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document");
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
            String externalId = identifiers.get(EXTERNAL_IDENTIFIER_PROPERTY_NAME);
            data.setStringValue(EXTERNAL_IDENTIFIER_PROPERTY_NAME, externalId);

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
        if (selectedFieldNames != null && !selectedFieldNames.contains(EXTERNAL_IDENTIFIER_PROPERTY_NAME)) {
            return;
        }

        PatientData<String> patientData = patient.<String>getData(DATA_NAME);
        if (patientData != null && patientData.isNamed()) {
            Iterator<Entry<String, String>> values = patientData.dictionaryIterator();

            while (values.hasNext()) {
                Entry<String, String> datum = values.next();
                if (StringUtils.isNotBlank(datum.getValue())) {
                    json.put(datum.getKey(), datum.getValue());
                }
            }
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        if (!json.containsKey(EXTERNAL_IDENTIFIER_PROPERTY_NAME)) {
            // no data supported by this controller is present in provided JSON
            return null;
        }
        String externalId = json.getString(EXTERNAL_IDENTIFIER_PROPERTY_NAME);

        Map<String, String> result = new LinkedHashMap<String, String>();
        result.put(EXTERNAL_IDENTIFIER_PROPERTY_NAME, externalId);
        return new DictionaryPatientData<String>(DATA_NAME, result);
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }
}
