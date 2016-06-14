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

import org.json.JSONObject;

/**
 * JSON Response to client. Formats information from StatusResponse.
 *
 * @version $Id$
 */
public class AlreadyHasFamilyResponse extends AbstractJSONResponse
{
    private String patientId;

    private String familyId;

    /**
     * Default constructor, takes no parameters.
     *
     * @param patientId Id of the patient which already belongs ot another family
     * @param familyId Id of the family that patient already belongs to.
     */
    public AlreadyHasFamilyResponse(String patientId, String familyId)
    {
        this.patientId = patientId;
        this.familyId = familyId;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject reply = baseErrorJSON(
            getErrorMessage(PedigreeScriptServiceErrorMessage.ALREADY_HAS_FAMILY, this.patientId, this.familyId));
        reply.put("patientId", this.patientId);
        reply.put("familyId", this.familyId);
        return reply;
    }

    @Override
    public boolean isErrorResponse()
    {
        return true;
    }
}
