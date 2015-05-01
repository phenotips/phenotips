package org.phenotips.studies.family.internal;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * Passed around to preserve important error information. Holds onto a status (modelled after HTTP statuses), a message,
 * and an error type.
 *
 * @version $Id$
 * @since 1.2RC1
 */
public class StatusResponse
{
    /** Status codes modelled after the standard HTTP status codes. */
    public int statusCode = 200;

    /** Should contain a message if there has been an error. */
    public String message = "";

    /** Should containt the type of error, if an error occured. */
    public String errorType = "";

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
     * successful
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

    /**
     * Convenience method for creating a pre-filled {@link StatusResponse}.
     * @param patientId to include in the error message
     * @return a {@link StatusResponse} with status code 401
     */
    public static StatusResponse createInsufficientPatientPermissionsResponse(String patientId)
    {
        StatusResponse response = new StatusResponse();
        response.statusCode = 401;
        response.errorType = "permissions";
        response.message = String.format("Insufficient permissions to edit the patient record (%s).", patientId);
        return response;
    }
}
