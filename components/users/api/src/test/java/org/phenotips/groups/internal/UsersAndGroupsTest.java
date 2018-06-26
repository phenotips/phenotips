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

import org.phenotips.Constants;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.lang.reflect.ParameterizedType;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Provider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class UsersAndGroupsTest
{
    private static final EntityReference USER_CLASS = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final EntityReference GROUP_CLASS = new EntityReference("PhenoTipsGroupClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    private static String groupsQueryString;

    private static String usersQueryString;

    @Rule
    public final MockitoComponentMockingRule<UsersAndGroups> mocker =
        new MockitoComponentMockingRule<UsersAndGroups>(UsersAndGroups.class);

    @Mock
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private ComponentManager cm;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    private Provider<XWikiContext> contextProvider;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        ParameterizedType cpType = new DefaultParameterizedType(null, Provider.class, XWikiContext.class);
        this.contextProvider = this.mocker.getInstance(cpType);

        Mockito.doReturn(this.context).when(this.contextProvider).get();
        Mockito.doReturn(this.xwiki).when(this.context).getWiki();
    }

    static {
        StringBuilder groupsQuerySb = new StringBuilder();
        groupsQuerySb.append("from doc.object(PhenoTips.PhenoTipsGroupClass) as groups");
        groupsQuerySb.append(" where concat(concat(lower(doc.name), ' '), lower(doc.title)) like :input");
        groupsQuerySb.append(" and doc.fullName <> 'PhenoTips.PhenoTipsGroupTemplate'");
        groupsQuerySb.append(" order by doc.name");
        UsersAndGroupsTest.groupsQueryString = groupsQuerySb.toString();

        StringBuilder usersQuerySb = new StringBuilder();
        usersQuerySb.append("from doc.object(XWiki.XWikiUsers) as user");
        usersQuerySb.append(" where lower(doc.name) like :input");
        usersQuerySb.append(" or concat(concat(lower(user.first_name), ' '), lower(user.last_name)) like :input");
        usersQuerySb.append(" order by user.first_name, user.last_name");
        UsersAndGroupsTest.usersQueryString = usersQuerySb.toString();
    }

    @Test
    public void searchTest1() throws ComponentLookupException, QueryException
    {
        String input = "a";

        String userName = "Admin";
        String userFullName = "XWiki:XWiki.Admin";

        JSONArray resultsArray = new JSONArray();
        List<String> usersList = new LinkedList<String>();
        usersList.add(userFullName);

        JSONObject resultItem = new JSONObject();
        resultItem.put("id", userFullName);
        resultItem.put("value", userName);
        resultItem.put("description", userFullName);
        resultItem.put("info", userName);
        resultsArray.put(resultItem);

        Query q = mock(Query.class);
        when(q.bindValue("input", "%%" + input + "%%")).thenReturn(q);
        when(q.<String>execute()).thenReturn(usersList);

        QueryManager qm = this.mocker.getInstance(QueryManager.class);
        when(qm.createQuery(usersQueryString, Query.XWQL)).thenReturn(q);

        User u = mock(User.class);
        when(u.getName()).thenReturn(userName);
        when(u.getUsername()).thenReturn(userFullName);

        UserManager um = this.mocker.getInstance(UserManager.class);
        when(um.getUser(userFullName)).thenReturn(u);

        JSONObject expectedResult = new JSONObject();
        expectedResult.put("matched", resultsArray);

        String searchResult = this.mocker.getComponentUnderTest().search(input, 10, true, false, true);
        Assert.assertEquals(expectedResult.toString(), searchResult);
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
        resultItem.put("id", groupName);
        resultItem.put("value", groupFullName);
        resultItem.put("description", groupFullName);
        resultItem.put("info", groupName);
        resultsArray.put(resultItem);

        JSONObject expectedResult = new JSONObject();
        expectedResult.put("matched", resultsArray);

        String searchResult = this.mocker.getComponentUnderTest().search(input, 10, false, true, true);
        Assert.assertEquals(expectedResult.toString(), searchResult);
    }

    @Test
    public void searchTest3() throws QueryException, ComponentLookupException
    {
        String input = "a";

        String userName = "Admin";
        String userFullName = "XWiki:XWiki.Admin";

        JSONArray resultsArray = new JSONArray();
        List<String> usersList = new LinkedList<String>();
        usersList.add(userFullName);

        JSONObject resultItem = new JSONObject();
        resultItem.put("id", userFullName);
        resultItem.put("value", userName);
        resultItem.put("description", userFullName);
        resultItem.put("info", userName);
        resultsArray.put(resultItem);

        Query q = mock(Query.class);
        when(q.bindValue("input", "%%" + input + "%%")).thenReturn(q);
        when(q.<String>execute()).thenReturn(usersList);

        QueryManager qm = this.mocker.getInstance(QueryManager.class);
        when(qm.createQuery(usersQueryString, Query.XWQL)).thenReturn(q);

        User u = mock(User.class);
        when(u.getName()).thenReturn(userName);
        when(u.getUsername()).thenReturn(userFullName);

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

        JSONObject resultGroupItem = new JSONObject();
        resultGroupItem.put("id", groupName);
        resultGroupItem.put("value", groupFullName);
        resultGroupItem.put("description", groupFullName);
        resultGroupItem.put("info", groupName);
        resultsArray.put(resultGroupItem);

        JSONObject expectedResult = new JSONObject();
        expectedResult.put("matched", resultsArray);

        String searchResult = this.mocker.getComponentUnderTest().search(input, 10, true, true, true);
        Assert.assertEquals(expectedResult.toString(), searchResult);
    }

    @Test
    public void searchTest4() throws QueryException, ComponentLookupException
    {
        String input = "b";

        JSONArray resultsArray = new JSONArray();
        JSONObject expectedResult = new JSONObject();
        expectedResult.put("matched", resultsArray);

        String searchResult = this.mocker.getComponentUnderTest().search(input, 10, false, false, true);
        Assert.assertEquals(expectedResult.toString(), searchResult);
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

        Assert.assertEquals(expectedResult.toString(), this.mocker.getComponentUnderTest()
            .search(input, 10, true, true, true)
            .toString());
        Assert.assertEquals(expectedResult.toString(),
            this.mocker.getComponentUnderTest().search(input, 10, true, false, true));
        Assert.assertEquals(expectedResult.toString(),
            this.mocker.getComponentUnderTest().search(input, 10, false, true, true));
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

        Assert.assertEquals(expectedResult.toString(),
            this.mocker.getComponentUnderTest().search(input, 10, true, false, true));
        Assert.assertEquals(expectedResult.toString(),
            this.mocker.getComponentUnderTest().search(input, 10, false, false, true));
    }

    @Test
    public void getTypeTest() throws Exception
    {
        DocumentReference groupDocument = new DocumentReference("xwiki", "Groups", "g1");
        XWikiDocument groupXDocument = mock(XWikiDocument.class);
        org.mockito.Mockito.when(groupXDocument.getXObject(GROUP_CLASS)).thenReturn(new BaseObject());
        DocumentAccessBridge bridge = this.mocker.getInstance(DocumentAccessBridge.class);
        when(bridge.getDocument(groupDocument)).thenReturn(groupXDocument);
        UsersAndGroups usersAndGroups = this.mocker.getComponentUnderTest();
        org.junit.Assert.assertEquals(true, usersAndGroups.isGroup(groupDocument));

        DocumentReference userDocument = new DocumentReference("xwiki", "XWiki", "u1");
        XWikiDocument userXDocument = mock(XWikiDocument.class);
        org.mockito.Mockito.when(userXDocument.getXObject(USER_CLASS)).thenReturn(new BaseObject());
        when(bridge.getDocument(userDocument)).thenReturn(userXDocument);
        org.junit.Assert.assertEquals(true, usersAndGroups.isUser(userDocument));

        DocumentReference unknownDocument = new DocumentReference("xwiki", "qwerty", "qwerty");
        XWikiDocument unknownXDocument = mock(XWikiDocument.class);
        when(bridge.getDocument(unknownDocument)).thenReturn(unknownXDocument);
        org.junit.Assert.assertEquals(false, usersAndGroups.isUser(unknownDocument));
        org.junit.Assert.assertEquals(false, usersAndGroups.isGroup(unknownDocument));

        DocumentReference mock = mock(DocumentReference.class);
        when(bridge.getDocument(mock)).thenThrow(new Exception("Failed"));
        org.junit.Assert.assertEquals(false, usersAndGroups.isUser(mock));
        org.junit.Assert.assertEquals(false, usersAndGroups.isGroup(mock));
    }

    @Test
    public void suggestUserAsXML() throws Exception
    {
        String input = "a";

        String userName = "Admin";
        String userFullName = "XWiki:XWiki.Admin";

        JSONArray resultsArray = new JSONArray();
        List<String> usersList = new LinkedList<String>();
        usersList.add(userFullName);

        JSONObject resultItem = new JSONObject();
        resultItem.put("id", userFullName);
        resultItem.put("value", userName);
        resultItem.put("description", userFullName);
        resultItem.put("info", userName);
        resultsArray.put(resultItem);

        Query q = mock(Query.class);
        when(q.bindValue("input", "%%" + input + "%%")).thenReturn(q);
        when(q.<String>execute()).thenReturn(usersList);

        QueryManager qm = this.mocker.getInstance(QueryManager.class);
        when(qm.createQuery(usersQueryString, Query.XWQL)).thenReturn(q);

        User u = mock(User.class);
        when(u.getName()).thenReturn(userName);
        when(u.getUsername()).thenReturn(userFullName);

        UserManager um = this.mocker.getInstance(UserManager.class);
        when(um.getUser(userFullName)).thenReturn(u);

        String searchResult = this.mocker.getComponentUnderTest().search(input, 10, true, false, false);

        Document response = parseXML(searchResult);
        NodeList suggestions = response.getElementsByTagName("rs");

        Assert.assertEquals(1, suggestions.getLength());
        Assert.assertEquals("XWiki:XWiki.Admin", ((Element) suggestions.item(0)).getAttribute("id"));
        Assert.assertEquals("Admin", ((Element) suggestions.item(0)).getAttribute("info"));
        Assert.assertEquals("Admin", ((Element) suggestions.item(0)).getAttribute("value"));
        Assert.assertEquals("XWiki:XWiki.Admin", ((Element) suggestions.item(0)).getTextContent());
    }

    private Document parseXML(String input) throws Exception
    {
        DOMImplementationLS implementation =
            (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
        LSInput in = implementation.createLSInput();
        in.setStringData(input);
        LSParser parser = implementation.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
        Document doc = parser.parse(in);
        return doc;
    }
}
