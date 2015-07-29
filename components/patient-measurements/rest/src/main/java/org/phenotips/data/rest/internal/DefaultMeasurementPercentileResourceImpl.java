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

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.joda.time.Period;
import org.json.JSONObject;

/**
 * Default implementation for {@link MeasurementPercentileResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M4
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultMeasurementPercentileResourceImpl")
@Singleton
public class DefaultMeasurementPercentileResourceImpl extends AbstractMeasurementRestResource implements
    MeasurementPercentileResource
{
    @Override
    public Response getMeasurementPercentile(String measurement, float value, String age, char sex)
    {
        boolean isMale = Character.toLowerCase(sex) == 'm';
        if (!isMale && Character.toLowerCase(sex) != 'f') {
            throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST,
                "Invalid sex. Supported: M or F."));
        }

        Period agePeriod;
        try {
            agePeriod = Period.parse("P" + age);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST, "Cannot parse age."));
        }
        float ageMonths = 0;
        ageMonths += agePeriod.getYears() * 12;
        ageMonths += agePeriod.getMonths();
        ageMonths += agePeriod.getWeeks() * 7 / 30.4375;
        ageMonths += agePeriod.getDays() / 30.4375;

        MeasurementHandler handler;
        handler = this.handlers.get(measurement);
        if (handler == null) {
            throw new WebApplicationException(generateErrorResponse(Response.Status.NOT_FOUND,
                "Specified measurement type not found."));
        }

        JSONObject resp = new JSONObject();
        resp.accumulate("percentile", handler.valueToPercentile(isMale, ageMonths, value));
        resp.accumulate("stddev", handler.valueToStandardDeviation(isMale, ageMonths, value));

        return Response.ok(resp, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
