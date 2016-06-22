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
package org.phenotips.groups.script;

import org.phenotips.groups.internal.UsersAndGroups;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.query.QueryException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Rule;
import org.junit.Test;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @version $Id$
 */
public class UsersAndGroupsScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<UsersAndGroupsScriptService> mocker =
        new MockitoComponentMockingRule<UsersAndGroupsScriptService>(UsersAndGroupsScriptService.class);

    @Test
    public void UsersAndGroupsScriptServiceTest1() throws ComponentLookupException, QueryException
    {
        JSONArray array = new JSONArray();
        JSONObject result = new JSONObject();
        result.put("id", "Xwiki.Admin");
        result.put("value", "Admin;user");
        array.add(result);
        JSONObject resultJson = new JSONObject();
        resultJson.put("matched", array);

        UsersAndGroups suag = this.mocker.getInstance(UsersAndGroups.class);
        org.mockito.Mockito.when(suag.search("a", true, false)).thenReturn(resultJson);

        org.junit.Assert.assertEquals(resultJson,
            this.mocker.getComponentUnderTest().searchUsersAndGroups("a", true, false));
    }
}
