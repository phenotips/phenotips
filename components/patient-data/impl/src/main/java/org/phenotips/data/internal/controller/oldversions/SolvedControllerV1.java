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
package org.phenotips.data.internal.controller.oldversions;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.internal.SolvedData;

import org.xwiki.component.annotation.Component;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONObject;

/**
 * Handles fields for solved patient records, including solved status, PubMed ID, gene symbol, and notes when data
 * should be serialized in pre-1.4 JSON format (e.g. for pushing to servers older than 1.4 and thus only supporting push
 * protocols before version 1.3)
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = { PatientDataController.class })
@Named("solvedv1")
@Singleton
public class SolvedControllerV1 implements PatientDataController<SolvedData>
{
    private static final String SOLVED_STRING = "solved";

    private static final String CONTROLLER_NAME = "solved_v1";

    private static final String SOLVED_ENABLING_FIELD_NAME = CONTROLLER_NAME;

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    public PatientData<SolvedData> load(Patient patient)
    {
        // Explicitly do nothing.
        //
        // This controller is only used for serializing data (writeJSON) in old format,
        // and uses data loaded into Patient by the "regular" DateController
        return null;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        // this controller is only activated if a deprecated field is explicitly requested
        if (selectedFieldNames == null || !selectedFieldNames.contains(SOLVED_ENABLING_FIELD_NAME)) {
            return;
        }

        PatientData<SolvedData> data = patient.getData(SOLVED_STRING);
        if (data == null) {
            json.put(SOLVED_STRING, new JSONObject());
            return;
        }

        SolvedData dataHolder = data.getValue();
        json.put(SOLVED_STRING, dataHolder.toJSON());

        // select first Pubmed ID from the list of Pubmed IDs
        // TODO in future, may be select the valid one or all of them as one comma-separated string
        if (!dataHolder.getPubmedIds().isEmpty()) {
            String firstPubmedID = dataHolder.getPubmedIds().get(0);
            JSONObject container = json.optJSONObject(SOLVED_STRING);
            container.put(SolvedData.PUBMED_ID_JSON_KEY, firstPubmedID);
        }
    }

    @Override
    public PatientData<SolvedData> readJSON(JSONObject json)
    {
        // Explicitly do nothing.
        //
        // This controller is only used for serializing data (writeJSON) in old format,
        // and uses data loaded into Patient by the "regular" DateController
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        save(patient, PatientWritePolicy.UPDATE);
    }

    @Override
    public void save(@Nonnull final Patient patient, @Nonnull final PatientWritePolicy policy)
    {
        // Explicitly do nothing.
        //
        // This controller is only used for serializing data (writeJSON) in old format,
        // and uses data loaded into Patient by the "regular" DateController
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }
}
