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
package org.phenotips.data;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link MedicationEffect} enum.
 *
 * @version $Id$
 * @since 1.2M5
 */
public class MedicationEffectTest
{
    @Test
    public void toStringUsesCamelCase()
    {
        Assert.assertEquals("unknown", MedicationEffect.UNKNOWN.toString());
        Assert.assertEquals("worsening", MedicationEffect.WORSENING.toString());
        Assert.assertEquals("none", MedicationEffect.NONE.toString());
        Assert.assertEquals("slightImprovement", MedicationEffect.SLIGHT_IMPROVEMENT.toString());
        Assert.assertEquals("intermediateImprovement", MedicationEffect.INTERMEDIATE_IMPROVEMENT.toString());
        Assert.assertEquals("strongImprovement", MedicationEffect.STRONG_IMPROVEMENT.toString());
    }

    @Test
    public void fromStringAcceptsCamelCase()
    {
        Assert.assertEquals(MedicationEffect.UNKNOWN, MedicationEffect.fromString("unknown"));
        Assert.assertEquals(MedicationEffect.WORSENING, MedicationEffect.fromString("worsening"));
        Assert.assertEquals(MedicationEffect.NONE, MedicationEffect.fromString("none"));
        Assert.assertEquals(MedicationEffect.SLIGHT_IMPROVEMENT, MedicationEffect.fromString("slightImprovement"));
        Assert.assertEquals(MedicationEffect.INTERMEDIATE_IMPROVEMENT,
            MedicationEffect.fromString("intermediateImprovement"));
        Assert.assertEquals(MedicationEffect.STRONG_IMPROVEMENT, MedicationEffect.fromString("strongImprovement"));
    }

    @Test
    public void fromStringAcceptsUpperCase()
    {
        Assert.assertEquals(MedicationEffect.UNKNOWN, MedicationEffect.fromString("UNKNOWN"));
        Assert.assertEquals(MedicationEffect.WORSENING, MedicationEffect.fromString("WORSENING"));
        Assert.assertEquals(MedicationEffect.NONE, MedicationEffect.fromString("NONE"));
        Assert.assertEquals(MedicationEffect.SLIGHT_IMPROVEMENT, MedicationEffect.fromString("SLIGHT_IMPROVEMENT"));
        Assert.assertEquals(MedicationEffect.INTERMEDIATE_IMPROVEMENT,
            MedicationEffect.fromString("INTERMEDIATE_IMPROVEMENT"));
        Assert.assertEquals(MedicationEffect.STRONG_IMPROVEMENT, MedicationEffect.fromString("STRONG_IMPROVEMENT"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromStringRejectsUnknownValues()
    {
        MedicationEffect.fromString("invalid");
    }

    @Test
    public void fromStringReturnsNullForBlankValue()
    {
        Assert.assertNull(MedicationEffect.fromString(null));
        Assert.assertNull(MedicationEffect.fromString(""));
        Assert.assertNull(MedicationEffect.fromString(" "));
    }
}
