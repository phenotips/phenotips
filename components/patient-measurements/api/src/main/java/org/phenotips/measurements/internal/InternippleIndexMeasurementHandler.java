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
 * Internipple index measurements, as a unitless index.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("inIndex")
@Singleton
public class InternippleIndexMeasurementHandler extends AbstractMeasurementHandler implements ComputedMeasurementHandler
{
    @Override
    public String getName()
    {
        return "inIndex";
    }

    @Override
    public String getUnit()
    {
        return "";
    }

    @Override
    public Collection<String> getComputationDependencies()
    {
        return Arrays.asList("ind", "chest");
    }

    @Override
    public double handleComputation(MultivaluedMap<String, String> params) throws IllegalArgumentException
    {
        String ind = params.getFirst("ind");
        String chest = params.getFirst("chest");
        if (ind == null || chest == null) {
            throw new IllegalArgumentException("Computation arguments were not all provided");
        }

        try {
            double indInCentimeters = Double.parseDouble(ind);
            double chestInCentimeters = Double.parseDouble(chest);

            return compute(indInCentimeters, chestInCentimeters);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot parse computation arguments.");
        }
    }

    /**
     * Compute the US:LS ratio (Upper segment : Lower segment) for the given upper-segment and lower-segment lengths.
     * The formula is {@code (internipple distance * 100) / chest circumference}.
     *
     * @param indInCentimeters the measured internipple distance, in centimeters
     * @param chestInCentimeters the measured chest circumference, in centimeters
     * @return the internipple index value
     */
    @Override
    public double compute(double indInCentimeters, double chestInCentimeters)
    {
        if (indInCentimeters <= 0 || chestInCentimeters <= 0) {
            return 0;
        }
        return (indInCentimeters * 100) / chestInCentimeters;
    }
}
