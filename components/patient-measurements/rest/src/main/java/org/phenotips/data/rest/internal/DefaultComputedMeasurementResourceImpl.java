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

import org.phenotips.data.rest.ComputedMeasurementResource;
import org.phenotips.measurements.internal.BMIMeasurementHandler;

import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.sf.json.JSONObject;

/**
 * Default implementation for {@link ComputedMeasurementResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultComputedMeasurementResourceImpl")
@Singleton
public class DefaultComputedMeasurementResourceImpl extends AbstractMeasurementRestResource implements
        ComputedMeasurementResource
{
    /** BMI short name. */
    private static final String BMI = "bmi";

    @Override
    public Response getComputedMeasurement(UriInfo uriInfo)
    {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        String measurement = params.getFirst("measurement");
        if (measurement == null) {
            throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST,
                    "Measurement not specified."));
        }

        if (BMI.equals(measurement)) {
            double value = handleBmi(params);

            JSONObject resp = new JSONObject();
            resp.accumulate("value", value);

            return Response.ok(resp, MediaType.APPLICATION_JSON_TYPE).build();
        } else {
            throw new WebApplicationException(generateErrorResponse(Response.Status.NOT_FOUND,
                    "Specified measurement type not found."));
        }
    }

    /**
     * Handle BMI computation.
     *
     * @param params query parameters
     * @return computed value
     */
    private double handleBmi(MultivaluedMap<String, String> params)
    {
        String weight = params.getFirst("weight");
        String height = params.getFirst("height");
        if (weight == null || height == null) {
            throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST,
                    "Computation arguments were not all provided."));
        }

        try {
            double value = ((BMIMeasurementHandler) handlers.get(BMI)).computeBMI(Double.parseDouble(weight),
                    Double.parseDouble(height));

            return value;
        } catch (NumberFormatException e) {
            throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST,
                    "Cannot parse computation arguments."));
        }
    }
}
