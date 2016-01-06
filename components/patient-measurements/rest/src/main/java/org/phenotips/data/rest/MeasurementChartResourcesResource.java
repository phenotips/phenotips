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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for getting chart resources, i.e. available charts with their titles and URLs.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Path("/measurements/chart-resources")
public interface MeasurementChartResourcesResource
{
    /**
     * Get a set of chart resources that are available for the given measurement sets.
     *
     * @param json the request's JSON input
     * @return the set of available chart resources
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response getChartResources(String json);
}
