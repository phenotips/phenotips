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
package org.phenotips.measurements.internal;

import org.phenotips.measurements.ComputedMeasurementHandler;

import org.xwiki.component.annotation.Component;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;

/**
 * US/LS (Upper-body-Segment : Lower-body-Segment) measurements, as a unitless ratio.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("usls")
@Singleton
public class USLSMeasurementHandler extends AbstractMeasurementHandler implements ComputedMeasurementHandler
{
    @Override
    public String getName()
    {
        return "usls";
    }

    @Override
    public String getUnit()
    {
        return "";
    }

    @Override
    public Collection<String> getComputationDependencies()
    {
        return Arrays.asList("upperSeg", "lowerSeg");
    }

    @Override
    public double handleComputation(MultivaluedMap<String, String> params) throws IllegalArgumentException
    {
        String upperSeg = params.getFirst("upperSeg");
        String lowerSeg = params.getFirst("lowerSeg");
        if (upperSeg == null || lowerSeg == null) {
            throw new IllegalArgumentException("Computation arguments were not all provided");
        }

        try {
            double upperSegInCentimeters = Double.parseDouble(upperSeg);
            double lowerSegInCentimeters = Double.parseDouble(lowerSeg);

            return compute(upperSegInCentimeters, lowerSegInCentimeters);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot parse computation arguments.");
        }
    }

    /**
     * Compute the US:LS ratio (Upper segment : Lower segment) for the given upper-segment and lower-segment lengths.
     * The formula is {@code upper-segment / lower-segment}.
     *
     * @param upperSegInCentimeters the measured upper-segment length, in centimeters
     * @param lowerSegInCentimeters the measured lower-segment length, in centimeters
     * @return the US/LS value
     */
    @Override
    public double compute(double upperSegInCentimeters, double lowerSegInCentimeters)
    {
        if (upperSegInCentimeters <= 0 || lowerSegInCentimeters <= 0) {
            return 0;
        }
        return upperSegInCentimeters / lowerSegInCentimeters;
    }

    /*
     * Since the US/LS ratio varies highly depending on race, percentile and standard deviation calculations
     * are not currently supported for this measurement.
     *
     * TODO: If race/ethnicity is ever implemented to affect measurement statistics,
     * add methods for centile and stdev calculations here.
     */
}
