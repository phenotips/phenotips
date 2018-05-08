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

import org.phenotips.Constants;
import org.phenotips.data.Gene;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles the patients genes in pre-1.4.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = { PatientDataController.class })
@Named("genes_v1")
@Singleton
public class GeneListControllerV1 implements PatientDataController<Gene>
{
    /** The XClass used for storing gene data. */
    protected static final EntityReference GENE_CLASS_REFERENCE = new EntityReference("GeneClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String GENES_STRING = "genes";

    private static final String CONTROLLER_NAME = "genes_v1";

    private static final String GENES_ENABLING_FIELD_NAME = CONTROLLER_NAME;

    private static final String INTERNAL_STATUS_KEY = "status";

    private static final String INTERNAL_COMMENTS_KEY = "comments";

    private static final String INTERNAL_REJECTED_VALUE = "rejected";

    private static final String INTERNAL_REJECTED_CANDIDATE_VALUE = "rejected_candidate";

    private static final String JSON_COMMENTS_KEY = INTERNAL_COMMENTS_KEY;

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    public PatientData<Gene> load(Patient patient)
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
        if (selectedFieldNames == null || !selectedFieldNames.contains(GENES_ENABLING_FIELD_NAME)) {
            return;
        }

        PatientData<Gene> data = patient.getData(GENES_STRING);
        if (data == null || data.size() == 0) {
            json.put(GENES_STRING, new JSONArray());
            return;
        }

        JSONArray geneArray = new JSONArray();

        for (Gene gene : data) {
            JSONObject geneObj = gene.toJSON();
            // change "rejected_candidate" status to "rejected"
            if (gene.getStatus().equals(INTERNAL_REJECTED_CANDIDATE_VALUE)) {
                geneObj.put(INTERNAL_STATUS_KEY, INTERNAL_REJECTED_VALUE);
                String comment = geneObj.optString(JSON_COMMENTS_KEY, "");
                if ("".equals(comment)) {
                    geneObj.put(JSON_COMMENTS_KEY, "* Rejected candidate");
                } else {
                    geneObj.put(JSON_COMMENTS_KEY, "* Rejected candidate \n\n" + comment);
                }
            }
            geneArray.put(geneObj);
        }

        json.put(GENES_STRING, geneArray);
    }

    @Override
    public PatientData<Gene> readJSON(JSONObject json)
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
