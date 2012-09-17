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
package edu.toronto.cs.cidb.tools;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.csrf.internal.DefaultCSRFToken;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Tests for the {@link DefaultCSRFToken} component.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@MockingRequirement(PercentileTools.class)
public class PercentileToolsTest extends AbstractMockingComponentTestCase
{
    private PercentileTools getTool()
    {
        try {
            return (PercentileTools) this.getMockedComponent();
        } catch (ComponentLookupException ex) {
            return null;
        }
    }

    @Test
    public void testPercentileComputation()
    {
        Assert.assertEquals(0, getTool().valueToPercentile(Double.MIN_VALUE, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, getTool().valueToPercentile(-1, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, getTool().valueToPercentile(0, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, getTool().valueToPercentile(1, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(2, getTool().valueToPercentile(2.114041, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(5, getTool().valueToPercentile(2.179956, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(10, getTool().valueToPercentile(2.250293, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(25, getTool().valueToPercentile(2.374837, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(50, getTool().valueToPercentile(2.5244, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(75, getTool().valueToPercentile(2.686987, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(90, getTool().valueToPercentile(2.84566, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(95, getTool().valueToPercentile(2.946724, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(98, getTool().valueToPercentile(3.050268, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getTool().valueToPercentile(3.5, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getTool().valueToPercentile(20, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getTool().valueToPercentile(1000, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getTool().valueToPercentile(Double.MAX_VALUE, 2.5244, -0.3521, 0.09153));
    }

    @Test
    public void testValueComputation()
    {
        // Values taken from the CDC data tables (Weight for age, boys, 0.5 months)
        double x = getTool().percentileToValue(3, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.799548641, x, 1.0E-8);
        x = getTool().percentileToValue(5, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.964655655, x, 1.0E-8);
        x = getTool().percentileToValue(10, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(3.209510017, x, 1.0E-8);
        x = getTool().percentileToValue(25, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(3.597395573, x, 1.0E-8);
        x = getTool().percentileToValue(50, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.003106424, x, 1.0E-8);
        x = getTool().percentileToValue(75, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.387422565, x, 1.0E-8);
        x = getTool().percentileToValue(90, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.718161283, x, 1.0E-8);
        x = getTool().percentileToValue(95, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.910130108, x, 1.0E-8);
        x = getTool().percentileToValue(97, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.032624982, x, 1.0E-8);
        // Values taken from the CDC data tables (Weight for age, boys, 9.5 months)
        x = getTool().percentileToValue(3, 9.476500305, -0.1600954, 0.11218624);
        Assert.assertEquals(7.700624405, x, 1.0E-8);
        x = getTool().percentileToValue(90, 9.476500305, -0.1600954, 0.11218624);
        Assert.assertEquals(10.96017225, x, 1.0E-8);
        // Don't expect a child with +- Infinity kilograms...
        x = getTool().percentileToValue(0, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = getTool().percentileToValue(100, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
        // Correct out of range percentiles
        x = getTool().percentileToValue(Integer.MIN_VALUE, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = getTool().percentileToValue(-50, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = getTool().percentileToValue(1000, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
        x = getTool().percentileToValue(Integer.MAX_VALUE, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
    }

    @Test
    public void testGetBMI()
    {
        Assert.assertEquals(100.0, getTool().getBMI(100, 100));
        Assert.assertEquals(31.25, getTool().getBMI(80, 160));
        Assert.assertEquals(0.0, getTool().getBMI(0, 0));
        Assert.assertEquals(0.0, getTool().getBMI(80, 0));
        Assert.assertEquals(0.0, getTool().getBMI(0, 120));
        Assert.assertEquals(0.0, getTool().getBMI(-80, -160));
    }

    @Test
    public void testGetBMIPercentile()
    {
        Assert.assertEquals(50, getTool().getBMIPercentile(true, 0, 3.34, 49.9));
        Assert.assertEquals(50, getTool().getBMIPercentile(false, 0, 3.32, 49.9));
        Assert.assertEquals(0, getTool().getBMIPercentile(true, 0, 1, 1000));
        Assert.assertEquals(100, getTool().getBMIPercentile(true, 0, 1000, 1));
        Assert.assertEquals(0, getTool().getBMIPercentile(false, 0, 1, 1000));
        Assert.assertEquals(100, getTool().getBMIPercentile(false, 0, 1000, 1));
        Assert.assertEquals(0, getTool().getBMIPercentile(false, 0, 0, 0));
        Assert.assertEquals(10, getTool().getBMIPercentile(true, 42, 14.49, 100.0));
        Assert.assertEquals(90, getTool().getBMIPercentile(false, 42, 17.36, 100.0));
        Assert.assertEquals(0, getTool().getBMIPercentile(true, 100, 18, 130.0));
        Assert.assertEquals(100, getTool().getBMIPercentile(true, 100, 90, 110.0));
        Assert.assertEquals(16, getTool().getBMIPercentile(true, 349, 67.0, 181.0));
        Assert.assertEquals(0, getTool().getBMIPercentile(false, 359, 49.0, 173.0));
    }

    @Test
    public void testGetPercentileBMI()
    {
        Assert.assertEquals(13.4, getTool().getPercentileBMI(true, 0, 50), 1.0E-2);
        Assert.assertEquals(13.34, getTool().getPercentileBMI(false, 0, 50), 1.0E-2);
        Assert.assertEquals(10.36, getTool().getPercentileBMI(true, 0, 0), 1.0E-2);
        Assert.assertEquals(17.74, getTool().getPercentileBMI(true, 0, 100), 1.0E-2);
        Assert.assertEquals(10.3, getTool().getPercentileBMI(false, 0, 0), 1.0E-2);
        Assert.assertEquals(17.34, getTool().getPercentileBMI(false, 0, 100), 1.0E-2);
        Assert.assertEquals(23.04, getTool().getPercentileBMI(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(22.07, getTool().getPercentileBMI(true, 349, 37), 1.0E-2);
        Assert.assertEquals(18.7, getTool().getPercentileBMI(false, 359, 12), 1.0E-2);
    }

    @Test
    public void testGetWeightPercentile()
    {
        Assert.assertEquals(50, getTool().getWeightPercentile(true, 0, 4.0));
        Assert.assertEquals(50, getTool().getWeightPercentile(false, 0, 3.8));
        Assert.assertEquals(0, getTool().getWeightPercentile(true, 0, 0));
        Assert.assertEquals(100, getTool().getWeightPercentile(true, 0, 1000));
        Assert.assertEquals(0, getTool().getWeightPercentile(false, 0, 0));
        Assert.assertEquals(100, getTool().getWeightPercentile(false, 0, 1000));
        Assert.assertEquals(50, getTool().getWeightPercentile(true, 1000, 70.6));
        Assert.assertEquals(37, getTool().getWeightPercentile(true, 349, 67.0));
        Assert.assertEquals(12, getTool().getWeightPercentile(false, 359, 49.0));
    }

    @Test
    public void testGetPercentileWeight()
    {
        Assert.assertEquals(4.0, getTool().getPercentileWeight(true, 0, 50), 1.0E-2);
        Assert.assertEquals(3.8, getTool().getPercentileWeight(false, 0, 50), 1.0E-2);
        Assert.assertEquals(2.09, getTool().getPercentileWeight(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.5, getTool().getPercentileWeight(true, 0, 100), 1.0E-2);
        Assert.assertEquals(2.2, getTool().getPercentileWeight(false, 0, 0), 1.0E-2);
        Assert.assertEquals(5.18, getTool().getPercentileWeight(false, 0, 100), 1.0E-2);
        Assert.assertEquals(70.6, getTool().getPercentileWeight(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(67.0, getTool().getPercentileWeight(true, 349, 37), 1.0E-2);
        Assert.assertEquals(49.04, getTool().getPercentileWeight(false, 359, 12), 1.0E-2);
    }

    @Test
    public void testGetICDPercentile()
    {
        Assert.assertEquals(50, getTool().getInnerCanthalDistancePercentile(true, 0, 2));
        Assert.assertEquals(0, getTool().getInnerCanthalDistancePercentile(true, 0, 0));
        Assert.assertEquals(100, getTool().getInnerCanthalDistancePercentile(true, 0, 1000));
        Assert.assertEquals(50, getTool().getInnerCanthalDistancePercentile(true, 1000, 3.1357));
        Assert.assertEquals(5, getTool().getInnerCanthalDistancePercentile(true, 16, 2.0475));
        Assert.assertEquals(50, getTool().getInnerCanthalDistancePercentile(true, 16, 2.5825));
        Assert.assertEquals(95, getTool().getInnerCanthalDistancePercentile(true, 16, 3.0485));
        Assert.assertEquals(50, getTool().getInnerCanthalDistancePercentile(true, 30, 2.6925));
    }

    @Test
    public void testGetPercentileICD()
    {
        Assert.assertEquals(2, getTool().getPercentileInnerCanthalDistance(true, 0, 50), 1.0E-2);
        Assert.assertEquals(1.17, getTool().getPercentileInnerCanthalDistance(true, 0, 0), 1.0E-2);
        Assert.assertEquals(2.85, getTool().getPercentileInnerCanthalDistance(true, 0, 100), 1.0E-2);
        Assert.assertEquals(3.1275, getTool().getPercentileInnerCanthalDistance(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(2.06, getTool().getPercentileInnerCanthalDistance(true, 16, 5), 1.0E-2);
        Assert.assertEquals(2.5825, getTool().getPercentileInnerCanthalDistance(true, 16, 50), 1.0E-2);
        Assert.assertEquals(3.03, getTool().getPercentileInnerCanthalDistance(true, 16, 95), 1.0E-2);
        Assert.assertEquals(2.6925, getTool().getPercentileInnerCanthalDistance(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetIPDPercentile()
    {
        Assert.assertEquals(50, getTool().getInterpupilaryDistancePercentile(true, 0, 3.91));
        Assert.assertEquals(0, getTool().getInterpupilaryDistancePercentile(true, 0, 0));
        Assert.assertEquals(100, getTool().getInterpupilaryDistancePercentile(true, 0, 1000));
        Assert.assertEquals(50, getTool().getInterpupilaryDistancePercentile(true, 1000, 6.13));
        Assert.assertEquals(3, getTool().getInterpupilaryDistancePercentile(true, 36, 4.23));
        Assert.assertEquals(50, getTool().getInterpupilaryDistancePercentile(true, 36, 4.835));
        Assert.assertEquals(97, getTool().getInterpupilaryDistancePercentile(true, 36, 5.49));
        Assert.assertEquals(50, getTool().getInterpupilaryDistancePercentile(true, 30, 4.7825));
    }

    @Test
    public void testGetPercentileIPD()
    {
        Assert.assertEquals(3.91, getTool().getPercentileInterpupilaryDistance(true, 0, 50), 1.0E-2);
        Assert.assertEquals(3.04, getTool().getPercentileInterpupilaryDistance(true, 0, 0), 1.0E-2);
        Assert.assertEquals(4.98, getTool().getPercentileInterpupilaryDistance(true, 0, 100), 1.0E-2);
        Assert.assertEquals(6.13, getTool().getPercentileInterpupilaryDistance(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(4.23, getTool().getPercentileInterpupilaryDistance(true, 36, 3), 1.0E-2);
        Assert.assertEquals(4.835, getTool().getPercentileInterpupilaryDistance(true, 36, 50), 1.0E-2);
        Assert.assertEquals(5.49, getTool().getPercentileInterpupilaryDistance(true, 36, 97), 1.0E-2);
        Assert.assertEquals(4.7825, getTool().getPercentileInterpupilaryDistance(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetOCDPercentile()
    {
        Assert.assertEquals(50, getTool().getOuterCanthalDistancePercentile(true, 0, 6.3));
        Assert.assertEquals(0, getTool().getOuterCanthalDistancePercentile(true, 0, 0));
        Assert.assertEquals(100, getTool().getOuterCanthalDistancePercentile(true, 0, 1000));
        Assert.assertEquals(50, getTool().getOuterCanthalDistancePercentile(true, 1000, 9.08));
        Assert.assertEquals(3, getTool().getOuterCanthalDistancePercentile(true, 16, 6.27));
        Assert.assertEquals(50, getTool().getOuterCanthalDistancePercentile(true, 16, 7.305));
        Assert.assertEquals(97, getTool().getOuterCanthalDistancePercentile(true, 16, 8.33));
        Assert.assertEquals(50, getTool().getOuterCanthalDistancePercentile(true, 30, 7.4725));
    }

    @Test
    public void testGetPercentileOCD()
    {
        Assert.assertEquals(6.3, getTool().getPercentileOuterCanthalDistance(true, 0, 50), 1.0E-2);
        Assert.assertEquals(4.86, getTool().getPercentileOuterCanthalDistance(true, 0, 0), 1.0E-2);
        Assert.assertEquals(7.98, getTool().getPercentileOuterCanthalDistance(true, 0, 100), 1.0E-2);
        Assert.assertEquals(9.08, getTool().getPercentileOuterCanthalDistance(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(6.27, getTool().getPercentileOuterCanthalDistance(true, 16, 3), 1.0E-2);
        Assert.assertEquals(7.305, getTool().getPercentileOuterCanthalDistance(true, 16, 50), 1.0E-2);
        Assert.assertEquals(8.33, getTool().getPercentileOuterCanthalDistance(true, 16, 97), 1.0E-2);
        Assert.assertEquals(7.4725, getTool().getPercentileOuterCanthalDistance(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetEarLengthPercentile()
    {
        Assert.assertEquals(50, getTool().getEarLengthPercentile(true, 0, 4.04));
        Assert.assertEquals(0, getTool().getEarLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, getTool().getEarLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, getTool().getEarLengthPercentile(true, 1000, 6.0825));
        Assert.assertEquals(5, getTool().getEarLengthPercentile(true, 36, 4.51));
        Assert.assertEquals(50, getTool().getEarLengthPercentile(true, 36, 5.115));
        Assert.assertEquals(95, getTool().getEarLengthPercentile(true, 36, 5.86));
        Assert.assertEquals(50, getTool().getEarLengthPercentile(true, 30, 5.015));
    }

    @Test
    public void testGetPercentileEarLength()
    {
        Assert.assertEquals(4.04, getTool().getPercentileEarLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(3, getTool().getPercentileEarLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.21, getTool().getPercentileEarLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(6.0825, getTool().getPercentileEarLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(4.51, getTool().getPercentileEarLength(true, 36, 5), 1.0E-2);
        Assert.assertEquals(5.115, getTool().getPercentileEarLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(5.86, getTool().getPercentileEarLength(true, 36, 95), 1.0E-2);
        Assert.assertEquals(5.015, getTool().getPercentileEarLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetPalpebralFissureLengthPercentile()
    {
        Assert.assertEquals(50, getTool().getPalpebralFissureLengthPercentile(true, 0, 1.9));
        Assert.assertEquals(0, getTool().getPalpebralFissureLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, getTool().getPalpebralFissureLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, getTool().getPalpebralFissureLengthPercentile(true, 1000, 3.13));
        Assert.assertEquals(5, getTool().getPalpebralFissureLengthPercentile(true, 36, 2.215));
        Assert.assertEquals(50, getTool().getPalpebralFissureLengthPercentile(true, 36, 2.49));
        Assert.assertEquals(95, getTool().getPalpebralFissureLengthPercentile(true, 36, 2.78));
        Assert.assertEquals(50, getTool().getPalpebralFissureLengthPercentile(true, 30, 2.4325));
    }

    @Test
    public void testGetPercentilePalpebralFissureLength()
    {
        Assert.assertEquals(1.9, getTool().getPercentilePalpebralFissureLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(1.6, getTool().getPercentilePalpebralFissureLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(2.28, getTool().getPercentilePalpebralFissureLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(3.13, getTool().getPercentilePalpebralFissureLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(2.215, getTool().getPercentilePalpebralFissureLength(true, 36, 5), 1.0E-2);
        Assert.assertEquals(2.49, getTool().getPercentilePalpebralFissureLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(2.78, getTool().getPercentilePalpebralFissureLength(true, 36, 95), 1.0E-2);
        Assert.assertEquals(2.4325, getTool().getPercentilePalpebralFissureLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetHandLengthPercentile()
    {
        Assert.assertEquals(-1, getTool().getHandLengthPercentile(true, 0, 6.3));
        Assert.assertEquals(-1, getTool().getHandLengthPercentile(true, 0, 0));
        Assert.assertEquals(-1, getTool().getHandLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, getTool().getHandLengthPercentile(true, 24, 10.5));
        Assert.assertEquals(0, getTool().getHandLengthPercentile(true, 24, 0));
        Assert.assertEquals(100, getTool().getHandLengthPercentile(true, 24, 1000));
        Assert.assertEquals(50, getTool().getHandLengthPercentile(true, 1000, 19.25));
        Assert.assertEquals(3, getTool().getHandLengthPercentile(true, 36, 9.95));
        Assert.assertEquals(50, getTool().getHandLengthPercentile(true, 36, 11.3));
        Assert.assertEquals(97, getTool().getHandLengthPercentile(true, 36, 12.45));
        Assert.assertEquals(50, getTool().getHandLengthPercentile(true, 30, 10.9));
    }

    @Test
    public void testGetPercentileHandLength()
    {
        Assert.assertEquals(-1, getTool().getPercentileHandLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(-1, getTool().getPercentileHandLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(-1, getTool().getPercentileHandLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(10.5, getTool().getPercentileHandLength(true, 24, 50), 1.0E-2);
        Assert.assertEquals(8.33, getTool().getPercentileHandLength(true, 24, 0), 1.0E-2);
        Assert.assertEquals(12.08, getTool().getPercentileHandLength(true, 24, 100), 1.0E-2);
        Assert.assertEquals(19.25, getTool().getPercentileHandLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(9.95, getTool().getPercentileHandLength(true, 36, 3), 1.0E-2);
        Assert.assertEquals(11.3, getTool().getPercentileHandLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(12.45, getTool().getPercentileHandLength(true, 36, 97), 1.0E-2);
        Assert.assertEquals(10.9, getTool().getPercentileHandLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetPalmLengthPercentile()
    {
        Assert.assertEquals(50, getTool().getPalmLengthPercentile(true, 0, 3.9));
        Assert.assertEquals(0, getTool().getPalmLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, getTool().getPalmLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, getTool().getPalmLengthPercentile(true, 1000, 11.225));
        Assert.assertEquals(3, getTool().getPalmLengthPercentile(true, 36, 5.625));
        Assert.assertEquals(50, getTool().getPalmLengthPercentile(true, 36, 6.475));
        Assert.assertEquals(97, getTool().getPalmLengthPercentile(true, 36, 7.3));
        Assert.assertEquals(50, getTool().getPalmLengthPercentile(true, 30, 6.237));
    }

    @Test
    public void testGetPercentilePalmLength()
    {
        Assert.assertEquals(3.9, getTool().getPercentilePalmLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(2.56, getTool().getPercentilePalmLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.24, getTool().getPercentilePalmLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(11.225, getTool().getPercentilePalmLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(5.625, getTool().getPercentilePalmLength(true, 36, 3), 1.0E-2);
        Assert.assertEquals(6.475, getTool().getPercentilePalmLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(7.3, getTool().getPercentilePalmLength(true, 36, 97), 1.0E-2);
        Assert.assertEquals(6.237, getTool().getPercentilePalmLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetFootLengthPercentile()
    {
        Assert.assertEquals(50, getTool().getFootLengthPercentile(true, 0, 7.5));
        Assert.assertEquals(50, getTool().getFootLengthPercentile(false, 0, 8.5));
        Assert.assertEquals(0, getTool().getFootLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, getTool().getFootLengthPercentile(true, 0, 1000));
        Assert.assertEquals(0, getTool().getFootLengthPercentile(false, 0, 0));
        Assert.assertEquals(100, getTool().getFootLengthPercentile(false, 0, 1000));
        Assert.assertEquals(3, getTool().getFootLengthPercentile(true, 36, 13.4));
        Assert.assertEquals(50, getTool().getFootLengthPercentile(true, 36, 15.2));
        Assert.assertEquals(97, getTool().getFootLengthPercentile(true, 36, 16.8));
        Assert.assertEquals(50, getTool().getFootLengthPercentile(true, 30, 14.5125));
        Assert.assertEquals(50, getTool().getFootLengthPercentile(true, 1000, 26.45));
        Assert.assertEquals(3, getTool().getFootLengthPercentile(false, 36, 13));
        Assert.assertEquals(50, getTool().getFootLengthPercentile(false, 36, 15.075));
        Assert.assertEquals(97, getTool().getFootLengthPercentile(false, 36, 16.95));
        Assert.assertEquals(50, getTool().getFootLengthPercentile(false, 30, 14.4875));
        Assert.assertEquals(50, getTool().getFootLengthPercentile(false, 1000, 23.975));
    }

    @Test
    public void testGetPercentileFootLength()
    {
        Assert.assertEquals(7.5, getTool().getPercentileFootLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(8.5, getTool().getPercentileFootLength(false, 0, 50), 1.0E-2);
        Assert.assertEquals(6.75, getTool().getPercentileFootLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(8.25, getTool().getPercentileFootLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(19.54, getTool().getPercentileFootLength(false, 300, 0), 1.0E-2);
        Assert.assertEquals(27.05, getTool().getPercentileFootLength(false, 300, 100), 1.0E-2);
        Assert.assertEquals(13.4, getTool().getPercentileFootLength(true, 36, 3), 1.0E-2);
        Assert.assertEquals(15.2, getTool().getPercentileFootLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(16.8, getTool().getPercentileFootLength(true, 36, 97), 1.0E-2);
        Assert.assertEquals(14.5125, getTool().getPercentileFootLength(true, 30, 50), 1.0E-2);
        Assert.assertEquals(26.45, getTool().getPercentileFootLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(13, getTool().getPercentileFootLength(false, 36, 3), 1.0E-2);
        Assert.assertEquals(15.075, getTool().getPercentileFootLength(false, 36, 50), 1.0E-2);
        Assert.assertEquals(16.95, getTool().getPercentileFootLength(false, 36, 97), 1.0E-2);
        Assert.assertEquals(14.4875, getTool().getPercentileFootLength(false, 30, 50), 1.0E-2);
        Assert.assertEquals(23.975, getTool().getPercentileFootLength(false, 1000, 50), 1.0E-2);
    }

    @Test
    public void testGetHeightPercentile()
    {
        Assert.assertEquals(50, getTool().getHeightPercentile(true, 0, 52.7));
        Assert.assertEquals(50, getTool().getHeightPercentile(false, 0, 51.68));
        Assert.assertEquals(0, getTool().getHeightPercentile(true, 0, 0));
        Assert.assertEquals(100, getTool().getHeightPercentile(true, 0, 1000));
        Assert.assertEquals(0, getTool().getHeightPercentile(false, 0, 0));
        Assert.assertEquals(100, getTool().getHeightPercentile(false, 0, 1000));
        Assert.assertEquals(50, getTool().getHeightPercentile(true, 1000, 176.85));
        Assert.assertEquals(72, getTool().getHeightPercentile(true, 349, 181.0));
        Assert.assertEquals(93, getTool().getHeightPercentile(false, 359, 173.0));
    }

    @Test
    public void testGetPercentileHeight()
    {
        Assert.assertEquals(52.7, getTool().getPercentileHeight(true, 0, 50), 1.0E-2);
        Assert.assertEquals(51.68, getTool().getPercentileHeight(false, 0, 50), 1.0E-2);
        Assert.assertEquals(45.73, getTool().getPercentileHeight(true, 0, 0), 1.0E-2);
        Assert.assertEquals(60.14, getTool().getPercentileHeight(true, 0, 100), 1.0E-2);
        Assert.assertEquals(45.61, getTool().getPercentileHeight(false, 0, 0), 1.0E-2);
        Assert.assertEquals(59.39, getTool().getPercentileHeight(false, 0, 100), 1.0E-2);
        Assert.assertEquals(176.85, getTool().getPercentileHeight(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(181.0, getTool().getPercentileHeight(true, 349, 72), 1.0E-2);
        Assert.assertEquals(172.86, getTool().getPercentileHeight(false, 359, 93), 1.0E-2);
    }

    @Test
    public void testGetFuzzyValue()
    {
        Assert.assertEquals("extreme-below-normal", getTool().getFuzzyValue(Integer.MIN_VALUE));
        Assert.assertEquals("extreme-below-normal", getTool().getFuzzyValue(-1));
        Assert.assertEquals("extreme-below-normal", getTool().getFuzzyValue(0));
        Assert.assertEquals("extreme-below-normal", getTool().getFuzzyValue(3));
        Assert.assertEquals("below-normal", getTool().getFuzzyValue(4));
        Assert.assertEquals("below-normal", getTool().getFuzzyValue(7));
        Assert.assertEquals("below-normal", getTool().getFuzzyValue(10));
        Assert.assertEquals("normal", getTool().getFuzzyValue(11));
        Assert.assertEquals("normal", getTool().getFuzzyValue(50));
        Assert.assertEquals("normal", getTool().getFuzzyValue(89));
        Assert.assertEquals("above-normal", getTool().getFuzzyValue(90));
        Assert.assertEquals("above-normal", getTool().getFuzzyValue(93));
        Assert.assertEquals("above-normal", getTool().getFuzzyValue(96));
        Assert.assertEquals("extreme-above-normal", getTool().getFuzzyValue(97));
        Assert.assertEquals("extreme-above-normal", getTool().getFuzzyValue(99));
        Assert.assertEquals("extreme-above-normal", getTool().getFuzzyValue(100));
        Assert.assertEquals("extreme-above-normal", getTool().getFuzzyValue(101));
        Assert.assertEquals("extreme-above-normal", getTool().getFuzzyValue(Integer.MAX_VALUE));
    }
}
