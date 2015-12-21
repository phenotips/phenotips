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

import net.sf.json.JSONObject;

/**
 * JSON Response to client. Formats information from StatusResponse.
 *
 * @version $Id$
 */
public class PatientContainedMultipleTimesInPedigreeResponse extends AbstractJSONResponse
{
    private String duplicateId;

    /**
     * Default constructor, takes no parameters.
     * @param duplicateId An id which is contained more than once in the pedigree. If there is more than one
     * such ID, any one of them may be returned as the response.
     */
    public PatientContainedMultipleTimesInPedigreeResponse(String duplicateId) {
        this.duplicateId = duplicateId;
    }

    @Override
    public JSONObject toJSON() {
        return baseErrorJSON(getErrorMessage(PedigreeScriptServiceErrorMessage.DUPLICATE_PATIENT, this.duplicateId));
    }

    @Override
    public boolean isErrorResponse() {
        return true;
    }
}
