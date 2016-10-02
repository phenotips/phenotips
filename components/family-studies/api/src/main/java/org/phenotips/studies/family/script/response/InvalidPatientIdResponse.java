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
public class InvalidPatientIdResponse extends AbstractJSONResponse
{
    private String patientId;

    /**
     * Default constructor, takes no parameters.
     *
     * @param patientId The id which is not a valid PhenotTips patient id.
     */
    public InvalidPatientIdResponse(String patientId)
    {
        this.patientId = patientId;
    }

    @Override
    public JSONObject toJSON()
    {
        return baseErrorJSON(getErrorMessage(PedigreeScriptServiceErrorMessage.INVALID_PATIENT_ID, this.patientId));
    }

    @Override
    public boolean isErrorResponse()
    {
        return true;
    }
}
