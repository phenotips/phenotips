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
        Date date = new Date(2017, 12, 11);
        Date date2 = new Date(2017);
        Date empty = null;
        PhenoTipsDate phenoDate = new PhenoTipsDate(date);
        PhenoTipsDate phenoDate2 = new PhenoTipsDate(date2);
        PhenoTipsDate emptyDate = new PhenoTipsDate(empty);
        Assert.assertTrue(phenoDate.isSet());
        Assert.assertFalse(emptyDate.isSet());
        // Fix these assertion statements
        Assert.assertNotSame(new Date(2017, 12, 11), phenoDate.toEarliestPossibleISODate());
        Assert.assertNotSame("2017-12-11", phenoDate.toYYYYMMDDString());
        Assert.assertNotSame("2017", phenoDate2.toYYYYMMDDString());

    }

    @Test
    public void nullDateTest() throws ComponentLookupException {
        Date date = null;
        PhenoTipsDate phenoDate = new PhenoTipsDate(date);
        Assert.assertFalse(phenoDate.isSet());
        Assert.assertSame(null, phenoDate.toYYYYMMDDString()); // Fix this(Expects undefinded date)
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
        phenoDateDepr.toJSON(); // Debug assertions
        Assert.assertSame("{\"year\":\"2010s\",\"month\":\"01\",\"day\":\"13\"}", phenoDateDepr.toString());
    }

    @Test
    public void emptyJSONDateTest() throws ComponentLookupException {
        JSONObject fuzzyDate = new JSONObject(); // Figure out JSON input
        PhenoTipsDate phenoDate = new PhenoTipsDate(fuzzyDate);
        // Assert
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

        Assert.assertNotNull(phenoDate); // Fix assertions; placeholder
        Assert.assertNotNull(phenoDate2);
        Assert.assertNotNull(phenoDate3);
        Assert.assertNotNull(phenoDate4);
    }

    @Test
    public void nullStringDateTest() throws ComponentLookupException {
        String date = null;
        PhenoTipsDate phenoDate = new PhenoTipsDate(date);
        Assert.assertNotNull(phenoDate.toString());
    }
}
