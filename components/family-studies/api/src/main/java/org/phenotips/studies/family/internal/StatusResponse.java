package org.phenotips.studies.family.internal;

import net.sf.json.JSONObject;

/**
 * Passed around different functions to preserve important error information.
 */
public class StatusResponse
{
    public int statusCode = 200;

    public String message = "";

    public String errorType = "";

    public String asProcessing() {
        boolean isError = statusCode != 200;
        JSONObject json = baseErrorJson();
        json.put("error", isError);
        return json.toString();
    }

    public String asVerification() {
        boolean valid = statusCode != 200;
        JSONObject json = baseErrorJson();
        json.put("validLink", valid);
        return json.toString();
    }

    private JSONObject baseErrorJson() {
        JSONObject json = new JSONObject();
        json.put("errorMessage", message);
        json.put("errorType", errorType);
        return json;
    }
}
