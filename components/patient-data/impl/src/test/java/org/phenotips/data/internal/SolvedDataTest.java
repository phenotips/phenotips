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
package org.phenotips.data.internal;

import java.util.Arrays;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.xpn.xwiki.XWikiException;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class SolvedDataTest
{
    @Test
    public void testInvalidInputsBehavior() throws XWikiException
    {
        SolvedData data = new SolvedData("z", "200100", null);

        Assert.assertEquals(null, data.getStatus());
        Assert.assertEquals("200100", data.getNotes());
        Assert.assertEquals(Collections.emptyList(), data.getPubmedIds());
    }

    @Test
    public void testConstructorFromJson() throws XWikiException
    {
        JSONObject object = new JSONObject();
        object.put(SolvedData.STATUS_JSON_KEY, SolvedData.STATUS_PROPERTY_NAME);
        object.put(SolvedData.NOTES_JSON_KEY, "200100");
        object.put(SolvedData.PUBMED_ID_JSON_KEY, Arrays.asList("PM:1001", "PM:1002"));

        SolvedData data = new SolvedData(object);

        Assert.assertEquals(true, data.isSolved());
        Assert.assertEquals("200100", data.getNotes());
        Assert.assertEquals(Arrays.asList("PM:1001", "PM:1002"), data.getPubmedIds());

        object.put(SolvedData.STATUS_JSON_KEY, SolvedData.STATUS_UNSOLVED);
        data = new SolvedData(object);
        Assert.assertEquals(false, data.isSolved());
        Assert.assertEquals("0", data.getStatus());

        object.put(SolvedData.STATUS_JSON_KEY, "z");
        data = new SolvedData(object);
        Assert.assertEquals(false, data.isSolved());
        Assert.assertEquals(null, data.getStatus());
    }

    @Test
    public void testToJsonBehavior() throws XWikiException
    {
        SolvedData data = new SolvedData("1", "200100", Arrays.asList("PM:1001", "PM:1002"));
        JSONObject json = data.toJSON();

        Assert.assertEquals(3, json.length());
        Assert.assertEquals(SolvedData.STATUS_PROPERTY_NAME, json.getString(SolvedData.STATUS_JSON_KEY));
        Assert.assertEquals("200100", json.getString(SolvedData.NOTES_JSON_KEY));
        Assert.assertEquals("PM:1001", ((JSONArray) json.get(SolvedData.PUBMED_ID_JSON_KEY)).get(0));
        Assert.assertEquals("PM:1002", ((JSONArray) json.get(SolvedData.PUBMED_ID_JSON_KEY)).get(1));

        data = new SolvedData("0", null, null);
        json = data.toJSON();

        Assert.assertEquals(1, json.length());
        Assert.assertEquals(SolvedData.STATUS_UNSOLVED, json.getString(SolvedData.STATUS_JSON_KEY));
    }

}
