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
package org.phenotips.rest;

import org.phenotips.rest.DateTimeAdapter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.IllegalFieldValueException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the {@link DateTimeAdapter} class.
 */
public class DateTimeAdapterTest
{

    private DateTimeAdapter adapter;

    private DateTime dateTime;

    private String validDateString;

    @Before
    public void setUp() throws Exception
    {
        this.adapter = new DateTimeAdapter();
        this.dateTime = new DateTime(2015, 1, 1, 12, 0, DateTimeZone.UTC);
        this.validDateString = "2015-01-01T12:00:00.000Z";
    }

    // -------------------------Marshal Tests-------------------------

    @Test
    public void marshalReturnsCorrectString()
    {
        Assert.assertEquals(this.validDateString, this.adapter.marshal(this.dateTime));
    }

    @Test
    public void marshalReturnsNullWhenPassedNull()
    {
        Assert.assertNull(this.adapter.marshal(null));
    }

    // -------------------------UnMarshal Tests-------------------------

    @Test
    public void unMarshalReturnsCorrectDateTime()
    {
        Assert.assertEquals(this.dateTime, this.adapter.unmarshal(this.validDateString));
    }

    @Test(expected = IllegalFieldValueException.class)
    public void unMarshalThrowsExceptionWhenPassedInvalidString()
    {
        this.adapter.unmarshal("2015-02-29T12:00:00.000Z");
    }

    @Test
    public void unMarshalReturnsNullWhenPassedEmptyString()
    {
        Assert.assertNull(this.adapter.unmarshal(""));
    }

    @Test
    public void unMarshalReturnsNullWhenPassedNull()
    {
        Assert.assertNull(this.adapter.unmarshal(null));
    }

    // -------------------------Reflexivity Test-------------------------

    @Test
    public void testReflexivity()
    {
        Assert.assertEquals(this.validDateString, this.adapter.marshal(this.adapter.unmarshal(this.validDateString)));
        Assert.assertEquals(this.dateTime, this.adapter.unmarshal(this.adapter.marshal(this.dateTime)));
    }
}
