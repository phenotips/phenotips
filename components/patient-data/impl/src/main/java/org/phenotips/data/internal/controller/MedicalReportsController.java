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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import net.sf.json.JSONObject;

/**
 * Provides a dictionary of medical document names to links.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component(roles = { PatientDataController.class })
@Named("medicalreports")
@Singleton
public class MedicalReportsController implements PatientDataController<String>
{
    /**
     * Provides access to the underlying data storage.
     */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiContext context = contextProvider.get();
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException("The patient does not have a PatientClass");
            }
            Map<String, String> result = new LinkedHashMap<>();

            /* Getting the documents which are reports instead of just getting all attachments */
            BaseProperty medicalReportsField = (BaseProperty) data.getField("reports_history");
            List<String> reports = (List<String>) medicalReportsField.getValue();

            for (String report : reports) {
                String url = doc.getAttachmentURL(report, "download", context);
                result.put(report, url);
            }

            return new DictionaryPatientData<>(getName(), result);
        } catch (Exception e) {
            this.logger.error(
                "Could not find requested document or some unforeseen error has occurred during controller loading");
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException("Medical reports should not be present in the JSON");
    }

    @Override
    public String getName()
    {
        return "medicalreports";
    }
}
