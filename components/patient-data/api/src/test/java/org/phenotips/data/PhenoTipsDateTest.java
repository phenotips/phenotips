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

import org.phenotips.data.PhenoTipsDate;
import org.xwiki.component.manager.ComponentLookupException;

public class PhenoTipsDateTest {

    @Test
    public void normalDateTest() throws ComponentLookupException {
        @SuppressWarnings("deprecation")
        Date date = new Date(100, 11, 21);
        PhenoTipsDate phenoDate = new PhenoTipsDate(date);
        
        Assert.assertTrue(phenoDate.isSet());
        Assert.assertEquals(date, phenoDate.toEarliestPossibleISODate());
        Assert.assertEquals("2000-12-21", phenoDate.toYYYYMMDDString());
    }

    @Test
    public void nullDateTest() throws ComponentLookupException {
        Date date = null;
        PhenoTipsDate phenoDate = new PhenoTipsDate(date);
        
        Assert.assertFalse(phenoDate.isSet());
        Assert.assertSame("", phenoDate.toYYYYMMDDString());
        Assert.assertEquals(null, phenoDate.toEarliestPossibleISODate());
    }

    @Test
    public void normalJSONDateTest() throws ComponentLookupException {
        JSONObject date1 = new JSONObject("{\"year\":\"2013\"}");
        JSONObject date2 = new JSONObject("{\"year\":\"2010s\",\"month\":\"01\",\"day\":\"13\"}");
        JSONObject depricatedDate = new JSONObject("{\"decade\":\"201s\",\"month\":\"01\",\"day\":\"13\"}");       
        PhenoTipsDate phenoDate = new PhenoTipsDate(date1);
        PhenoTipsDate phenoDate2 = new PhenoTipsDate(date2);
        PhenoTipsDate phenoDateDepr = new PhenoTipsDate(depricatedDate);
        phenoDate.toJSON();
        phenoDate2.toJSON();
        phenoDateDepr.toJSON(); 
        
        Assert.assertEquals("{\"month\":1,\"day\":13}", phenoDate2.toString());
        Assert.assertEquals("{\"month\":1,\"year\":201,\"range\":{\"years\":10},\"day\":13}", phenoDateDepr.toString());
    }

    @Test
    public void emptyJSONDateTest() throws ComponentLookupException {
        JSONObject nullDate = null;
        PhenoTipsDate emptyJSON = new PhenoTipsDate(nullDate);
        emptyJSON.toJSON();
        Assert.assertEquals("{}", emptyJSON.toString());
    }

    @Test
    public void normalStringDateTest() throws ComponentLookupException {
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
    public void nullStringDateTest() throws ComponentLookupException {
        String date = null;
        PhenoTipsDate phenoDate = new PhenoTipsDate(date);       
        Assert.assertEquals("{}",phenoDate.toString());
    }
}
