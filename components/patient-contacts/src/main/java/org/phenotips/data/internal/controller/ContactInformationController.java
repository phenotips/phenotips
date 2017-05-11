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
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientContactsManager;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles the contact information for people responsible for a patient record.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Component(roles = { PatientDataController.class })
@Named("owner-contact")
@Singleton
public class ContactInformationController implements PatientDataController<ContactInfo>
{
    private static final String DATA_CONTACT = "contact";

    @Inject
    private PatientContactsManager contactsManager;

    @Override
    public PatientData<ContactInfo> load(Patient patient)
    {
        List<ContactInfo> contacts = this.contactsManager.getAll(patient);
        if (contacts == null || contacts.isEmpty()) {
            return null;
        }
        return new IndexedPatientData<>(getName(), contacts);
    }

    @Override
    public void save(Patient patient)
    {
        // This is a read-only controller, at least for the moment, so nothing to do
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(DATA_CONTACT)) {
            return;
        }
        PatientData<ContactInfo> data = patient.getData(DATA_CONTACT);
        if (data == null || !data.isIndexed() || data.size() == 0) {
            return;
        }

        JSONArray container = json.optJSONArray(DATA_CONTACT);
        if (container == null) {
            json.put(DATA_CONTACT, new JSONArray());
            container = json.optJSONArray(DATA_CONTACT);
        }
        for (ContactInfo info : data) {
            container.put(info.toJSON());
        }
    }

    @Override
    public PatientData<ContactInfo> readJSON(JSONObject json)
    {
        // This is a read-only controller, at least for the moment, so nothing to do
        return null;
    }

    @Override
    public String getName()
    {
        return DATA_CONTACT;
    }
}
