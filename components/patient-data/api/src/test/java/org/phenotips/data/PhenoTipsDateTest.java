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

import java.util.Date;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class PhenoTipsDateTest
{
    @Test
    public void dateConstructorWorks()
    {
        @SuppressWarnings("deprecation")
        Date date = new Date(100, 11, 21);
        PhenoTipsDate phenoDate = new PhenoTipsDate(date);

        Assert.assertTrue(phenoDate.isSet());
        Assert.assertEquals(date, phenoDate.toEarliestPossibleISODate());
        Assert.assertEquals("2000-12-21", phenoDate.toYYYYMMDDString());
        Assert.assertTrue(new JSONObject("{\"year\":2000,\"month\":12,\"day\":21}").similar(phenoDate.toJSON()));
    }

    @Test
    public void dateFromNullDateIsEmpty()
    {
        Date date = null;
        PhenoTipsDate phenoDate = new PhenoTipsDate(date);

        Assert.assertFalse(phenoDate.isSet());
        Assert.assertSame("", phenoDate.toYYYYMMDDString());
        Assert.assertEquals(null, phenoDate.toEarliestPossibleISODate());
        Assert.assertTrue(new JSONObject().similar(phenoDate.toJSON()));
        Assert.assertEquals("{}", phenoDate.toString());
    }

    @Test
    public void roundtripFromJSON()
    {
        Assert.assertTrue(new JSONObject("{\"year\":2013}").similar(
            new PhenoTipsDate(new JSONObject("{\"year\":2013}")).toJSON()));
        Assert.assertTrue(new JSONObject("{\"year\":2013}").similar(
            new PhenoTipsDate(new JSONObject("{\"year\":\"2013\"}")).toJSON()));
        Assert.assertTrue(new JSONObject("{\"year\":2010,\"month\":1,\"day\":13}").similar(
            new PhenoTipsDate(new JSONObject("{\"year\":\"2010\",\"month\":\"01\",\"day\":\"13\"}")).toJSON()));
        Assert.assertTrue(new JSONObject("{\"year\":2010,\"month\":1,\"day\":13}").similar(
            new PhenoTipsDate(new JSONObject("{\"year\":2010,\"month\":01,\"day\":13}")).toJSON()));
        Assert.assertTrue(new JSONObject("{\"year\":2010,\"range\":{\"years\":10},\"month\":1,\"day\":13}").similar(
            new PhenoTipsDate(new JSONObject("{\"year\":2010,\"range\":{\"years\":10},\"month\":01,\"day\":13}"))
                .toJSON()));
    }

    @Test
    public void deprecatedDecadeJSONIsNormalizedAsRangeJSON()
    {
        Assert.assertTrue(new JSONObject("{\"year\":2010,\"range\":{\"years\":10},\"month\":1,\"day\":13}").similar(
            new PhenoTipsDate(new JSONObject("{\"decade\":\"2010s\",\"month\":\"01\",\"day\":13}")).toJSON()));
    }

    @Test
    public void invalidDecadeIsIgnored()
    {
        Assert.assertTrue(new JSONObject("{\"month\":1,\"day\":13}").similar(
            new PhenoTipsDate(new JSONObject("{\"decade\":\"1990\",\"month\":\"01\",\"day\":13}")).toJSON()));
    }

    @Test
    public void invalidRangeIsIgnored()
    {
        Assert.assertTrue(new JSONObject("{\"year\":1990,\"month\":1,\"day\":13}").similar(
            new PhenoTipsDate(
                new JSONObject("{\"year\":\"1990\",\"range\":{\"years\":\"five\"},\"month\":\"01\",\"day\":13}"))
                    .toJSON()));
    }

    @Test
    public void whenYearIsPresentDecadeIsIgnored()
    {
        Assert.assertTrue(new JSONObject("{\"year\":1995,\"month\":1,\"day\":13}").similar(
            new PhenoTipsDate(new JSONObject("{\"decade\":\"1990s\",\"year\":1995,\"month\":\"01\",\"day\":13}"))
                .toJSON()));
    }

    @Test
    public void invalidJSONElementsAreIgnored()
    {
        Assert.assertTrue(new JSONObject("{\"month\":1,\"day\":13}").similar(
            new PhenoTipsDate(new JSONObject("{\"year\":\"2010s\",\"month\":\"01\",\"day\":\"13\"}"))
                .toJSON()));
        Assert.assertTrue(new JSONObject("{\"year\":2010}").similar(
            new PhenoTipsDate(new JSONObject("{\"year\":\"2010\",\"month\":\"55\",\"day\":\"-2\"}"))
                .toJSON()));
        Assert.assertTrue(new JSONObject("{\"year\":2010}").similar(
            new PhenoTipsDate(new JSONObject("{\"year\":\"2010\",\"month\":-2,\"day\":55}"))
                .toJSON()));
    }

    @Test
    public void dateFromEmptyJSONIsEmpty()
    {
        JSONObject emptyDate = new JSONObject();
        PhenoTipsDate emptyJSON = new PhenoTipsDate(emptyDate);
        Assert.assertEquals("{}", emptyJSON.toString());
    }

    @Test
    public void dateFromNullJSONIsEmpty()
    {
        JSONObject nullDate = null;
        PhenoTipsDate emptyJSON = new PhenoTipsDate(nullDate);
        Assert.assertEquals("{}", emptyJSON.toString());
    }

    @Test
    public void roundtripFromString()
    {
        Assert.assertEquals("1995-01-01", new PhenoTipsDate("1995-01-01").toYYYYMMDDString());
        Assert.assertEquals("1995-12-31", new PhenoTipsDate("1995-12-31").toYYYYMMDDString());
        Assert.assertEquals("1995-06", new PhenoTipsDate("1995-06").toYYYYMMDDString());
        Assert.assertEquals("1995-11", new PhenoTipsDate("1995-11").toYYYYMMDDString());
        Assert.assertEquals("1995", new PhenoTipsDate("1995").toYYYYMMDDString());
        Assert.assertEquals("1990s", new PhenoTipsDate("1990s").toYYYYMMDDString());
    }

    @Test
    public void invalidStringElementsAreIgnored()
    {
        Assert.assertEquals("1995", new PhenoTipsDate("1995-27-72").toYYYYMMDDString());
    }

    @Test
    public void dateFromEmptyStringIsEmpty()
    {
        String date = "";
        PhenoTipsDate phenoDate = new PhenoTipsDate(date);
        Assert.assertEquals("{}", phenoDate.toString());
    }

    @Test
    public void dateFromNullStringIsEmpty()
    {
        String date = null;
        PhenoTipsDate phenoDate = new PhenoTipsDate(date);
        Assert.assertEquals("{}", phenoDate.toString());
    }

    @Test
    public void isSetReturnsTrueWhenAnyElementIsSet()
    {
        Assert.assertFalse(new PhenoTipsDate(new JSONObject()).isSet());
        Assert.assertTrue(new PhenoTipsDate(new JSONObject("{\"decade\":\"2010s\"}")).isSet());
        Assert.assertTrue(new PhenoTipsDate(new JSONObject("{\"year\":\"2010\"}")).isSet());
        Assert.assertTrue(new PhenoTipsDate(new JSONObject("{\"month\":2}")).isSet());
        Assert.assertTrue(new PhenoTipsDate(new JSONObject("{\"day\":2}")).isSet());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void earliestPossibleDateIsCorrect()
    {
        Assert.assertEquals(new Date(95, 2, 3), new PhenoTipsDate("1995-03-03").toEarliestPossibleISODate());
        Assert.assertEquals(new Date(95, 2, 1), new PhenoTipsDate("1995-03").toEarliestPossibleISODate());
        Assert.assertEquals(new Date(95, 0, 1), new PhenoTipsDate("1995").toEarliestPossibleISODate());
        Assert.assertEquals(new Date(90, 0, 1), new PhenoTipsDate("1990s").toEarliestPossibleISODate());
        Assert.assertNull(new PhenoTipsDate("").toEarliestPossibleISODate());
    }
}
