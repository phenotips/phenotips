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

import org.phenotips.data.ContactInfo;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientContactsManager;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleValuePatientData;
import org.phenotips.data.internal.DefaultPatientContactsManager;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;

import java.util.Collection;

import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handle's the patient owner's contact information.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Component(roles = { PatientDataController.class })
@Named("owner-contact")
@Singleton
public class ContactInformationController implements PatientDataController<PatientContactsManager>
{
    private static final String DATA_CONTACT = "contact";

    @Override
    public PatientData<PatientContactsManager> load(Patient patient)
    {
        PatientContactsManager contactsManager = new DefaultPatientContactsManager(patient);
        if (contactsManager.size() == 0) {
            return null;
        }
        return new SimpleValuePatientData<>(getName(), contactsManager);
    }

    @Override
    public void save(Patient patient, DocumentModelBridge doc)
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
        if (selectedFieldNames != null && !selectedFieldNames.contains(getEnablingFieldName())) {
            return;
        }
        PatientData<PatientContactsManager> data = patient.getData(DATA_CONTACT);
        if (data == null) {
            return;
        }

        Collection<ContactInfo> contacts = data.getValue().getAll();
        if (contacts == null) {
            return;
        }

        JSONArray container = json.optJSONArray(DATA_CONTACT);
        if (container == null) {
            json.put(DATA_CONTACT, new JSONArray());
            container = json.optJSONArray(DATA_CONTACT);
        }
        for (ContactInfo info : contacts) {
            container.put(info.toJSON());
        }
    }

    @Override
    public PatientData<PatientContactsManager> readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return DATA_CONTACT;
    }

    /**
     * Unlike all other controllers, there is no field name controlling presence of version information in JSON output.
     * This method returns a name which can be used instead.
     *
     * @return a name which can be included in the list of enabled fields to enable version info in JSON output
     */
    public static String getEnablingFieldName()
    {
        return DATA_CONTACT;
    }
}
