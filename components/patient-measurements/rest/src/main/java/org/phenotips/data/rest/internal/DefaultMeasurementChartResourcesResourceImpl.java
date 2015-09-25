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
import org.phenotips.measurements.internal.AbstractMeasurementHandler;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.json.JSONArray;
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
    /** Provides access to the XWiki data. */
    @Inject
    private DocumentAccessBridge bridge;

    /** The set of chart resource objects corresponding to this request. */
    private List<ChartResource> charts;

    @Override
    public Response getChartResources(String json)
    {
        JSONObject reqObj = JSONObject.fromObject(json);
        this.generateChartResources(reqObj);
        return this.getResponse();
    }

    /**
     * Generate the set of chart resource objects corresponding to this request, based on the available chart
     * configurations.
     *
     * @param reqObj the JSON request object
     * @throws WebApplicationException if an age cannot be parsed
     */
    private void generateChartResources(JSONObject reqObj) throws WebApplicationException
    {
        String sex = reqObj.getString("sex");
        JSONArray measurementSets = reqObj.getJSONArray("measurementSets");
        this.charts = new LinkedList<>();
        for (Map.Entry<String, MeasurementHandler> entry : this.handlers.entrySet()) {
            for (MeasurementsChartConfiguration config : entry.getValue().getChartsConfigurations()) {
                ChartResource chart = null;

                for (int i = 0; i < measurementSets.size(); i++) {
                    JSONObject measurementSet = measurementSets.getJSONObject(i);
                    String age = measurementSet.getString("age");
                    JSONObject measurements = measurementSet.getJSONObject("measurements");
                    double ageMonths;
                    try {
                        ageMonths = AbstractMeasurementHandler.convertAgeStrToNumMonths(age);
                    } catch (IllegalArgumentException e) {
                        throw new WebApplicationException(generateErrorResponse(Response.Status.BAD_REQUEST,
                                "Cannot parse age."));
                    }

                    if (config.getLowerAgeLimit() <= ageMonths && config.getUpperAgeLimit() >= ageMonths
                            && measurements.containsKey(config.getMeasurementType())) {
                        if (chart == null) {
                            chart = new ChartResource(config.getMeasurementType(), sex.charAt(0), config, this.bridge);
                        }

                        Double value = measurements.getDouble(config.getMeasurementType());
                        chart.addAgeValue(ageMonths, value);
                    }
                }

                if (chart != null) {
                    this.charts.add(chart);
                }
            }
        }
    }

    /**
     * Get the response based on the generated chart resource objects.
     *
     * @return the response
     */
    private Response getResponse()
    {
        JSONArray chartsJson = new JSONArray();
        for (ChartResource chart : this.charts) {
            JSONObject chartJson = new JSONObject();
            chartJson.accumulate("title", chart.getChartConfig().getChartTitle());
            chartJson.accumulate("url", chart.toString());
            chartsJson.add(chartJson);
        }
        JSONObject resp = new JSONObject();
        resp.accumulate("charts", chartsJson);

        return Response.ok(resp, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
