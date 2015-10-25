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

import org.joda.time.Period;

/**
 * Utility class for measurement handlers.
 *
 * @version $Id$
 * @since 1.2M5
 */
public final class MeasurementUtils {
    /** Fuzzy value representing a measurement value considered extremely below normal. */
    private static final String VALUE_EXTREME_BELOW_NORMAL = "extreme-below-normal";

    /** Fuzzy value representing a measurement value considered below normal, but not extremely. */
    private static final String VALUE_BELOW_NORMAL = "below-normal";

    /** Fuzzy value representing a measurement value considered normal. */
    private static final String VALUE_NORMAL = "normal";

    /** Fuzzy value representing a measurement value considered above normal, but not extremely. */
    private static final String VALUE_ABOVE_NORMAL = "above-normal";

    /** Fuzzy value representing a measurement value considered extremely above normal. */
    private static final String VALUE_EXTREME_ABOVE_NORMAL = "extreme-above-normal";

    /** Private default constructor, so that this utility class can't be instantiated. */
    private MeasurementUtils()
    {
        // Nothing to do
    }

    /**
     * Get the number of months corresponding to a period string.
     *
     * @param age the ISO-8601 period string, without leading 'P'
     * @throws IllegalArgumentException if the age cannot be parsed
     * @return number of months
     */
    public static Double convertAgeStrToNumMonths(String age) throws IllegalArgumentException
    {
        Period agePeriod;
        agePeriod = Period.parse("P" + age);

        Double ageMonths = 0.0;
        ageMonths += agePeriod.getYears() * 12;
        ageMonths += agePeriod.getMonths();
        ageMonths += agePeriod.getWeeks() * 7 / 30.4375;
        ageMonths += agePeriod.getDays() / 30.4375;

        return ageMonths;
    }

    /**
     * Convert a percentile number into a string grossly describing the value.
     *
     * @param percentile a number between 0 and 100
     * @return the percentile description
     */
    public static String getFuzzyValue(int percentile)
    {
        String returnValue = VALUE_NORMAL;
        if (percentile <= 1) {
            returnValue = VALUE_EXTREME_BELOW_NORMAL;
        } else if (percentile <= 3) {
            returnValue = VALUE_BELOW_NORMAL;
        } else if (percentile >= 99) {
            returnValue = VALUE_EXTREME_ABOVE_NORMAL;
        } else if (percentile >= 97) {
            returnValue = VALUE_ABOVE_NORMAL;
        }
        return returnValue;
    }

    /**
     * Convert a standard deviation number into a string grossly describing the value.
     *
     * @param deviation standard deviation value
     * @return the deviation description
     */
    public static String getFuzzyValue(double deviation)
    {
        String returnValue = VALUE_NORMAL;
        if (deviation <= -3.0) {
            returnValue = VALUE_EXTREME_BELOW_NORMAL;
        } else if (deviation <= -2.0) {
            returnValue = VALUE_BELOW_NORMAL;
        } else if (deviation >= 3.0) {
            returnValue = VALUE_EXTREME_ABOVE_NORMAL;
        } else if (deviation >= 2.0) {
            returnValue = VALUE_ABOVE_NORMAL;
        }
        return returnValue;
    }
}
