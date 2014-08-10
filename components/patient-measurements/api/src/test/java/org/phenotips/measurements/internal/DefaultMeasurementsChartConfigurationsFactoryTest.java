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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for the {@link DefaultMeasurementsChartConfigurationsFactory} component.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class DefaultMeasurementsChartConfigurationsFactoryTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementsChartConfigurationsFactory> mocker =
        new MockitoComponentMockingRule<MeasurementsChartConfigurationsFactory>(
            DefaultMeasurementsChartConfigurationsFactory.class);

    @Test
    public void loadChartDiscardInvalidConfigurations() throws ComponentLookupException
    {
        List<MeasurementsChartConfiguration> settings =
            this.mocker.getComponentUnderTest().loadConfigurationsForMeasurementType("tested");
        // 7 are listed, but only 5 are valid
        Assert.assertEquals(5, settings.size());
    }

    @Test
    public void simpleLoadChart() throws ComponentLookupException
    {
        List<MeasurementsChartConfiguration> settings =
            this.mocker.getComponentUnderTest().loadConfigurationsForMeasurementType("tested");

        Assert.assertEquals("tested", settings.get(0).getMeasurementType());
        Assert.assertEquals(12, settings.get(0).getLowerAgeLimit());
        Assert.assertEquals(36, settings.get(0).getUpperAgeLimit());
        Assert.assertEquals(3, settings.get(0).getAgeTickStep());
        Assert.assertEquals(6, settings.get(0).getAgeLabelStep());
        Assert.assertEquals(12.2, settings.get(0).getLowerValueLimit(), 1.0E-2);
        Assert.assertEquals(22.2, settings.get(0).getUpperValueLimit(), 1.0E-2);
        Assert.assertEquals(0.5, settings.get(0).getValueTickStep(), 1.0E-2);
        Assert.assertEquals(2.0, settings.get(0).getValueLabelStep(), 1.0E-2);
        Assert.assertEquals("Weight for age, 1 to 3 years", settings.get(0).getChartTitle());
        Assert.assertEquals("Age (months)", settings.get(0).getTopLabel());
        Assert.assertEquals("Age (monthly)", settings.get(0).getBottomLabel());
        Assert.assertEquals("Weight (kg)", settings.get(0).getLeftLabel());
        Assert.assertEquals("Weight (kilos)", settings.get(0).getRightLabel());
    }

    @Test
    public void loadChartWithDefaultValues() throws ComponentLookupException
    {
        List<MeasurementsChartConfiguration> settings =
            this.mocker.getComponentUnderTest().loadConfigurationsForMeasurementType("tested");

        Assert.assertEquals("tested", settings.get(1).getMeasurementType());
        Assert.assertEquals(0, settings.get(1).getLowerAgeLimit());
        Assert.assertEquals(240, settings.get(1).getUpperAgeLimit());
        Assert.assertEquals(6, settings.get(1).getAgeTickStep());
        Assert.assertEquals(12, settings.get(1).getAgeLabelStep());
        Assert.assertEquals(0.0, settings.get(1).getLowerValueLimit(), 1.0E-2);
        Assert.assertEquals(20.0, settings.get(1).getUpperValueLimit(), 1.0E-2);
        Assert.assertEquals(0.2, settings.get(1).getValueTickStep(), 1.0E-2);
        Assert.assertEquals(1.0, settings.get(1).getValueLabelStep(), 1.0E-2);
        Assert.assertEquals("Weight for age, birth to 20 years", settings.get(1).getChartTitle());
        Assert.assertEquals("Age (years)", settings.get(1).getTopLabel());
        Assert.assertEquals("Age (years)", settings.get(1).getBottomLabel());
        Assert.assertEquals("Weight (kg)", settings.get(1).getLeftLabel());
        Assert.assertEquals("Weight (kg)", settings.get(1).getRightLabel());
    }

    @Test
    public void loadChartWithOneSidedLabels() throws ComponentLookupException
    {
        List<MeasurementsChartConfiguration> settings =
            this.mocker.getComponentUnderTest().loadConfigurationsForMeasurementType("tested");

        Assert.assertEquals("Age labels", settings.get(2).getTopLabel());
        Assert.assertEquals("Age labels", settings.get(2).getBottomLabel());
        Assert.assertEquals("Measurement labels", settings.get(2).getLeftLabel());
        Assert.assertEquals("Measurement labels", settings.get(2).getRightLabel());

        Assert.assertEquals("Age labels", settings.get(3).getTopLabel());
        Assert.assertEquals("Age labels", settings.get(3).getBottomLabel());
        Assert.assertEquals("Measurement labels", settings.get(3).getLeftLabel());
        Assert.assertEquals("Measurement labels", settings.get(3).getRightLabel());
    }

    @Test
    public void loadChartWithBrokenValues() throws ComponentLookupException
    {
        List<MeasurementsChartConfiguration> settings =
            this.mocker.getComponentUnderTest().loadConfigurationsForMeasurementType("tested");

        Assert.assertEquals("tested", settings.get(4).getMeasurementType());
        Mockito.verify(this.mocker.getMockedLogger()).warn("Invalid chart settings for [{}]: invalid integer [{}]",
            "charts.tested.configurations.broken.lowerAgeLimit", "two");
        Assert.assertEquals(0, settings.get(4).getLowerAgeLimit());
        Mockito.verify(this.mocker.getMockedLogger()).warn("Invalid chart settings for [{}]: invalid integer [{}]",
            "charts.tested.configurations.broken.upperAgeLimit", "36.5");
        Assert.assertEquals(240, settings.get(4).getUpperAgeLimit());
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: value should be a positive integer, was [{}]",
            "charts.tested.configurations.broken.ageTickStep", "-1");
        Assert.assertEquals(6, settings.get(4).getAgeTickStep());
        Mockito.verify(this.mocker.getMockedLogger()).warn("Invalid chart settings for [{}]: invalid integer [{}]",
            "charts.tested.configurations.broken.ageLabelStep", "");
        Assert.assertEquals(12, settings.get(4).getAgeLabelStep());
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: value should be finite, was [{}]",
            "charts.tested.configurations.broken.lowerValueLimit", "NaN");
        Assert.assertEquals(0.0, settings.get(4).getLowerValueLimit(), 1.0E-2);
        Assert.assertEquals(24.0, settings.get(4).getUpperValueLimit(), 1.0E-2);
        Assert.assertEquals(0.2, settings.get(4).getValueTickStep(), 1.0E-2);
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: value should be finite, was [{}]",
            "charts.tested.configurations.broken.valueLabelStep", "Infinity");
        Assert.assertEquals(1.0, settings.get(4).getValueLabelStep(), 1.0E-2);
        Assert.assertEquals("\uFFFEWeight for age,b\u0123 to 2\u00000 years", settings.get(4).getChartTitle());
        Assert.assertEquals("Age (years)", settings.get(4).getTopLabel());
        Assert.assertEquals("Age (years)", settings.get(4).getBottomLabel());
        Assert.assertEquals("Weight (kg)", settings.get(4).getLeftLabel());
        Assert.assertEquals("Weight (kg)", settings.get(4).getRightLabel());
    }

    @Test
    public void loadChartWithInvalidConfiguration() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().loadConfigurationsForMeasurementType("tested");

        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: age limits missing or out of order: [{}] and [{}]",
            "charts.tested.configurations.invalid.", 36, 12);
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: age grid lines don't fit evenly: [{}] in [{}-{}]",
            "charts.tested.configurations.invalid.", 7, 36, 12);
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: major/minor age grid lines don't match: [{}]/[{}]",
            "charts.tested.configurations.invalid.", 5, 7);
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: value limits missing or out of order: [{}] and [{}]",
            "charts.tested.configurations.invalid.", 19.5, 1.0);
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: value should be positive, was [{}]",
            "charts.tested.configurations.invalid.valueTickStep", "-1");
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: value grid lines don't fit evenly: [{}] in [{}-{}]",
            "charts.tested.configurations.invalid.", 0.2, 19.5, 1.0);
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: major/minor value grid lines don't match: [{}]/[{}]",
            "charts.tested.configurations.invalid.", 1.1, 0.2);
        Mockito.verify(this.mocker.getMockedLogger()).warn("Invalid chart settings for [{}]: missing chart title",
            "charts.tested.configurations.invalid.");
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: missing left and right labels", "charts.tested.configurations.invalid.");
        Mockito.verify(this.mocker.getMockedLogger()).warn(
            "Invalid chart settings for [{}]: missing top and bottom labels", "charts.tested.configurations.invalid.");
    }

    @Test
    public void loadChartWithMissingMeasurement() throws ComponentLookupException
    {
        List<MeasurementsChartConfiguration> settings =
            this.mocker.getComponentUnderTest().loadConfigurationsForMeasurementType("missing");
        Assert.assertNotNull(settings);
        Assert.assertTrue(settings.isEmpty());
    }

    @Test
    public void loadChartWithUndefinedMeasurement() throws ComponentLookupException
    {
        List<MeasurementsChartConfiguration> settings =
            this.mocker.getComponentUnderTest().loadConfigurationsForMeasurementType("none");
        Assert.assertNotNull(settings);
        Assert.assertTrue(settings.isEmpty());
    }

    @Test
    public void loadChartWithInvalidMeasurement() throws ComponentLookupException
    {
        List<MeasurementsChartConfiguration> settings =
            this.mocker.getComponentUnderTest().loadConfigurationsForMeasurementType("invalid");
        Assert.assertNotNull(settings);
        Assert.assertTrue(settings.isEmpty());
    }

    @Test
    public void loadChartWithoutConfiguration() throws ComponentLookupException
    {
        List<MeasurementsChartConfiguration> settings =
            this.mocker.getComponentUnderTest().loadConfigurationsForMeasurementType("inexistent");
        Assert.assertNotNull(settings);
        Assert.assertTrue(settings.isEmpty());
    }
}
