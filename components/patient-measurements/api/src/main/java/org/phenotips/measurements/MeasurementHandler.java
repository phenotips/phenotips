/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.measurements;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.List;

/**
 * Handles measurements of a certain type: converting a measured value (in centimeters, kilograms or other units) at a
 * certain age (in months) into the corresponding percentile or standard deviation relative to a normal population, and
 * vice-versa. Most measurement types can distinguish between boys and girls.
 *
 * @version $Id$
 * @since 1.0M3
 */
@Unstable
@Role
public interface MeasurementHandler
{
    /**
     * Get the percentile for the given measured value and age.
     *
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param value the measured value, usually in centimeters or kilograms
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    int valueToPercentile(boolean male, float ageInMonths, double value);

    /**
     * Get the standard deviation for the given measured value and age.
     *
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param value the measured value, usually in centimeters or kilograms
     * @return a number specifying how many standard deviations does this measurement deviate from the mean
     */
    double valueToStandardDeviation(boolean male, float ageInMonths, double value);

    /**
     * Get the measurement that would correspond to the given percentile.
     *
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the measurement (usually in centimeters or kilograms) that falls in the middle of the target percentile,
     *         with the exception of the open ended 0 and 100 percentiles, for which the value corresponding to the
     *         0.25, respectively 99.75 percentage is returned
     */
    double percentileToValue(boolean male, float ageInMonths, int targetPercentile);

    /**
     * Get the measurement that would correspond to the given standard deviation.
     *
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetDeviation a number specifying the target deviation
     * @return the measurement (usually in centimeters or kilograms) that falls on the target standard deviation
     */
    double standardDeviationToValue(boolean male, float ageInMonths, double targetDeviation);

    /**
     * Some measurements should be taken on both sides of the body, since they can differ, for example different left
     * and right ear lengths. This method is used to indicate those measurements.
     *
     * @return {@code true} if measurements on both sides should be recorded
     */
    boolean isDoubleSided();

    /**
     * Get the list of charts configured for this type of measurement.
     *
     * @return a list of chart configurations, or an empty list if no charts are configured for this measurement
     */
    List<MeasurementsChartConfiguration> getChartsConfigurations();
}
