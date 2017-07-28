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
import org.phenotips.measurements.internal.USLSMeasurementHandler;

import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;

/**
 * Default implementation for {@link ComputedMeasurementResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M4
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultComputedMeasurementResourceImpl")
@Singleton
public class DefaultComputedMeasurementResourceImpl extends AbstractMeasurementRestResource implements
    ComputedMeasurementResource
{
    /** BMI and US/LS short names. */
    private static final String BMI = "bmi";
    private static final String USLS = "usls";

    @Override
    public Response getComputedMeasurement(UriInfo uriInfo)
    {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        String measurement = params.getFirst("measurement");
        if (measurement == null) {
            throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST,
                "Measurement not specified."));
        }

        double value;
        if (BMI.equals(measurement)) {
            String[] dependencies = {params.getFirst("weight"), params.getFirst("height")};
            value = handleComputations(BMI, dependencies);
        } else if (USLS.equals(measurement)) {
            String[] dependencies = {params.getFirst("upperSeg"), params.getFirst("lowerSeg")};
            value = handleComputations(USLS, dependencies);
        } else {
            throw new WebApplicationException(generateErrorResponse(Response.Status.NOT_FOUND,
                "Specified measurement type not found."));
        }

        JSONObject resp = new JSONObject();
        resp.accumulate("value", value);

        return Response.ok(resp, MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Handle computations for measurements such as BMI and US:LS.
     *
     * @param measurement the measurement to be computed
     * @param dependencies dependencies for the computed measurement (eg. weight, height, segment length)
     * @return computed value
     */
    private double handleComputations(String measurement, String[] dependencies) throws IllegalArgumentException
    {
        for (String dep : dependencies) {
            if (dep == null) {
                throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST,
                    "Computation arguments were not all provided."));
            }
        }

        try {
            double value;
            if (BMI.equals(measurement)) {
                // BMI is the measurement to be computed. Pass deps 0 and 1 as the respective height and weight
                value = ((BMIMeasurementHandler) this.handlers.get(BMI))
                .computeBMI(Double.parseDouble(dependencies[0]), Double.parseDouble(dependencies[1]));
            } else if (USLS.equals(measurement)) {
                // US/LS is the measurement to be computed. Pass deps 0 and 1 as the respective upperSeg and lowerSeg
                value = ((USLSMeasurementHandler) this.handlers.get(USLS))
                .computeUSLS(Double.parseDouble(dependencies[0]), Double.parseDouble(dependencies[1]));
            } else {
                // The measurement param passed to this method does not match any known computed measurement types
                throw new IllegalArgumentException();
            }

            return value;
        } catch (NumberFormatException e) {
            throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST,
                "Cannot parse computation arguments."));
        }
    }
}
