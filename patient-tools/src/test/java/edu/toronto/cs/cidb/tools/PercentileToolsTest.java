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
public class PercentileToolsTest extends AbstractMockingComponentTestCase<PercentileTools>
{
    @Test
    public void testPercentileComputation() throws ComponentLookupException
    {
        Assert.assertEquals(0, getMockedComponent().valueToPercentile(Double.MIN_VALUE, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, getMockedComponent().valueToPercentile(-1, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, getMockedComponent().valueToPercentile(0, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, getMockedComponent().valueToPercentile(1, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(2, getMockedComponent().valueToPercentile(2.114041, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(5, getMockedComponent().valueToPercentile(2.179956, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(10, getMockedComponent().valueToPercentile(2.250293, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(25, getMockedComponent().valueToPercentile(2.374837, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(50, getMockedComponent().valueToPercentile(2.5244, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(75, getMockedComponent().valueToPercentile(2.686987, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(90, getMockedComponent().valueToPercentile(2.84566, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(95, getMockedComponent().valueToPercentile(2.946724, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(98, getMockedComponent().valueToPercentile(3.050268, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getMockedComponent().valueToPercentile(3.5, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getMockedComponent().valueToPercentile(20, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getMockedComponent().valueToPercentile(1000, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getMockedComponent().valueToPercentile(Double.MAX_VALUE, 2.5244, -0.3521, 0.09153));
    }

    @Test
    public void testValueComputation() throws ComponentLookupException
    {
        // Values taken from the CDC data tables (Weight for age, boys, 0.5 months)
        double x = getMockedComponent().percentileToValue(3, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.799548641, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(5, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.964655655, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(10, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(3.209510017, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(25, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(3.597395573, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(50, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.003106424, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(75, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.387422565, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(90, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.718161283, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(95, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.910130108, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(97, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.032624982, x, 1.0E-8);
        // Values taken from the CDC data tables (Weight for age, boys, 9.5 months)
        x = getMockedComponent().percentileToValue(3, 9.476500305, -0.1600954, 0.11218624);
        Assert.assertEquals(7.700624405, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(90, 9.476500305, -0.1600954, 0.11218624);
        Assert.assertEquals(10.96017225, x, 1.0E-8);
        // Don't expect a child with +- Infinity kilograms...
        x = getMockedComponent().percentileToValue(0, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(100, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
        // Correct out of range percentiles
        x = getMockedComponent().percentileToValue(Integer.MIN_VALUE, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(-50, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(1000, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
        x = getMockedComponent().percentileToValue(Integer.MAX_VALUE, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
    }

    @Test
    public void testGetBMI() throws ComponentLookupException
    {
        Assert.assertEquals(100.0, getMockedComponent().getBMI(100, 100));
        Assert.assertEquals(31.25, getMockedComponent().getBMI(80, 160));
        Assert.assertEquals(0.0, getMockedComponent().getBMI(0, 0));
        Assert.assertEquals(0.0, getMockedComponent().getBMI(80, 0));
        Assert.assertEquals(0.0, getMockedComponent().getBMI(0, 120));
        Assert.assertEquals(0.0, getMockedComponent().getBMI(-80, -160));
    }

    @Test
    public void testGetBMIPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getMockedComponent().getBMIPercentile(true, 0, 3.34, 49.9));
        Assert.assertEquals(50, getMockedComponent().getBMIPercentile(false, 0, 3.32, 49.9));
        Assert.assertEquals(0, getMockedComponent().getBMIPercentile(true, 0, 1, 1000));
        Assert.assertEquals(100, getMockedComponent().getBMIPercentile(true, 0, 1000, 1));
        Assert.assertEquals(0, getMockedComponent().getBMIPercentile(false, 0, 1, 1000));
        Assert.assertEquals(100, getMockedComponent().getBMIPercentile(false, 0, 1000, 1));
        Assert.assertEquals(0, getMockedComponent().getBMIPercentile(false, 0, 0, 0));
        Assert.assertEquals(10, getMockedComponent().getBMIPercentile(true, 42, 14.49, 100.0));
        Assert.assertEquals(90, getMockedComponent().getBMIPercentile(false, 42, 17.36, 100.0));
        Assert.assertEquals(0, getMockedComponent().getBMIPercentile(true, 100, 18, 130.0));
        Assert.assertEquals(100, getMockedComponent().getBMIPercentile(true, 100, 90, 110.0));
        Assert.assertEquals(16, getMockedComponent().getBMIPercentile(true, 349, 67.0, 181.0));
        Assert.assertEquals(0, getMockedComponent().getBMIPercentile(false, 359, 49.0, 173.0));
    }

    @Test
    public void testGetPercentileBMI() throws ComponentLookupException
    {
        Assert.assertEquals(13.4, getMockedComponent().getPercentileBMI(true, 0, 50), 1.0E-2);
        Assert.assertEquals(13.34, getMockedComponent().getPercentileBMI(false, 0, 50), 1.0E-2);
        Assert.assertEquals(10.36, getMockedComponent().getPercentileBMI(true, 0, 0), 1.0E-2);
        Assert.assertEquals(17.74, getMockedComponent().getPercentileBMI(true, 0, 100), 1.0E-2);
        Assert.assertEquals(10.3, getMockedComponent().getPercentileBMI(false, 0, 0), 1.0E-2);
        Assert.assertEquals(17.34, getMockedComponent().getPercentileBMI(false, 0, 100), 1.0E-2);
        Assert.assertEquals(23.04, getMockedComponent().getPercentileBMI(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(22.07, getMockedComponent().getPercentileBMI(true, 349, 37), 1.0E-2);
        Assert.assertEquals(18.7, getMockedComponent().getPercentileBMI(false, 359, 12), 1.0E-2);
    }

    @Test
    public void testGetWeightPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getMockedComponent().getWeightPercentile(true, 0, 4.0));
        Assert.assertEquals(50, getMockedComponent().getWeightPercentile(false, 0, 3.8));
        Assert.assertEquals(0, getMockedComponent().getWeightPercentile(true, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getWeightPercentile(true, 0, 1000));
        Assert.assertEquals(0, getMockedComponent().getWeightPercentile(false, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getWeightPercentile(false, 0, 1000));
        Assert.assertEquals(50, getMockedComponent().getWeightPercentile(true, 1000, 70.6));
        Assert.assertEquals(37, getMockedComponent().getWeightPercentile(true, 349, 67.0));
        Assert.assertEquals(12, getMockedComponent().getWeightPercentile(false, 359, 49.0));
        Assert.assertEquals(-1, getMockedComponent().getWeightPercentile(true, -1, 4.0));
    }

    @Test
    public void testGetPercentileWeight() throws ComponentLookupException
    {
        Assert.assertEquals(4.0, getMockedComponent().getPercentileWeight(true, 0, 50), 1.0E-2);
        Assert.assertEquals(3.8, getMockedComponent().getPercentileWeight(false, 0, 50), 1.0E-2);
        Assert.assertEquals(2.09, getMockedComponent().getPercentileWeight(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.5, getMockedComponent().getPercentileWeight(true, 0, 100), 1.0E-2);
        Assert.assertEquals(2.2, getMockedComponent().getPercentileWeight(false, 0, 0), 1.0E-2);
        Assert.assertEquals(5.18, getMockedComponent().getPercentileWeight(false, 0, 100), 1.0E-2);
        Assert.assertEquals(70.6, getMockedComponent().getPercentileWeight(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(67.0, getMockedComponent().getPercentileWeight(true, 349, 37), 1.0E-2);
        Assert.assertEquals(49.04, getMockedComponent().getPercentileWeight(false, 359, 12), 1.0E-2);
    }

    @Test
    public void testGetICDPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getMockedComponent().getInnerCanthalDistancePercentile(true, 0, 2));
        Assert.assertEquals(0, getMockedComponent().getInnerCanthalDistancePercentile(true, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getInnerCanthalDistancePercentile(true, 0, 1000));
        Assert.assertEquals(50, getMockedComponent().getInnerCanthalDistancePercentile(true, 1000, 3.1357));
        Assert.assertEquals(5, getMockedComponent().getInnerCanthalDistancePercentile(true, 16, 2.0475));
        Assert.assertEquals(50, getMockedComponent().getInnerCanthalDistancePercentile(true, 16, 2.5825));
        Assert.assertEquals(95, getMockedComponent().getInnerCanthalDistancePercentile(true, 16, 3.0485));
        Assert.assertEquals(50, getMockedComponent().getInnerCanthalDistancePercentile(true, 30, 2.6925));
    }

    @Test
    public void testGetPercentileICD() throws ComponentLookupException
    {
        Assert.assertEquals(2, getMockedComponent().getPercentileInnerCanthalDistance(true, 0, 50), 1.0E-2);
        Assert.assertEquals(1.17, getMockedComponent().getPercentileInnerCanthalDistance(true, 0, 0), 1.0E-2);
        Assert.assertEquals(2.85, getMockedComponent().getPercentileInnerCanthalDistance(true, 0, 100), 1.0E-2);
        Assert.assertEquals(3.1275, getMockedComponent().getPercentileInnerCanthalDistance(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(2.06, getMockedComponent().getPercentileInnerCanthalDistance(true, 16, 5), 1.0E-2);
        Assert.assertEquals(2.5825, getMockedComponent().getPercentileInnerCanthalDistance(true, 16, 50), 1.0E-2);
        Assert.assertEquals(3.03, getMockedComponent().getPercentileInnerCanthalDistance(true, 16, 95), 1.0E-2);
        Assert.assertEquals(2.6925, getMockedComponent().getPercentileInnerCanthalDistance(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetIPDPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getMockedComponent().getInterpupilaryDistancePercentile(true, 0, 3.91));
        Assert.assertEquals(0, getMockedComponent().getInterpupilaryDistancePercentile(true, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getInterpupilaryDistancePercentile(true, 0, 1000));
        Assert.assertEquals(50, getMockedComponent().getInterpupilaryDistancePercentile(true, 1000, 6.13));
        Assert.assertEquals(3, getMockedComponent().getInterpupilaryDistancePercentile(true, 36, 4.23));
        Assert.assertEquals(50, getMockedComponent().getInterpupilaryDistancePercentile(true, 36, 4.835));
        Assert.assertEquals(97, getMockedComponent().getInterpupilaryDistancePercentile(true, 36, 5.49));
        Assert.assertEquals(50, getMockedComponent().getInterpupilaryDistancePercentile(true, 30, 4.7825));
    }

    @Test
    public void testGetPercentileIPD() throws ComponentLookupException
    {
        Assert.assertEquals(3.91, getMockedComponent().getPercentileInterpupilaryDistance(true, 0, 50), 1.0E-2);
        Assert.assertEquals(3.04, getMockedComponent().getPercentileInterpupilaryDistance(true, 0, 0), 1.0E-2);
        Assert.assertEquals(4.98, getMockedComponent().getPercentileInterpupilaryDistance(true, 0, 100), 1.0E-2);
        Assert.assertEquals(6.13, getMockedComponent().getPercentileInterpupilaryDistance(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(4.23, getMockedComponent().getPercentileInterpupilaryDistance(true, 36, 3), 1.0E-2);
        Assert.assertEquals(4.835, getMockedComponent().getPercentileInterpupilaryDistance(true, 36, 50), 1.0E-2);
        Assert.assertEquals(5.49, getMockedComponent().getPercentileInterpupilaryDistance(true, 36, 97), 1.0E-2);
        Assert.assertEquals(4.7825, getMockedComponent().getPercentileInterpupilaryDistance(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetOCDPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getMockedComponent().getOuterCanthalDistancePercentile(true, 0, 6.3));
        Assert.assertEquals(0, getMockedComponent().getOuterCanthalDistancePercentile(true, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getOuterCanthalDistancePercentile(true, 0, 1000));
        Assert.assertEquals(50, getMockedComponent().getOuterCanthalDistancePercentile(true, 1000, 9.08));
        Assert.assertEquals(3, getMockedComponent().getOuterCanthalDistancePercentile(true, 16, 6.27));
        Assert.assertEquals(50, getMockedComponent().getOuterCanthalDistancePercentile(true, 16, 7.305));
        Assert.assertEquals(97, getMockedComponent().getOuterCanthalDistancePercentile(true, 16, 8.33));
        Assert.assertEquals(50, getMockedComponent().getOuterCanthalDistancePercentile(true, 30, 7.4725));
    }

    @Test
    public void testGetPercentileOCD() throws ComponentLookupException
    {
        Assert.assertEquals(6.3, getMockedComponent().getPercentileOuterCanthalDistance(true, 0, 50), 1.0E-2);
        Assert.assertEquals(4.86, getMockedComponent().getPercentileOuterCanthalDistance(true, 0, 0), 1.0E-2);
        Assert.assertEquals(7.98, getMockedComponent().getPercentileOuterCanthalDistance(true, 0, 100), 1.0E-2);
        Assert.assertEquals(9.08, getMockedComponent().getPercentileOuterCanthalDistance(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(6.27, getMockedComponent().getPercentileOuterCanthalDistance(true, 16, 3), 1.0E-2);
        Assert.assertEquals(7.305, getMockedComponent().getPercentileOuterCanthalDistance(true, 16, 50), 1.0E-2);
        Assert.assertEquals(8.33, getMockedComponent().getPercentileOuterCanthalDistance(true, 16, 97), 1.0E-2);
        Assert.assertEquals(7.4725, getMockedComponent().getPercentileOuterCanthalDistance(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetEarLengthPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getMockedComponent().getEarLengthPercentile(true, 0, 4.04));
        Assert.assertEquals(0, getMockedComponent().getEarLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getEarLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, getMockedComponent().getEarLengthPercentile(true, 1000, 6.0825));
        Assert.assertEquals(5, getMockedComponent().getEarLengthPercentile(true, 36, 4.51));
        Assert.assertEquals(50, getMockedComponent().getEarLengthPercentile(true, 36, 5.115));
        Assert.assertEquals(95, getMockedComponent().getEarLengthPercentile(true, 36, 5.86));
        Assert.assertEquals(50, getMockedComponent().getEarLengthPercentile(true, 30, 5.015));
    }

    @Test
    public void testGetPercentileEarLength() throws ComponentLookupException
    {
        Assert.assertEquals(4.04, getMockedComponent().getPercentileEarLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(3, getMockedComponent().getPercentileEarLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.21, getMockedComponent().getPercentileEarLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(6.0825, getMockedComponent().getPercentileEarLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(4.51, getMockedComponent().getPercentileEarLength(true, 36, 5), 1.0E-2);
        Assert.assertEquals(5.115, getMockedComponent().getPercentileEarLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(5.86, getMockedComponent().getPercentileEarLength(true, 36, 95), 1.0E-2);
        Assert.assertEquals(5.015, getMockedComponent().getPercentileEarLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetPalpebralFissureLengthPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getMockedComponent().getPalpebralFissureLengthPercentile(true, 0, 1.9));
        Assert.assertEquals(0, getMockedComponent().getPalpebralFissureLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getPalpebralFissureLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, getMockedComponent().getPalpebralFissureLengthPercentile(true, 1000, 3.13));
        Assert.assertEquals(5, getMockedComponent().getPalpebralFissureLengthPercentile(true, 36, 2.215));
        Assert.assertEquals(50, getMockedComponent().getPalpebralFissureLengthPercentile(true, 36, 2.49));
        Assert.assertEquals(95, getMockedComponent().getPalpebralFissureLengthPercentile(true, 36, 2.78));
        Assert.assertEquals(50, getMockedComponent().getPalpebralFissureLengthPercentile(true, 30, 2.4325));
    }

    @Test
    public void testGetPercentilePalpebralFissureLength() throws ComponentLookupException
    {
        Assert.assertEquals(1.9, getMockedComponent().getPercentilePalpebralFissureLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(1.6, getMockedComponent().getPercentilePalpebralFissureLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(2.28, getMockedComponent().getPercentilePalpebralFissureLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(3.13, getMockedComponent().getPercentilePalpebralFissureLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(2.215, getMockedComponent().getPercentilePalpebralFissureLength(true, 36, 5), 1.0E-2);
        Assert.assertEquals(2.49, getMockedComponent().getPercentilePalpebralFissureLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(2.78, getMockedComponent().getPercentilePalpebralFissureLength(true, 36, 95), 1.0E-2);
        Assert.assertEquals(2.4325, getMockedComponent().getPercentilePalpebralFissureLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetHandLengthPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(-1, getMockedComponent().getHandLengthPercentile(true, 0, 6.3));
        Assert.assertEquals(-1, getMockedComponent().getHandLengthPercentile(true, 0, 0));
        Assert.assertEquals(-1, getMockedComponent().getHandLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, getMockedComponent().getHandLengthPercentile(true, 24, 10.5));
        Assert.assertEquals(0, getMockedComponent().getHandLengthPercentile(true, 24, 0));
        Assert.assertEquals(100, getMockedComponent().getHandLengthPercentile(true, 24, 1000));
        Assert.assertEquals(50, getMockedComponent().getHandLengthPercentile(true, 1000, 19.25));
        Assert.assertEquals(3, getMockedComponent().getHandLengthPercentile(true, 36, 9.95));
        Assert.assertEquals(50, getMockedComponent().getHandLengthPercentile(true, 36, 11.3));
        Assert.assertEquals(97, getMockedComponent().getHandLengthPercentile(true, 36, 12.45));
        Assert.assertEquals(50, getMockedComponent().getHandLengthPercentile(true, 30, 10.9));
    }

    @Test
    public void testGetPercentileHandLength() throws ComponentLookupException
    {
        Assert.assertEquals(-1, getMockedComponent().getPercentileHandLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(-1, getMockedComponent().getPercentileHandLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(-1, getMockedComponent().getPercentileHandLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(10.5, getMockedComponent().getPercentileHandLength(true, 24, 50), 1.0E-2);
        Assert.assertEquals(8.33, getMockedComponent().getPercentileHandLength(true, 24, 0), 1.0E-2);
        Assert.assertEquals(12.08, getMockedComponent().getPercentileHandLength(true, 24, 100), 1.0E-2);
        Assert.assertEquals(19.25, getMockedComponent().getPercentileHandLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(9.95, getMockedComponent().getPercentileHandLength(true, 36, 3), 1.0E-2);
        Assert.assertEquals(11.3, getMockedComponent().getPercentileHandLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(12.45, getMockedComponent().getPercentileHandLength(true, 36, 97), 1.0E-2);
        Assert.assertEquals(10.9, getMockedComponent().getPercentileHandLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetPalmLengthPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getMockedComponent().getPalmLengthPercentile(true, 0, 3.9));
        Assert.assertEquals(0, getMockedComponent().getPalmLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getPalmLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, getMockedComponent().getPalmLengthPercentile(true, 1000, 11.225));
        Assert.assertEquals(3, getMockedComponent().getPalmLengthPercentile(true, 36, 5.625));
        Assert.assertEquals(50, getMockedComponent().getPalmLengthPercentile(true, 36, 6.475));
        Assert.assertEquals(97, getMockedComponent().getPalmLengthPercentile(true, 36, 7.3));
        Assert.assertEquals(50, getMockedComponent().getPalmLengthPercentile(true, 30, 6.237));
    }

    @Test
    public void testGetPercentilePalmLength() throws ComponentLookupException
    {
        Assert.assertEquals(3.9, getMockedComponent().getPercentilePalmLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(2.56, getMockedComponent().getPercentilePalmLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.24, getMockedComponent().getPercentilePalmLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(11.225, getMockedComponent().getPercentilePalmLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(5.625, getMockedComponent().getPercentilePalmLength(true, 36, 3), 1.0E-2);
        Assert.assertEquals(6.475, getMockedComponent().getPercentilePalmLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(7.3, getMockedComponent().getPercentilePalmLength(true, 36, 97), 1.0E-2);
        Assert.assertEquals(6.237, getMockedComponent().getPercentilePalmLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetFootLengthPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getMockedComponent().getFootLengthPercentile(true, 0, 7.5));
        Assert.assertEquals(50, getMockedComponent().getFootLengthPercentile(false, 0, 8.5));
        Assert.assertEquals(0, getMockedComponent().getFootLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getFootLengthPercentile(true, 0, 1000));
        Assert.assertEquals(0, getMockedComponent().getFootLengthPercentile(false, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getFootLengthPercentile(false, 0, 1000));
        Assert.assertEquals(3, getMockedComponent().getFootLengthPercentile(true, 36, 13.4));
        Assert.assertEquals(50, getMockedComponent().getFootLengthPercentile(true, 36, 15.2));
        Assert.assertEquals(97, getMockedComponent().getFootLengthPercentile(true, 36, 16.8));
        Assert.assertEquals(50, getMockedComponent().getFootLengthPercentile(true, 30, 14.5125));
        Assert.assertEquals(50, getMockedComponent().getFootLengthPercentile(true, 1000, 26.45));
        Assert.assertEquals(3, getMockedComponent().getFootLengthPercentile(false, 36, 13));
        Assert.assertEquals(50, getMockedComponent().getFootLengthPercentile(false, 36, 15.075));
        Assert.assertEquals(97, getMockedComponent().getFootLengthPercentile(false, 36, 16.95));
        Assert.assertEquals(50, getMockedComponent().getFootLengthPercentile(false, 30, 14.4875));
        Assert.assertEquals(50, getMockedComponent().getFootLengthPercentile(false, 1000, 23.975));
    }

    @Test
    public void testGetPercentileFootLength() throws ComponentLookupException
    {
        Assert.assertEquals(7.5, getMockedComponent().getPercentileFootLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(8.5, getMockedComponent().getPercentileFootLength(false, 0, 50), 1.0E-2);
        Assert.assertEquals(6.75, getMockedComponent().getPercentileFootLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(8.25, getMockedComponent().getPercentileFootLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(19.54, getMockedComponent().getPercentileFootLength(false, 300, 0), 1.0E-2);
        Assert.assertEquals(27.05, getMockedComponent().getPercentileFootLength(false, 300, 100), 1.0E-2);
        Assert.assertEquals(13.4, getMockedComponent().getPercentileFootLength(true, 36, 3), 1.0E-2);
        Assert.assertEquals(15.2, getMockedComponent().getPercentileFootLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(16.8, getMockedComponent().getPercentileFootLength(true, 36, 97), 1.0E-2);
        Assert.assertEquals(14.5125, getMockedComponent().getPercentileFootLength(true, 30, 50), 1.0E-2);
        Assert.assertEquals(26.45, getMockedComponent().getPercentileFootLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(13, getMockedComponent().getPercentileFootLength(false, 36, 3), 1.0E-2);
        Assert.assertEquals(15.075, getMockedComponent().getPercentileFootLength(false, 36, 50), 1.0E-2);
        Assert.assertEquals(16.95, getMockedComponent().getPercentileFootLength(false, 36, 97), 1.0E-2);
        Assert.assertEquals(14.4875, getMockedComponent().getPercentileFootLength(false, 30, 50), 1.0E-2);
        Assert.assertEquals(23.975, getMockedComponent().getPercentileFootLength(false, 1000, 50), 1.0E-2);
    }

    @Test
    public void testGetHeightPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getMockedComponent().getHeightPercentile(true, 0, 52.7));
        Assert.assertEquals(50, getMockedComponent().getHeightPercentile(false, 0, 51.68));
        Assert.assertEquals(0, getMockedComponent().getHeightPercentile(true, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getHeightPercentile(true, 0, 1000));
        Assert.assertEquals(0, getMockedComponent().getHeightPercentile(false, 0, 0));
        Assert.assertEquals(100, getMockedComponent().getHeightPercentile(false, 0, 1000));
        Assert.assertEquals(50, getMockedComponent().getHeightPercentile(true, 1000, 176.85));
        Assert.assertEquals(72, getMockedComponent().getHeightPercentile(true, 349, 181.0));
        Assert.assertEquals(93, getMockedComponent().getHeightPercentile(false, 359, 173.0));
    }

    @Test
    public void testGetPercentileHeight() throws ComponentLookupException
    {
        Assert.assertEquals(52.7, getMockedComponent().getPercentileHeight(true, 0, 50), 1.0E-2);
        Assert.assertEquals(51.68, getMockedComponent().getPercentileHeight(false, 0, 50), 1.0E-2);
        Assert.assertEquals(45.73, getMockedComponent().getPercentileHeight(true, 0, 0), 1.0E-2);
        Assert.assertEquals(60.14, getMockedComponent().getPercentileHeight(true, 0, 100), 1.0E-2);
        Assert.assertEquals(45.61, getMockedComponent().getPercentileHeight(false, 0, 0), 1.0E-2);
        Assert.assertEquals(59.39, getMockedComponent().getPercentileHeight(false, 0, 100), 1.0E-2);
        Assert.assertEquals(176.85, getMockedComponent().getPercentileHeight(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(181.0, getMockedComponent().getPercentileHeight(true, 349, 72), 1.0E-2);
        Assert.assertEquals(172.86, getMockedComponent().getPercentileHeight(false, 359, 93), 1.0E-2);
    }

    @Test
    public void testGetFuzzyValue() throws ComponentLookupException
    {
        Assert.assertEquals("extreme-below-normal", getMockedComponent().getFuzzyValue(Integer.MIN_VALUE));
        Assert.assertEquals("extreme-below-normal", getMockedComponent().getFuzzyValue(-1));
        Assert.assertEquals("extreme-below-normal", getMockedComponent().getFuzzyValue(0));
        Assert.assertEquals("extreme-below-normal", getMockedComponent().getFuzzyValue(3));
        Assert.assertEquals("below-normal", getMockedComponent().getFuzzyValue(4));
        Assert.assertEquals("below-normal", getMockedComponent().getFuzzyValue(7));
        Assert.assertEquals("below-normal", getMockedComponent().getFuzzyValue(10));
        Assert.assertEquals("normal", getMockedComponent().getFuzzyValue(11));
        Assert.assertEquals("normal", getMockedComponent().getFuzzyValue(50));
        Assert.assertEquals("normal", getMockedComponent().getFuzzyValue(89));
        Assert.assertEquals("above-normal", getMockedComponent().getFuzzyValue(90));
        Assert.assertEquals("above-normal", getMockedComponent().getFuzzyValue(93));
        Assert.assertEquals("above-normal", getMockedComponent().getFuzzyValue(96));
        Assert.assertEquals("extreme-above-normal", getMockedComponent().getFuzzyValue(97));
        Assert.assertEquals("extreme-above-normal", getMockedComponent().getFuzzyValue(99));
        Assert.assertEquals("extreme-above-normal", getMockedComponent().getFuzzyValue(100));
        Assert.assertEquals("extreme-above-normal", getMockedComponent().getFuzzyValue(101));
        Assert.assertEquals("extreme-above-normal", getMockedComponent().getFuzzyValue(Integer.MAX_VALUE));
    }
}
