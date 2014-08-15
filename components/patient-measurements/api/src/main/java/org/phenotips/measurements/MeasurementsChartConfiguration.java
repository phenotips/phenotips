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

/**
 * Configuration for a measurements chart, specifying settings such as age limits, the range of displayed values, labels
 * for the axes, etc.
 *
 * @version $Id$
 * @since 1.0M3
 */
public interface MeasurementsChartConfiguration
{
    /**
     * Get the measurement for which this chart is configured.
     *
     * @return the measurement identifier
     */
    String getMeasurementType();

    /**
     * Get the lower age limit displayed in this chart, in months. Measurements taken at an earlier age will not fit in
     * this chart, and will not be displayed.
     *
     * @return a positive number, {@code 0} for measurements at birth
     */
    int getLowerAgeLimit();

    /**
     * Get the upper age limit displayed in this chart, in months. Measurements taken at an older age will not fit in
     * this chart, and will not be displayed.
     *
     * @return a positive number, greater than {@link #getLowerAgeLimit() the lower age limit}
     */
    int getUpperAgeLimit();

    /**
     * How frequently to draw thin vertical grid lines on the chart.
     *
     * @return a positive number, should divide {@code upperAgeLimit - lowerAgeLimit}
     */
    int getAgeTickStep();

    /**
     * How frequently to draw thick vertical grid lines on the chart, labeled with the corresponding age. Must be a
     * multiple of {@link #getAgeTickStep() the age tick step}.
     *
     * @return a positive number, multiple of {@link #getAgeTickStep() the age tick step}
     */
    int getAgeLabelStep();

    /**
     * Get the lower value limit displayed in this chart. Measurements with a value smaller that this limit will not fit
     * in this chart, and will only be displayed as an out-of-range indicator at the bottom edge of the chart.
     *
     * @return a number
     */
    double getLowerValueLimit();

    /**
     * Get the upper value limit displayed in this chart. Measurements with a value greater that this limit will not fit
     * in this chart, and will only be displayed as an out-of-range indicator at the top edge of the chart.
     *
     * @return a number greater than {@link #getLowerValueLimit() the lower value limit}
     */
    double getUpperValueLimit();

    /**
     * How frequently to draw thin horizontal grid lines on the chart.
     *
     * @return a positive number, should divide {@code upperValueLimit - lowerValueLimit}
     */
    double getValueTickStep();

    /**
     * How frequently to draw thick horizontal grid lines on the chart, labeled with the corresponding value. Must be a
     * multiple of {@link #getValueTickStep() the value tick step}.
     *
     * @return a positive number, multiple of {@link #getValueTickStep() the value tick step}
     */
    double getValueLabelStep();

    /**
     * The chart title, displayed at the top of the chart.
     *
     * @return a non-empty string
     */
    String getChartTitle();

    /**
     * The chart top label, describing the X axis (age).
     *
     * @return a non-empty string, usually the same as {@link #getBottomLabel() the bottom label}
     */
    String getTopLabel();

    /**
     * The chart bottom label, describing the X axis (age).
     *
     * @return a non-empty string, usually the same as {@link #getTopLabel() the top label}
     */
    String getBottomLabel();

    /**
     * The chart left label, describing the Y axis (measurement value). Can be different from the right label, when the
     * measurement method (and thus actual anthropometric feature) is slightly different.
     *
     * @return a non-empty string, usually the same as {@link #getRightLabel() the right label}
     */
    String getLeftLabel();

    /**
     * The chart right label, describing the Y axis (measurement value). Can be different from the left label, when the
     * measurement method (and thus actual anthropometric feature) is slightly different.
     *
     * @return a non-empty string, usually the same as {@link #getRightLabel() the left label}
     */
    String getRightLabel();
}
