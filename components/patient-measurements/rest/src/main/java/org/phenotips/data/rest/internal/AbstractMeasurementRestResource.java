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
package org.phenotips.data.rest.internal;

import org.phenotips.measurements.MeasurementHandler;

import org.xwiki.rest.XWikiResource;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.json.JSONObject;

/**
 * Base class for implementing a measurement REST resource.
 *
 * @version $Id$
 * @since 1.2M5
 */
public abstract class AbstractMeasurementRestResource extends XWikiResource
{
    /** Injected map of measurement handlers. */
    @Inject
    protected Map<String, MeasurementHandler> handlers;

    /**
     * Generate a server response in case of error.
     *
     * @param status the HTTP status
     * @param text the error text to be returned to the client
     * @return the response object
     */
    protected static Response generateErrorResponse(Response.Status status, String text)
    {
        JSONObject resp = new JSONObject();
        resp.accumulate("error", text);

        return Response.status(status).entity(resp).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
