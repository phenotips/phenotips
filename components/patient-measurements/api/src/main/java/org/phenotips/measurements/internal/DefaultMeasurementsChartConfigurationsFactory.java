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

import org.phenotips.measurements.MeasurementsChartConfiguration;
import org.phenotips.measurements.MeasurementsChartConfigurationsFactory;

import org.xwiki.component.annotation.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Implementation of {@link MeasurementsChartConfigurationsFactory} that uses a {@link ResourceBundle .properties} file
 * with the chart configurations.
 *
 * @version $Id$
 * @since 1.0M3
 */
@Component
public class DefaultMeasurementsChartConfigurationsFactory implements MeasurementsChartConfigurationsFactory
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /**
     * Simple read-only bean implementation of {@link MeasurementsChartConfiguration}, populated by
     * {@link DefaultMeasurementsChartConfigurationsFactory}.
     */
    private static final class SimpleMeasurementsChartConfiguration implements MeasurementsChartConfiguration
    {
        /** Internal identifier used for debugging. Same as the prefix used in the configuration file. */
        private final String chartIdentifier;

        /** @see #getMeasurementType() */
        private String measurementType;

        /** @see #getLowerAgeLimit() */
        private int lowerAgeLimit;

        /** @see #getUpperAgeLimit() */
        private int upperAgeLimit = 240;

        /** @see #getAgeTickStep() */
        private int ageTickStep = 6;

        /** @see #getAgeLabelStep() */
        private int ageLabelStep = 12;

        /** @see #getLowerValueLimit() */
        private double lowerValueLimit;

        /** @see #getUpperValueLimit() */
        private double upperValueLimit;

        /** @see #getValueTickStep() */
        private double valueTickStep = 0.2;

        /** @see #getValueLabelStep() */
        private double valueLabelStep = 1;

        /** @see #getChartTitle() */
        private String chartTitle;

        /** @see #getTopLabel() */
        private String topLabel;

        /** @see #getBottomLabel() */
        private String bottomLabel;

        /** @see #getLeftLabel() */
        private String leftLabel;

        /** @see #getRightLabel() */
        private String rightLabel;

        /**
         * Private constructor.
         *
         * @param identifier the chart identifier used, see {@link #chartIdentifier}
         */
        private SimpleMeasurementsChartConfiguration(String identifier)
        {
            this.chartIdentifier = identifier;
        }

        @Override
        public String getMeasurementType()
        {
            return this.measurementType;
        }

        @Override
        public int getLowerAgeLimit()
        {
            return this.lowerAgeLimit;
        }

        @Override
        public int getUpperAgeLimit()
        {
            return this.upperAgeLimit;
        }

        @Override
        public int getAgeTickStep()
        {
            return this.ageTickStep;
        }

        @Override
        public int getAgeLabelStep()
        {
            return this.ageLabelStep;
        }

        @Override
        public double getLowerValueLimit()
        {
            return this.lowerValueLimit;
        }

        @Override
        public double getUpperValueLimit()
        {
            return this.upperValueLimit;
        }

        @Override
        public double getValueTickStep()
        {
            return this.valueTickStep;
        }

        @Override
        public double getValueLabelStep()
        {
            return this.valueLabelStep;
        }

        @Override
        public String getChartTitle()
        {
            return StringUtils.defaultString(this.chartTitle);
        }

        @Override
        public String getTopLabel()
        {
            if (StringUtils.isEmpty(this.topLabel)) {
                return this.bottomLabel;
            }
            return this.topLabel;
        }

        @Override
        public String getBottomLabel()
        {
            return this.bottomLabel;
        }

        @Override
        public String getLeftLabel()
        {
            if (StringUtils.isEmpty(this.leftLabel)) {
                return this.rightLabel;
            }
            return this.leftLabel;
        }

        @Override
        public String getRightLabel()
        {
            if (StringUtils.isEmpty(this.rightLabel)) {
                return this.leftLabel;
            }
            return this.rightLabel;
        }
    }

    @Override
    public List<MeasurementsChartConfiguration> loadConfigurationsForMeasurementType(String measurementType)
    {
        ResourceBundle configuration = ResourceBundle.getBundle("measurementsChartsConfigurations");
        String key = "charts." + measurementType + ".configurations";
        if (!configuration.containsKey(key)) {
            return Collections.emptyList();
        }
        String[] charts = configuration.getString(key).split(",");
        List<MeasurementsChartConfiguration> result = new ArrayList<MeasurementsChartConfiguration>(charts.length);
        for (String chart : charts) {
            SimpleMeasurementsChartConfiguration chartSettings = loadChart(key + '.' + chart + '.', configuration);
            if (validateChart(chartSettings)) {
                chartSettings.measurementType = measurementType;
                result.add(chartSettings);
            }
        }
        return result;
    }

    /**
     * Load the settings for one chart.
     *
     * @param prefix the prefix for the configuration keys, in the format
     *            {@code charts.<measurement type>.configurations.<chart identifier>.}
     * @param configuration the resource bundle of the configuration
     * @return the configured settings
     */
    private SimpleMeasurementsChartConfiguration loadChart(String prefix, ResourceBundle configuration)
    {
        SimpleMeasurementsChartConfiguration result = new SimpleMeasurementsChartConfiguration(prefix);
        result.lowerAgeLimit = getIntSetting(prefix + "lowerAgeLimit", result.lowerAgeLimit, configuration);
        result.upperAgeLimit = getIntSetting(prefix + "upperAgeLimit", result.upperAgeLimit, configuration);
        result.ageTickStep = getIntSetting(prefix + "ageTickStep", result.ageTickStep, configuration);
        result.ageLabelStep = getIntSetting(prefix + "ageLabelStep", result.ageLabelStep, configuration);
        result.lowerValueLimit = getDoubleSetting(prefix + "lowerValueLimit", result.lowerValueLimit, configuration);
        result.upperValueLimit = getDoubleSetting(prefix + "upperValueLimit", result.upperValueLimit, configuration);
        result.valueTickStep = getPositiveDoubleSetting(prefix + "valueTickStep", result.valueTickStep, configuration);
        result.valueLabelStep =
            getPositiveDoubleSetting(prefix + "valueLabelStep", result.valueLabelStep, configuration);
        result.chartTitle = getStringSetting(prefix + "chartTitle", result.chartTitle, configuration);
        result.topLabel = getStringSetting(prefix + "topLabel", result.topLabel, configuration);
        result.bottomLabel =
            getStringSetting(prefix + "bottomLabel", StringUtils.defaultString(result.topLabel, "Age (years)"),
                configuration);
        result.leftLabel = getStringSetting(prefix + "leftLabel", result.leftLabel, configuration);
        result.rightLabel = getStringSetting(prefix + "rightLabel", result.rightLabel, configuration);
        return result;
    }

    /**
     * Validate that the configured settings are valid: limits are in order, labels are present, grid fits evenly.
     *
     * @param settings the settings read from the configuration file
     * @return {@code true} if the settings are valid, {@code false} otherwise; encountered problems are logged
     */
    private boolean validateChart(SimpleMeasurementsChartConfiguration settings)
    {
        boolean isValid = true;
        isValid = validateLimits(settings) && isValid;
        isValid = validateGrid(settings) && isValid;
        isValid = validateLabels(settings) && isValid;
        return isValid;
    }

    /**
     * Validate that the configured age and value limits are valid.
     *
     * @param settings the settings read from the configuration file
     * @return {@code true} if the settings are valid, {@code false} otherwise; encountered problems are logged
     */
    private boolean validateLimits(SimpleMeasurementsChartConfiguration settings)
    {
        boolean isValid = true;
        if (settings.lowerAgeLimit >= settings.upperAgeLimit) {
            this.logger.warn("Invalid chart settings for [{}]: age limits missing or out of order: [{}] and [{}]",
                settings.chartIdentifier, settings.lowerAgeLimit, settings.upperAgeLimit);
            isValid = false;
        }
        if (settings.lowerValueLimit >= settings.upperValueLimit) {
            this.logger.warn("Invalid chart settings for [{}]: value limits missing or out of order: [{}] and [{}]",
                settings.chartIdentifier, settings.lowerValueLimit, settings.upperValueLimit);
            isValid = false;
        }
        return isValid;
    }

    /**
     * Validate that the configured grid is valid.
     *
     * @param settings the settings read from the configuration file
     * @return {@code true} if the settings are valid, {@code false} otherwise; encountered problems are logged
     */
    private boolean validateGrid(SimpleMeasurementsChartConfiguration settings)
    {
        boolean isValid = true;
        if ((settings.upperAgeLimit - settings.lowerAgeLimit) % settings.ageTickStep != 0) {
            this.logger.warn("Invalid chart settings for [{}]: age grid lines don't fit evenly: [{}] in [{}-{}]",
                settings.chartIdentifier, settings.ageTickStep, settings.lowerAgeLimit, settings.upperAgeLimit);
            isValid = false;
        }
        if (settings.ageLabelStep % settings.ageTickStep != 0) {
            this.logger.warn("Invalid chart settings for [{}]: major/minor age grid lines don't match: [{}]/[{}]",
                settings.chartIdentifier, settings.ageLabelStep, settings.ageTickStep);
            isValid = false;
        }
        if (Math.abs(Math.IEEEremainder(settings.upperValueLimit - settings.lowerValueLimit, settings.valueTickStep))
            > 1.0E-10) {
            this.logger.warn("Invalid chart settings for [{}]: value grid lines don't fit evenly: [{}] in [{}-{}]",
                settings.chartIdentifier, settings.valueTickStep, settings.lowerValueLimit, settings.upperValueLimit);
            isValid = false;
        }
        if (Math.abs(Math.IEEEremainder(settings.valueLabelStep, settings.valueTickStep)) > 1.0E-10) {
            this.logger.warn("Invalid chart settings for [{}]: major/minor value grid lines don't match: [{}]/[{}]",
                settings.chartIdentifier, settings.valueLabelStep, settings.valueTickStep);
            isValid = false;
        }
        return isValid;
    }

    /**
     * Validate that all the labels are configured.
     *
     * @param settings the settings read from the configuration file
     * @return {@code true} if the settings are valid, {@code false} otherwise; encountered problems are logged
     */
    private boolean validateLabels(SimpleMeasurementsChartConfiguration settings)
    {
        boolean isValid = true;
        if (StringUtils.isBlank(settings.chartTitle)) {
            this.logger.warn("Invalid chart settings for [{}]: missing chart title", settings.chartIdentifier);
            isValid = false;
        }
        if (StringUtils.isBlank(settings.topLabel) && StringUtils.isBlank(settings.bottomLabel)) {
            this.logger
                .warn("Invalid chart settings for [{}]: missing top and bottom labels", settings.chartIdentifier);
            isValid = false;
        }
        if (StringUtils.isBlank(settings.leftLabel) && StringUtils.isBlank(settings.rightLabel)) {
            this.logger
                .warn("Invalid chart settings for [{}]: missing left and right labels", settings.chartIdentifier);
            isValid = false;
        }
        return isValid;
    }

    /**
     * Read and return a setting from the configuration, parsing it as an {@code int} number, falling back on the
     * provided default value.
     *
     * @param settingName the name of the setting to read
     * @param defaultValue the default value to use when there's no value specified in the configuration, or the
     *            specified value is not a valid number
     * @param configuration the configuration bundle with all the settings
     * @return the configured value, if one is configured as a valid {@code int} number, or the default value otherwise
     */
    private int getIntSetting(String settingName, int defaultValue, ResourceBundle configuration)
    {
        int result = defaultValue;
        if (configuration.containsKey(settingName)) {
            try {
                result = Integer.parseInt(configuration.getString(settingName));
                if (result < 0) {
                    this.logger.warn("Invalid chart settings for [{}]: value should be a positive integer, was [{}]",
                        settingName, configuration.getString(settingName));
                    result = defaultValue;
                }
            } catch (NumberFormatException ex) {
                // Fall back to the default value
                this.logger.warn("Invalid chart settings for [{}]: invalid integer [{}]", settingName,
                    configuration.getString(settingName));
            }
        }
        return result;
    }

    /**
     * Read and return a setting from the configuration, parsing it as a {@code double} number, falling back on the
     * provided default value.
     *
     * @param settingName the name of the setting to read
     * @param defaultValue the default value to use when there's no value specified in the configuration, or the
     *            specified value is not a valid double number
     * @param configuration the configuration bundle with all the settings
     * @return the configured value, if one is configured as a valid {@code double} number, or the default value
     *         otherwise
     */
    private double getDoubleSetting(String settingName, double defaultValue, ResourceBundle configuration)
    {
        double result = defaultValue;
        if (configuration.containsKey(settingName)) {
            try {
                result = Double.parseDouble(configuration.getString(settingName));
                if (Double.isNaN(result) || Double.isInfinite(result)) {
                    this.logger.warn("Invalid chart settings for [{}]: value should be finite, was [{}]", settingName,
                        configuration.getString(settingName));
                    result = defaultValue;
                }
            } catch (NumberFormatException ex) {
                // Fall back to the default value
                this.logger.warn("Invalid chart settings for [{}]: invalid double [{}]", settingName,
                    configuration.getString(settingName));
            }
        }
        return result;
    }

    /**
     * Read and return a setting from the configuration, parsing it as a positive {@code double} number, falling back on
     * the provided default value.
     *
     * @param settingName the name of the setting to read
     * @param defaultValue the default value to use when there's no value specified in the configuration, or the
     *            specified value is not a valid double number
     * @param configuration the configuration bundle with all the settings
     * @return the configured value, if one is configured as a valid {@code double} number, or the default value
     *         otherwise
     */
    private double getPositiveDoubleSetting(String settingName, double defaultValue, ResourceBundle configuration)
    {
        double result = getDoubleSetting(settingName, defaultValue, configuration);
        if (result < 0) {
            this.logger.warn("Invalid chart settings for [{}]: value should be positive, was [{}]", settingName,
                configuration.getString(settingName));
            result = defaultValue;
        }
        return result;
    }

    /**
     * Read and return a setting from the configuration, falling back on the provided default value.
     *
     * @param settingName the name of the setting to read
     * @param defaultValue the default value to use when there's no value specified in the configuration; can be
     *            {@code null} or the empty string
     * @param configuration the configuration bundle with all the settings
     * @return the configured value, if one is configured (even as an empty string), or the default value otherwise
     */
    private String getStringSetting(String settingName, String defaultValue, ResourceBundle configuration)
    {
        String result = defaultValue;
        if (configuration.containsKey(settingName)) {
            result = configuration.getString(settingName);
        }
        return result;
    }
}
