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

import org.phenotips.measurements.MeasurementsChartConfiguration;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

/**
 * A resource corresponding to a measurement chart containing zero or more data points.
 *
 * @version $Id$
 * @since 1.2M5
 */
public class ChartResource
{
    /** The type of measurement. */
    private String measurementType;

    /** The patient's sex. */
    private char sex;

    /** A set of chart data points for this measurement type. */
    private Map<Double, Double> agesToValues;

    /** Provides access to the XWiki data. */
    private DocumentAccessBridge docBridge;

    /** This chart's configuration object. */
    private MeasurementsChartConfiguration chartConfig;

    /**
     * Default constructor.
     *
     * @param type measurement type
     * @param sex patient sex
     * @param config chart configuration
     * @param bridge XWiki document access bridge
     */
    ChartResource(String type, char sex, MeasurementsChartConfiguration config, DocumentAccessBridge bridge)
    {
        this.agesToValues = new HashMap<>();
        this.measurementType = type;
        this.docBridge = bridge;
        this.chartConfig = config;
        this.sex = sex;
    }

    /**
     * @return this resource's chart configuration
     */
    public MeasurementsChartConfiguration getChartConfig()
    {
        return this.chartConfig;
    }

    /**
     * Add an age-value set, i.e. datapoint, to this chart resource.
     * @param age patient age
     * @param value value for measurement at this age
     */
    public void addAgeValue(Double age, Double value)
    {
        agesToValues.put(age, value);
    }

    /**
     * @return the URL to this chart's generated form
     */
    public String toString()
    {
        String docUrl = this.docBridge.getDocumentURL(
                new DocumentReference("xwiki", "PhenoTips", "ChartService"), "get", "", "", false);

        UriBuilder uriBuilder = UriBuilder.fromUri(docUrl);
        for (Map.Entry<Double, Double> ageToValue : this.agesToValues.entrySet()) {
            uriBuilder.queryParam("a", ageToValue.getKey());
            uriBuilder.queryParam(this.measurementType, ageToValue.getValue());
        }
        uriBuilder.queryParam("standalone", "1");
        uriBuilder.queryParam("n", this.measurementType);
        uriBuilder.queryParam("s", this.sex);
        uriBuilder.queryParam("f", "svg");

        return uriBuilder.toString();
    }
}
