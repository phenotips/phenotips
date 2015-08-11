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

import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * JSON Response to client. Formats information from StatusResponse.
 *
 * @version $Id$
 */
public class JSONResponse
{
    private StatusResponse statusResponse;

    private String message;

    /**
     * Empty constructor.
     */
    public JSONResponse()
    {

    }

    /**
     * Creates a JSONResponse object based on a StatusResponse.
     *
     * @param statusResponse to base the JSONResponse on
     */
    public JSONResponse(StatusResponse statusResponse)
    {
        setStatusResponse(statusResponse);
    }

    /**
     * Sets the status response.
     *
     * @param statusResponse to format
     * @return reference to self
     */
    public JSONResponse setStatusResponse(StatusResponse statusResponse)
    {
        this.statusResponse = statusResponse;
        this.message = this.statusResponse.getMessageFormat();
        return this;
    }

    /**
     * Sets the parameter for the error message.
     *
     * @param parameters for the error message
     * @return reference to self
     */
    public JSONResponse setMessage(Object... parameters)
    {
        this.message = String.format(this.statusResponse.getMessageFormat(), parameters);
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
        JSONObject json = baseErrorJson();
        json.put("error", !this.isValid());
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
        JSONObject json = baseErrorJson();
        json.put("validLink", this.isValid());
        return json;
    }

    private JSONObject baseErrorJson()
    {
        JSONObject json = new JSONObject();
        json.put("errorMessage", this.message);
        json.put("errorType", this.statusResponse.getErrorType());
        return json;
    }

    /**
     * Checks if the response is valid.
     *
     * @return true is the response is valid.
     */
    public boolean isValid()
    {
        return this.statusResponse.isValid();
    }
}
