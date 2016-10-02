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
package org.phenotips.studies.family.script.response;

import org.xwiki.security.authorization.Right;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * JSON Response to client. Formats information from StatusResponse.
 *
 * @version $Id$
 */
public class NotEnoughPermissionsOnPatientResponse extends AbstractJSONResponse
{
    private List<String> listOfInaccessiblePatient;

    private Right missingRight;

    /**
     * Default constructor, takes no parameters.
     *
     * @param patientIDs List of patients which the current user does not have enough
     * @param missingRight The type of Right that the current user is missing permissions to perform the requested
     *            action (view or edit, depending on the action).
     */
    public NotEnoughPermissionsOnPatientResponse(List<String> patientIDs, Right missingRight)
    {
        this.listOfInaccessiblePatient = patientIDs;
        this.missingRight = missingRight;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONArray patientList = new JSONArray();
        for (String patient : this.listOfInaccessiblePatient) {
            patientList.put(patient);
        }
        JSONObject reply = baseErrorJSON(getErrorMessage((this.missingRight == Right.EDIT)
            ? PedigreeScriptServiceErrorMessage.INSUFFICIENT_PERMISSIONS_ON_PATIENT_EDIT
            : PedigreeScriptServiceErrorMessage.INSUFFICIENT_PERMISSIONS_ON_PATIENT_VIEW,
            patientList.toString()));
        reply.put("noPermissionsForPatientsList", patientList);
        reply.put("missingPermission", this.missingRight.toString());
        return reply;
    }

    @Override
    public boolean isErrorResponse()
    {
        return true;
    }
}
