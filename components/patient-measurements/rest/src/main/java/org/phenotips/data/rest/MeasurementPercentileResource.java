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
package org.phenotips.data.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Resource for working with patient measurement value percentiles and standard deviation.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Path("/measurements/percentile")
public interface MeasurementPercentileResource
{
    /**
     * Get the calculated percentile and standard deviation value.
     *
     * @param measurement The name of the measurement
     * @param value The measurement value
     * @param age The patient's age, in ISO 8601 duration format, with no leading 'P', e.g. 2Y2M3W6D.
     * @param sex The patient's sex, M or F
     * @return percentile and standard deviation, JSON encoded
     */
    @GET
    Response getMeasurementPercentile(@QueryParam("measurement") String measurement, @QueryParam("value") float value,
                        @QueryParam("age") String age, @QueryParam("sex") char sex);
}
