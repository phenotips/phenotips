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
package org.phenotips.studies.family.internal2;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * Passed around to preserve important error information. Holds onto a status (modelled after HTTP statuses), a message,
 * and an error type.
 *
 * @version $Id$
 */
public enum StatusResponse2
{
    /**
     * Patient can be added to a family.
     */
    OK(200,
        "",
        ""),

    DUPLICATE_PATIENT(400,
        "duplicate",
        "There is a duplicate link for patient %s"),

    /**
     * Patient cannot be added to a family because it is already associated with another family.
     */
    ALREADY_HAS_FAMILY(501,
        "familyConflict",
        "Patient %s already belongs to a different family, and therefore cannot be added to this one."),

    /**
     * Patient cannot be added to a family because current user has insufficient permissions on family.
     */
    INSUFFICIENT_PERMISSIONS_ON_FAMILY(401,
        "permissions",
        "Insufficient permissions to edit the family record."),

     /**
      * Patient cannot be added to a family because current user has insufficient permissions on family.
      */
     INSUFFICIENT_PERMISSIONS_ON_PATIENT(401,
         "permissions",
         "Insufficient permissions to edit the patient record."),

    FAMILY_HAS_NO_MEMBERS(402,
        "invalidUpdate",
        "The family has no members. Please specify at least one patient link."),

    /**
     * Invalid patient id.
     */
    INVALID_PATIENT_ID(404,
        "invalidPatientId",
        "Could not find patient %s."),

    /**
     * Invalid family id.
     */
    INVALID_FAMILY_ID(404,
        "invalidFamilyId",
        "Could not find family %s."),

    UNKNOWN_ERROR(500,
        "unknown",
        "Could not update patient records"),

    /**
     * Patient cannot be linked to proband's family.
     */
    PROBAND_HAS_NO_FAMILY(501,
        "NoFamilyForProband",
        "Patient %s cannot be linked to proband's family, because proband %s has not family."),

    /**
     * Patient cannot be added to family because its pedigree is not empty.
     */
    PEDIGREE_NOT_EMPTY(501,
        "existingPedigree",
        "patient %s already has a different pedigree, and therefore cannot be included in this one.");

    private int statusCode;

    private String errorType;

    private String messageFormat;

    private String message;

    StatusResponse2(int statusCode, String errorType, String messageFormat)
    {
        this.statusCode = statusCode;
        this.errorType = errorType;
        this.messageFormat = messageFormat;
    }

    /**
     * @return status code of the response
     */
    public int getStatusCode()
    {
        return this.statusCode;
    }

    /**
     * @return error type of the response
     */
    public String getErrorType()
    {
        return this.errorType;
    }

    /**
     * Sets the parameter for the error message.
     *
     * @param parameters for the error message
     * @return reference to self
     */
    public StatusResponse2 setMessage(Object... parameters)
    {
        this.message = String.format(this.messageFormat, parameters);
        return this;
    }

    /**
     * @return error message
     */
    public String getMessage()
    {
        return this.message;
    }

    /**
     * Formats itself as a response to a processing request.
     *
     * @return JSON with an 'error' field which is true/false; true if an error has occured
     */
    public JSON asProcessing()
    {
        boolean isError = this.statusCode != 200;
        JSONObject json = baseErrorJson();
        json.put("error", isError);
        return json;
    }

    /**
     * Formats itself as a response to a validation request.
     *
     * @return JSON with a 'validLink' field which is true if no errors have occurred and the validation process was
     *         successful
     */
    public JSON asVerification()
    {
        boolean valid = this.statusCode == 200;
        JSONObject json = baseErrorJson();
        json.put("validLink", valid);
        return json;
    }

    private JSONObject baseErrorJson()
    {
        JSONObject json = new JSONObject();
        json.put("errorMessage", this.message);
        json.put("errorType", this.errorType);
        return json;
    }
};
