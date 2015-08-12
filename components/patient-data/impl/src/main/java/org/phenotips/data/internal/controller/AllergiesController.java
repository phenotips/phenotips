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
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Handles allergies information.
 *
 * @version $Id$
 */
@Component(roles = { PatientDataController.class })
@Named("allergies")
@Singleton
public class AllergiesController implements PatientDataController<String>
{
    private static final String DATA_NAME = "allergies";

    private static final String NKDA = "NKDA";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontext;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException("The patient does not have a PatientClass");
            }

            @SuppressWarnings("unchecked")
            List<String> allergies = data.getListValue(DATA_NAME);
            int nkda = data.getIntValue(NKDA);

            List<String> result = new ArrayList<>(CollectionUtils.size(allergies) + 1);

            if (nkda == 1) {
                result.add(NKDA);
            }

            if (!CollectionUtils.isEmpty(allergies)) {
                result.addAll(allergies);
            }

            return new IndexedPatientData<>(DATA_NAME, result);
        } catch (Exception e) {
            this.logger.error(
                "Could not find requested document or some unforeseen error has occurred during controller loading.",
                e);
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        try {
            PatientData<String> data = patient.getData(DATA_NAME);
            if (data == null || !data.isIndexed()) {
                return;
            }

            boolean nkda = false;
            List<String> allergies = new ArrayList<>(data.size());
            for (String allergy : data) {
                if (NKDA.equals(allergy)) {
                    nkda = true;
                } else {
                    allergies.add(allergy);
                }
            }

            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject xobject = doc.getXObject(Patient.CLASS_REFERENCE, true, this.xcontext.get());
            xobject.setIntValue(NKDA, nkda ? 1 : 0);
            xobject.setDBStringListValue(DATA_NAME, allergies);

            this.xcontext.get().getWiki().saveDocument(doc, "Updated allergies from JSON", true, this.xcontext.get());
        } catch (Exception ex) {
            this.logger.error(
                "Could not find requested document or some unforeseen error has occurred during controller loading.",
                ex);
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

        PatientData<String> allergiesData = patient.getData(DATA_NAME);
        if (allergiesData == null || !allergiesData.isIndexed() || allergiesData.size() == 0) {
            return;
        }

        JSONArray allergiesJsonArray = new JSONArray();
        for (String allergy : allergiesData) {
            allergiesJsonArray.add(allergy);
        }
        json.put(DATA_NAME, allergiesJsonArray);
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        List<String> result = new ArrayList<>();

        if (json.has(DATA_NAME)) {
            Object allergiesValue = json.get(DATA_NAME);

            if (allergiesValue instanceof JSONArray) {
                JSONArray allergiesJsonArray = (JSONArray) allergiesValue;
                for (Object allergy : allergiesJsonArray) {
                    result.add(String.valueOf(allergy));
                }
            }
        }

        return new IndexedPatientData<>(DATA_NAME, result);
    }
}
