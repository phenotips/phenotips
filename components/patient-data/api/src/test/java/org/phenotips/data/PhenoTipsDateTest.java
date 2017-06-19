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
    }

    @Test
    public void deprecatedDecadeJSONIsNormalizedAsRangeJSON()
    {
        Assert.assertTrue(new JSONObject("{\"year\":2010,\"range\":{\"years\":10},\"month\":1,\"day\":13}").similar(
            new PhenoTipsDate(new JSONObject("{\"decade\":\"2010s\",\"month\":\"01\",\"day\":13}")).toJSON()));
    }

    @Test
    public void invalidJSONElementsAreIgnored()
    {
        Assert.assertTrue(new JSONObject("{\"month\":1,\"day\":13}").similar(
            new PhenoTipsDate(new JSONObject("{\"year\":\"2010s\",\"month\":\"01\",\"day\":\"13\"}"))
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
        String date1 = "1995-06-23";
        String date2 = "1995-06";
        String date3 = "1995";
        String date4 = "1990s";
        PhenoTipsDate phenoDate = new PhenoTipsDate(date1);
        PhenoTipsDate phenoDate2 = new PhenoTipsDate(date2);
        PhenoTipsDate phenoDate3 = new PhenoTipsDate(date3);
        PhenoTipsDate phenoDate4 = new PhenoTipsDate(date4);

        Assert.assertEquals("1995-06-23", phenoDate.toYYYYMMDDString());
        Assert.assertEquals("1995-06", phenoDate2.toYYYYMMDDString());
        Assert.assertEquals("1995", phenoDate3.toYYYYMMDDString());
        Assert.assertEquals("1990s", phenoDate4.toYYYYMMDDString());
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
}
