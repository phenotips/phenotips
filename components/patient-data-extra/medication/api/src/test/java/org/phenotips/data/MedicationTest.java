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

import org.joda.time.MutablePeriod;
import org.joda.time.Period;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link Medication} class.
 *
 * @version $Id$
 * @since 1.2M5
 */
public class MedicationTest
{
    @Test
    public void basicConstructorSetsAllFields()
    {
        MutablePeriod p = new MutablePeriod();
        p.setMonths(4);
        p.setYears(2);

        Medication m = new Medication("n", "gn", "d", "f", p.toPeriod(), "none", "note");
        Assert.assertEquals("n", m.getName());
        Assert.assertEquals("gn", m.getGenericName());
        Assert.assertEquals("d", m.getDose());
        Assert.assertEquals("f", m.getFrequency());
        Assert.assertEquals(p, m.getDuration());
        Assert.assertEquals(MedicationEffect.NONE, m.getEffect());
        Assert.assertEquals("note", m.getNotes());
    }

    @Test
    public void basicConstructorAcceptsNullFields()
    {
        Medication m = new Medication(null, null, null, null, null, null, null);
        Assert.assertNull(m.getName());
        Assert.assertNull(m.getGenericName());
        Assert.assertNull(m.getDose());
        Assert.assertNull(m.getFrequency());
        Assert.assertNull(m.getDuration());
        Assert.assertNull(m.getEffect());
        Assert.assertNull(m.getNotes());
    }

    @Test()
    public void basicConstructorWithUnknownEffectIgnoresEffect()
    {
        Medication m = new Medication(null, null, null, null, null, "invalid", null);
        Assert.assertNull(m.getEffect());
    }

    @Test
    public void jsonSerializationAndDeserializationWorks()
    {
        MutablePeriod p = new MutablePeriod();
        p.setMonths(4);
        p.setYears(2);

        Medication m = new Medication("n", "gn", "d", "f", p.toPeriod(), "none", "note");
        m = new Medication(m.toJSON());
        Assert.assertEquals("n", m.getName());
        Assert.assertEquals("gn", m.getGenericName());
        Assert.assertEquals("d", m.getDose());
        Assert.assertEquals("f", m.getFrequency());
        Assert.assertEquals(p, m.getDuration());
        Assert.assertEquals(MedicationEffect.NONE, m.getEffect());
        Assert.assertEquals("note", m.getNotes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void jsonConstructorDoesntAcceptNull()
    {
        new Medication(null);
    }

    @Test()
    public void jsonConstructorAcceptsEmptyJson()
    {
        Medication m = new Medication(new JSONObject());
        Assert.assertNull(m.getName());
        Assert.assertNull(m.getGenericName());
        Assert.assertNull(m.getDose());
        Assert.assertNull(m.getFrequency());
        Assert.assertNull(m.getDuration());
        Assert.assertNull(m.getEffect());
        Assert.assertNull(m.getNotes());

        Assert.assertEquals(0, m.toJSON().length());
    }

    @Test()
    public void jsonConstructorWithUnknownEffectIgnoresEffect()
    {
        JSONObject json = new JSONObject();
        json.put("effect", "invalid");
        Medication m = new Medication(json);
        Assert.assertNull(m.getEffect());
    }

    @Test()
    public void emptyPeriodIsIgnored()
    {
        Period p = new Period();
        Medication m = new Medication(null, null, null, null, p, null, null);
        Assert.assertEquals(Period.ZERO, m.getDuration());
        Assert.assertFalse(m.toJSON().has("period"));

        p = new Period(0);
        m = new Medication(null, null, null, null, p, null, null);
        Assert.assertEquals(Period.ZERO, m.getDuration());
        Assert.assertFalse(m.toJSON().has("period"));
    }
}
