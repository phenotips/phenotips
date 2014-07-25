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
package org.phenotips.measurements.internal;

import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * BMI (Body Mass Index) measurements, in kilograms per square meter.
 *
 * @version $Id$
 * @since 1.0M3
 */
@Component
@Named("bmi")
@Singleton
public class BMIMeasurementHandler extends AbstractMeasurementHandler
{
    @Override
    public String getName()
    {
        return "bmi";
    }

    /**
     * Compute the BMI (Body-Mass Index) for the given weigh and height. The formula is {@code weight / (height^2)}
     * multiplied by 10000 (to convert centimeters into meters).
     *
     * @param weightInKilograms the measured weight, in kilograms
     * @param heightInCentimeters the measured length for children under 24 months, or measured height for older
     *            children, in centimeters
     * @return the BMI value
     */
    public double computeBMI(double weightInKilograms, double heightInCentimeters)
    {
        if (heightInCentimeters <= 0 || weightInKilograms <= 0) {
            return 0;
        }
        return weightInKilograms * 10000 / (heightInCentimeters * heightInCentimeters);
    }

    /**
     * Get the BMI percentile for the given weight and height.
     *
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param weightInKilograms the measured weight, in kilograms
     * @param heightInCentimeters the measured length for children under 24 months, or measured height for older
     *            children, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int valueToPercentile(boolean male, int ageInMonths, double weightInKilograms, double heightInCentimeters)
    {
        return super.valueToPercentile(male, ageInMonths, computeBMI(weightInKilograms, heightInCentimeters));
    }

    /**
     * Get the BMI standard deviation for the given weight and height.
     *
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param weightInKilograms the measured weight, in kilograms
     * @param heightInCentimeters the measured length for children under 24 months, or measured height for older
     *            children, in centimeters
     * @return a number specifying how many standard deviations does this measurement deviate from the mean
     */
    public double valueToStandardDeviation(boolean male, int ageInMonths, double weightInKilograms,
        double heightInCentimeters)
    {
        return super.valueToStandardDeviation(male, ageInMonths, computeBMI(weightInKilograms, heightInCentimeters));
    }
}
