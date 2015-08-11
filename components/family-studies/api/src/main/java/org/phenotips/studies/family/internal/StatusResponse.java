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
package org.phenotips.studies.family.internal;

/**
 * Passed around to preserve important error information. Holds onto a status (modelled after HTTP statuses), a message,
 * and an error type.
 *
 * @version $Id$
 */
public enum StatusResponse
{
    /**
     * Patient can be added to a family.
     */
    OK(200,
        "",
        ""),

    /**
     * Duplicate patient in list.
     */
    DUPLICATE_PATIENT(400,
        "duplicate",
        "Patient list contains duplicates"),

    /**
     * Patient cannot be added to a family because it is already associated with another family.
     */
    ALREADY_HAS_FAMILY(501,
        "familyConflict",
        "Patient %1$s already belongs to a family %2$s, and therefore cannot be added to this one."),

    /**
     * Patient cannot be added to a family because current user has insufficient permissions on family.
     */
    INSUFFICIENT_PERMISSIONS_ON_FAMILY(401,
        "familyPermissions",
        "Insufficient permissions to edit the family record."),

    /**
     * Patient cannot be added to a family because current user has insufficient permissions on family.
     */
    INSUFFICIENT_PERMISSIONS_ON_PATIENT(401,
        "patientPermissions",
        "Insufficient permissions to edit the patient record."),

    /**
     * No members to add to family.
     **/
    FAMILY_HAS_NO_MEMBERS(402,
        "invalidUpdate",
        "The family has no members. Please specify at least one patient link."),

    /**
     * Invalid patient id.
     */
    INVALID_PATIENT_ID(404,
        "invalidPatientId",
        "Could not find patient %1$s."),

    /**
     * Invalid family id.
     */
    INVALID_FAMILY_ID(404,
        "invalidFamilyId",
        "Could not find family %2$s."),

    /**
     * Unknown error.
     */
    UNKNOWN_ERROR(500,
        "unknown",
        "Could not update patient records"),

    /**
     * Patient cannot be linked to proband's family.
     */
    PROBAND_HAS_NO_FAMILY(501,
        "NoFamilyForProband",
        "Patient %1$s cannot be linked to proband's family, because proband %3$s has not family.");

    private int statusCode;

    private String errorType;

    private String messageFormat;

    StatusResponse(int statusCode, String errorType, String messageFormat)
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
     * @return format of message
     */
    public String getMessageFormat()
    {
        return this.messageFormat;
    }

    /**
     * Checks if the response is valid.
     *
     * @return true is the response is valid.
     */
    public boolean isValid()
    {
        return this.getStatusCode() == StatusResponse.OK.getStatusCode();
    }
};
