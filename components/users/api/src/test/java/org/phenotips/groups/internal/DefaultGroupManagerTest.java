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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link GroupManager} implementation, {@link DefaultGroupManager}.
 *
 * @version $Id$
 */
public class DefaultGroupManagerTest
{
    private static final EntityReference GROUP_SPACE = new EntityReference("Groups", EntityType.SPACE);

    @Rule
    public final MockitoComponentMockingRule<GroupManager> mocker =
        new MockitoComponentMockingRule<GroupManager>(DefaultGroupManager.class);

    /** Basic tests for {@link DefaultGroupManager#getGroupsForUser(org.xwiki.model.reference.DocumentReference)}. */
    @Test
    public void getGroupsForUser() throws ComponentLookupException, QueryException
    {
        User u = mock(User.class);
        DocumentReference userProfile = new DocumentReference("xwiki", "XWiki", "Admin");
        when(u.getProfileDocument()).thenReturn(userProfile);
        EntityReferenceSerializer<String> serializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        when(serializer.serialize(userProfile)).thenReturn("XWiki.Admin");

        QueryManager qm = this.mocker.getInstance(QueryManager.class);
        Query q = mock(Query.class);
        when(q.bindValue("u", "xwiki:XWiki.Admin")).thenReturn(q);
        when(q.bindValue("su", "XWiki.Admin")).thenReturn(q);
        when(qm.createQuery("from doc.object(XWiki.XWikiGroups) grp where grp.member in (:u, :su)", Query.XWQL))
            .thenReturn(q);
        List<Object> groupNames = new LinkedList<>();
        groupNames.add("Groups.Group A");
        groupNames.add("Group B Administrators");
        when(q.<Object>execute()).thenReturn(groupNames);

        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        DocumentReference a = new DocumentReference("xwiki", "Groups", "Group A");
        when(resolver.resolve(eq("Groups.Group A"), eq(GROUP_SPACE))).thenReturn(a);
        DocumentReference ba = new DocumentReference("xwiki", "Groups", "Group B Administrators");
        when(resolver.resolve(eq("Group B Administrators"), eq(GROUP_SPACE))).thenReturn(ba);

        q = mock(Query.class);
        when(q.bindValue(1, "xwiki:Groups.Group A")).thenReturn(q);
        when(q.bindValue(2, "Groups.Group A")).thenReturn(q);
        when(q.bindValue(3, "xwiki:Groups.Group B Administrators")).thenReturn(q);
        when(q.bindValue(4, "Groups.Group B Administrators")).thenReturn(q);
        when(qm.createQuery("from doc.object(XWiki.XWikiGroups) grp where grp.member in (?1,?2,?3,?4)", Query.XWQL))
            .thenReturn(q);
        groupNames = new LinkedList<>();
        groupNames.add("Groups.Group B");
        when(q.<Object>execute()).thenReturn(groupNames);

        q = mock(Query.class);
        DocumentReference b = new DocumentReference("xwiki", "Groups", "Group B");
        when(resolver.resolve(eq("Groups.Group B"), eq(GROUP_SPACE))).thenReturn(b);
        when(q.bindValue(1, "xwiki:Groups.Group B")).thenReturn(q);
        when(q.bindValue(2, "Groups.Group B")).thenReturn(q);
        when(qm.createQuery("from doc.object(XWiki.XWikiGroups) grp where grp.member in (?1,?2)", Query.XWQL))
            .thenReturn(q);
        when(q.<Object>execute()).thenReturn(Collections.emptyList());

        q = mock(Query.class);
        when(qm.createQuery("from doc.object(XWiki.XWikiGroups) grp, doc.object(PhenoTips.PhenoTipsGroupClass) phgrp",
            Query.XWQL)).thenReturn(q);
        groupNames = new LinkedList<>();
        groupNames.add("Groups.Group A");
        groupNames.add("Groups.Group B");
        when(q.<Object>execute()).thenReturn(groupNames);

        Set<Group> result = this.mocker.getComponentUnderTest().getGroupsForUser(u);
        Assert.assertEquals(2, result.size());
        Iterator<Group> resultGroups = result.iterator();
        Assert.assertEquals(a, resultGroups.next().getReference());
        Assert.assertEquals(b, resultGroups.next().getReference());
    }

    /** {@link DefaultGroupManager#getGroupsForUser(User)} ignores invalid profiles. */
    @Test
    public void getGroupsForUserWithWrongProfile() throws ComponentLookupException, QueryException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().getGroupsForUser(null).isEmpty());
        User u = mock(User.class);
        Assert.assertTrue(this.mocker.getComponentUnderTest().getGroupsForUser(u).isEmpty());
    }

    /** {@link DefaultGroupManager#getGroupsForUser(User)} catches exception. */
    @Test
    public void getGroupsForUserWithException() throws ComponentLookupException, QueryException
    {
        User u = mock(User.class);
        DocumentReference userProfile = new DocumentReference("xwiki", "XWiki", "Admin");
        when(u.getProfileDocument()).thenReturn(userProfile);

        QueryManager qm = this.mocker.getInstance(QueryManager.class);
        Query q = mock(Query.class);
        when(q.bindValue(any(String.class), any(String.class))).thenReturn(q);
        when(qm.createQuery(any(String.class), any(String.class))).thenReturn(q);
        when(q.<String>execute()).thenThrow(new QueryException("Failed", q, null));

        Assert.assertTrue(this.mocker.getComponentUnderTest().getGroupsForUser(u).isEmpty());
    }

    /** Basic tests for {@link DefaultGroupManager#getGroup(DocumentReference)}. */
    @Test
    public void getGroupWithReference() throws ComponentLookupException
    {
        DocumentReference a = new DocumentReference("xwiki", "Groups", "Group A");
        Assert.assertEquals(a, this.mocker.getComponentUnderTest().getGroup(a).getReference());
    }

    /** {@link DefaultGroupManager#getGroup(DocumentReference)} returns null for null reference. */
    @Test
    public void getGroupWithNullReference() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().getGroup((DocumentReference) null));
    }

    /** Basic tests for {@link DefaultGroupManager#getGroup(DocumentReference)}. */
    @Test
    public void getGroupWithName() throws ComponentLookupException
    {
        DocumentReference a = new DocumentReference("xwiki", "Groups", "Group A");
        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(resolver.resolve(eq("Group A"), eq(GROUP_SPACE))).thenReturn(a);

        Assert.assertEquals(a, this.mocker.getComponentUnderTest().getGroup("Group A").getReference());
    }

    /** Basic tests for {@link DefaultGroupManager#getGroup(DocumentReference)}. */
    @Test
    public void getGroupWithMissingName() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().getGroup((String) null));
        Assert.assertNull(this.mocker.getComponentUnderTest().getGroup(""));
    }
}
