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

import org.phenotips.data.rest.MeasurementPercentileResource;
import org.phenotips.measurements.MeasurementHandler;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.json.JSONObject;

/**
 * Default implementation for {@link MeasurementPercentileResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultMeasurementPercentileResourceImpl")
@Singleton
public class DefaultMeasurementPercentileResourceImpl extends XWikiResource implements MeasurementPercentileResource
{
    /** Injected map of measurement handlers. */
    @Inject
    private Map<String, MeasurementHandler> handlers;

    @Override
    public Response getMeasurementPercentile(String measurement, float value, float ageMonths, char sex)
    {
        boolean isMale = Character.toLowerCase(sex) == 'm';
        if (!isMale && Character.toLowerCase(sex) != 'f') {
            return generateErrorResponse(Response.Status.BAD_REQUEST, "Invalid sex. Supported: M or F.");
        }

        MeasurementHandler handler;
        handler = handlers.get(measurement);
        if (handler == null) {
            return generateErrorResponse(Response.Status.NOT_FOUND, "Specified measurement type not found.");
        }

        JSONObject resp = new JSONObject();
        resp.accumulate("percentile", handler.valueToPercentile(isMale, ageMonths, value));
        resp.accumulate("stddev", handler.valueToStandardDeviation(isMale, ageMonths, value));

        return Response.ok(resp, MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Generate a server response in case of error.
     *
     * @param status The HTTP status.
     * @param text The error text to be returned to the client.
     * @return The response object.
     */
    private Response generateErrorResponse(Response.Status status, String text)
    {
        return Response.status(status).entity(text).header("Content-Type", "text/plain").build();
    }
}
