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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.ListProperty;

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
public class AllergiesController implements PatientDataController<Object>
{
    private static final String DATA_NAME = "allergiesData";

    private static final String ALLERGIES = "allergies";

    private static final String NKDA = "NKDA";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    @Override
    public PatientData<Object> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException("The patient does not have a PatientClass");
            }

            Map<String, Object> result = new LinkedHashMap<String, Object>();

            // allergies
            ListProperty allergiesListProperty = (ListProperty) data.get(ALLERGIES);
            List<String> allergiesList = allergiesListProperty.getList();
            result.put(ALLERGIES, allergiesList);

            // NKDA
            IntegerProperty nkdaProperty = (IntegerProperty) data.get(NKDA);
            Integer nkdaInteger = (Integer) nkdaProperty.getValue();
            boolean nkda = nkdaInteger == 1;
            result.put(NKDA, nkda);

            return new DictionaryPatientData<>(getName(), result);
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
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(ALLERGIES)) {
            return;
        }

        PatientData<Object> allergiesData = patient.getData(DATA_NAME);
        if (allergiesData == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<String> allergies = (List<String>) allergiesData.get(ALLERGIES);
        Boolean nkda = (Boolean) allergiesData.get(NKDA);

        JSONArray allergiesJsonArray = new JSONArray();
        if (nkda != null && nkda.booleanValue()) {
            allergiesJsonArray.add(NKDA);
        }

        if (allergies != null && allergies.size() > 0) {
            allergiesJsonArray.addAll(allergies);
        }
        if (!allergiesJsonArray.isEmpty()) {
            json.put(ALLERGIES, allergiesJsonArray);
        }
    }

    @Override
    public PatientData<Object> readJSON(JSONObject json)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> allergiesList = new LinkedList<>();
        boolean nkda = false;

        if (json.has(ALLERGIES)) {
            Object allergiesValue = json.get(ALLERGIES);

            if (allergiesValue instanceof JSONArray) {
                JSONArray allergiesJsonArray = (JSONArray) allergiesValue;
                for (Object o : allergiesJsonArray) {
                    String allergy = (String) o;
                    if (NKDA.equalsIgnoreCase(allergy)) {
                        nkda = true;
                    } else {
                        allergiesList.add(allergy);
                    }
                }
            }
        }

        result.put(NKDA, nkda);
        result.put(ALLERGIES, allergiesList);
        return new DictionaryPatientData<>(DATA_NAME, result);
    }
}
