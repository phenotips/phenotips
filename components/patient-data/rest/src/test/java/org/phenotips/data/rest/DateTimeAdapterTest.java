package org.phenotips.data.rest;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for the {@link DateTimeAdapter} class
 */
public class DateTimeAdapterTest {

    private DateTimeAdapter adapter;

    private DateTime dateTime;

    private String ValidDateString;


    @Before
    public void setUp() throws Exception
    {
        this.adapter = new DateTimeAdapter();
        this.dateTime = new DateTime(2015, 1, 1, 12, 0, DateTimeZone.UTC);
        this.ValidDateString = "2015-01-01T12:00:00.000Z";
    }

    //-------------------------Marshal Tests-------------------------

    @Test
    public void marshalReturnsCorrectString() throws Exception
    {
        Assert.assertEquals(this.ValidDateString, this.adapter.marshal(this.dateTime));
    }

    @Ignore
    @Test(expected = Exception.class)
    public void marshalThrowsExceptionWhenPassedNull() throws Exception
    {
        this.adapter.marshal(null);
    }

    //-------------------------UnMarshal Tests-------------------------

    @Test
    public void unMarshalReturnsCorrectDateTime() throws Exception
    {
        Assert.assertEquals(this.dateTime, this.adapter.unmarshal(this.ValidDateString));
    }

    @Test(expected = Exception.class)
    public void unMarshalThrowsExceptionWhenPassedInvalidString() throws Exception
    {
        this.adapter.unmarshal("2015-02-29T12:00:00.000Z");
    }

    @Test(expected = Exception.class)
    public void unMarshalThrowsExceptionWhenPassedEmptyString() throws Exception
    {
        this.adapter.unmarshal("");
    }

    @Test(expected = Exception.class)
    public void unMarshalThrowsExceptionWhenPassedNull() throws Exception
    {
        this.adapter.unmarshal(null);
    }

    //-------------------------Reflexivity Test-------------------------

    @Test
    public void testReflexivity() throws Exception
    {
        Assert.assertEquals(this.ValidDateString, this.adapter.marshal(this.adapter.unmarshal(this.ValidDateString)));
        Assert.assertEquals(this.dateTime, this.adapter.unmarshal(this.adapter.marshal(this.dateTime)));
    }
}
