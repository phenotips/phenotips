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

import org.phenotips.data.rest.MeasurementChartResourcesResource;
import org.phenotips.measurements.MeasurementHandler;
import org.phenotips.measurements.MeasurementsChartConfiguration;

import org.xwiki.component.annotation.Component;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.sf.json.JSONObject;

/**
 * Default implementation for {@link MeasurementChartResourcesResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultMeasurementChartResourcesResourceImpl")
@Singleton
public class DefaultMeasurementChartResourcesResourceImpl extends AbstractMeasurementRestResource implements
        MeasurementChartResourcesResource
{
    @Override
    public Response getChartResources(UriInfo uriInfo)
    {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        String sex = params.getFirst("sex");

        for (Map.Entry<String, MeasurementHandler> entry : this.handlers.entrySet()) {
            for (MeasurementsChartConfiguration config : entry.getValue().getChartsConfigurations()) {
                for (String age : params.get("age")) {
                    break;
                }
            }
        }

        JSONObject resp = new JSONObject();
        resp.accumulate("value", 12);

        return Response.ok(resp, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
