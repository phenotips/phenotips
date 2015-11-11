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
package org.phenotips.groups.internal;

import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class SearchUsersAndGroupsTest
{
    @Rule
    public final MockitoComponentMockingRule<UsersAndGroups> mocker =
        new MockitoComponentMockingRule<UsersAndGroups>(UsersAndGroups.class);

    private static String groupsQueryString;

    private static String usersQueryString;

    static {
        StringBuilder groupsQuerySb = new StringBuilder();
        groupsQuerySb.append("from doc.object(PhenoTips.PhenoTipsGroupClass) as groups ");
        groupsQuerySb.append(" where lower(doc.name)  like :input");
        groupsQuerySb.append(" and doc.fullName <> 'PhenoTips.PhenoTipsGroupTemplate' ");
        groupsQuerySb.append(" order by doc.name");
        SearchUsersAndGroupsTest.groupsQueryString = groupsQuerySb.toString();

        StringBuilder usersQuerySb = new StringBuilder();
        usersQuerySb.append("from doc.object(XWiki.XWikiUsers) as user ");
        usersQuerySb.append(" where lower(doc.name) like :input");
        usersQuerySb.append(" or concat(concat(lower(user.first_name), ' '), lower(user.last_name)) like :input");
        usersQuerySb.append(" order by user.first_name, user.last_name");
        SearchUsersAndGroupsTest.usersQueryString = usersQuerySb.toString();
    }

    @Test
    public void searchTest1() throws ComponentLookupException, QueryException
    {
        String input = "a";

        String userName = "Admin";
        String userFullName = "XWiki.Admin";

        JSONArray resultsArray = new JSONArray();
        List<String> usersList = new LinkedList<String>();
        usersList.add(userFullName);

        JSONObject resultItem = new JSONObject();
        resultItem.put("id", userFullName + ";user");
        resultItem.put("value", userName);
        resultsArray.add(resultItem);

        Query q = mock(Query.class);
        when(q.bindValue("input", "%%" + input + "%%")).thenReturn(q);
        when(q.<String>execute()).thenReturn(usersList);

        QueryManager qm = this.mocker.getInstance(QueryManager.class);
        when(qm.createQuery(usersQueryString, Query.XWQL)).thenReturn(q);

        User u = mock(User.class);
        when(u.getUsername()).thenReturn(userName);

        UserManager um = this.mocker.getInstance(UserManager.class);
        when(um.getUser(userFullName)).thenReturn(u);

        JSONObject expectedResult = new JSONObject();
        expectedResult.put("matched", resultsArray);

        JSON searchResult = this.mocker.getComponentUnderTest().search(input, true, false);
        Assert.assertEquals(expectedResult, searchResult);
    }

    @Test
    public void searchTest2() throws ComponentLookupException, QueryException
    {
        String input = "g";

        String groupName = "g1";
        String groupFullName = "Groups.g1";

        JSONArray resultsArray = new JSONArray();
        List<String> groupsList = new LinkedList<String>();

        groupsList.add(groupFullName);

        Query q = mock(Query.class);
        when(q.bindValue("input", "%%" + input + "%%")).thenReturn(q);
        when(q.<String>execute()).thenReturn(groupsList);

        QueryManager qm = this.mocker.getInstance(QueryManager.class);
        when(qm.createQuery(groupsQueryString, Query.XWQL)).thenReturn(q);

        DocumentReference groupDocument = new DocumentReference("xwiki", "Groups", groupName);
        Group g = mock(Group.class);
        when(g.getReference()).thenReturn(groupDocument);

        GroupManager gm = this.mocker.getInstance(GroupManager.class);
        when(gm.getGroup(groupFullName)).thenReturn(g);

        JSONObject resultItem = new JSONObject();
        resultItem.put("id", groupFullName + ";group");
        resultItem.put("value", groupName);
        resultsArray.add(resultItem);

        JSONObject expectedResult = new JSONObject();
        expectedResult.put("matched", resultsArray);

        JSON searchResult = this.mocker.getComponentUnderTest().search(input, false, true);
        Assert.assertEquals(expectedResult, searchResult);
    }

    @Test
    public void searchTest3() throws QueryException, ComponentLookupException
    {
        String input = "a";

        String userName = "Admin";
        String userFullName = "XWiki.Admin";

        JSONArray resultsArray = new JSONArray();
        List<String> usersList = new LinkedList<String>();
        usersList.add(userFullName);
        JSONObject resultItem = new JSONObject();
        resultItem.put("id", userFullName + ";user");
        resultItem.put("value", userName);
        resultsArray.add(resultItem);

        Query q = mock(Query.class);
        when(q.bindValue("input", "%%" + input + "%%")).thenReturn(q);
        when(q.<String>execute()).thenReturn(usersList);

        QueryManager qm = this.mocker.getInstance(QueryManager.class);
        when(qm.createQuery(usersQueryString, Query.XWQL)).thenReturn(q);

        User u = mock(User.class);
        when(u.getUsername()).thenReturn(userName);

        UserManager um = this.mocker.getInstance(UserManager.class);
        when(um.getUser(userFullName)).thenReturn(u);

        String groupName = "A1";
        String groupFullName = "Groups.A1";

        List<String> groupsList = new LinkedList<String>();
        groupsList.add(groupFullName);

        Query q2 = mock(Query.class);
        when(q2.bindValue("input", "%%" + input + "%%")).thenReturn(q2);
        when(q2.<String>execute()).thenReturn(groupsList);

        when(qm.createQuery(groupsQueryString, Query.XWQL)).thenReturn(q2);

        DocumentReference groupDocument = new DocumentReference("xwiki", "Groups", groupName);
        Group g = mock(Group.class);
        when(g.getReference()).thenReturn(groupDocument);

        GroupManager gm = this.mocker.getInstance(GroupManager.class);
        when(gm.getGroup(groupFullName)).thenReturn(g);

        resultItem.put("id", groupFullName + ";group");
        resultItem.put("value", groupName);
        resultsArray.add(resultItem);

        JSONObject expectedResult = new JSONObject();
        expectedResult.put("matched", resultsArray);

        JSON searchResult = this.mocker.getComponentUnderTest().search(input, true, true);
        Assert.assertEquals(expectedResult, searchResult);
    }

    @Test
    public void searchTest4() throws QueryException, ComponentLookupException
    {
        String input = "b";

        JSONArray resultsArray = new JSONArray();
        JSONObject expectedResult = new JSONObject();
        expectedResult.put("matched", resultsArray);

        JSON searchResult = this.mocker.getComponentUnderTest().search(input, false, false);
        Assert.assertEquals(expectedResult, searchResult);
    }

    @Test
    public void searchTest5() throws QueryException, ComponentLookupException
    {
        String input = "a";

        JSONArray resultsArray = new JSONArray();
        List<String> usersList = new LinkedList<String>();

        Query q = mock(Query.class);
        QueryManager qm = this.mocker.getInstance(QueryManager.class);
        when(q.bindValue("input", "%%" + input + "%%")).thenReturn(q);
        when(q.<String>execute()).thenReturn(usersList);
        when(qm.createQuery(usersQueryString, Query.XWQL)).thenReturn(q);

        List<String> groupsList = new LinkedList<String>();

        Query q2 = mock(Query.class);
        when(q2.bindValue("input", "%%" + input + "%%")).thenReturn(q2);
        when(q2.<String>execute()).thenReturn(groupsList);
        when(qm.createQuery(groupsQueryString, Query.XWQL)).thenReturn(q2);

        JSONObject expectedResult = new JSONObject();
        expectedResult.put("matched", resultsArray);

        Assert.assertEquals(expectedResult, this.mocker.getComponentUnderTest().search(input, true, true));
        Assert.assertEquals(expectedResult, this.mocker.getComponentUnderTest().search(input, true, false));
        Assert.assertEquals(expectedResult, this.mocker.getComponentUnderTest().search(input, false, true));
    }

    @Test
    public void searchTest6() throws QueryException, ComponentLookupException
    {
        String input = "a";

        JSONArray resultsArray = new JSONArray();

        Query q = mock(Query.class);
        QueryManager qm = this.mocker.getInstance(QueryManager.class);
        when(q.bindValue("input", "%%" + input + "%%")).thenReturn(q);
        when(q.<String>execute()).thenThrow(new QueryException("failed", q, null));
        when(qm.createQuery(usersQueryString, Query.XWQL)).thenReturn(q);

        JSONObject expectedResult = new JSONObject();
        expectedResult.put("matched", resultsArray);

        Assert.assertEquals(expectedResult, this.mocker.getComponentUnderTest().search(input, true, false));
        Assert.assertEquals(expectedResult, this.mocker.getComponentUnderTest().search(input, false, false));
    }

}
