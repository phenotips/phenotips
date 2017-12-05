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

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    @Inject
    private Container container;

    @Override
    public Response getComputedMeasurement(String measurement)
    {
        if (!this.computationHandlers.containsKey(measurement)) {
            throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST,
                "This measurement is not intended to be computed."));
        }

        double value;
        try {
            value = this.computationHandlers.get(measurement).handleComputation(getParameters());
        } catch (IllegalArgumentException ex) {
            throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST, ex.getMessage()));
        }

        JSONObject resp = new JSONObject();
        resp.accumulate("value", value);

        return Response.ok(resp, MediaType.APPLICATION_JSON_TYPE).build();
    }

    private Map<String, Number> getParameters()
    {
        Map<String, String[]> requestParams =
            ((ServletRequest) this.container.getRequest()).getHttpServletRequest().getParameterMap();

        Map<String, Number> result = new HashMap<>();
        for (Map.Entry<String, String[]> i : requestParams.entrySet()) {
            try {
                double value = Double.parseDouble(i.getValue()[0]);
                if (Double.isFinite(value)) {
                    result.put(i.getKey(), value);
                }
            } catch (Exception ex) {
                // Invalid user-provided values, just ignore them
            }
        }
        return result;
    }
}
