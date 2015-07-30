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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Resource for working with computed measurement values.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Path("/measurements/computed")
public interface ComputedMeasurementResource
{
    /**
     * Get a computed measurement based on a number of inputs required for the computation. The parameters are extracted
     * manually by the implementation, since computed values vary in their numbers of parameters.
     *
     * @param uriInfo the request's URI info
     * @return the computed measurement value
     */
    @GET
    Response getComputedMeasurement(@Context UriInfo uriInfo);
}
