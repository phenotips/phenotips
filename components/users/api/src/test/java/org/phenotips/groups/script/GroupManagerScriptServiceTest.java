/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.groups.script;

import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link GroupManager} script service, {@link GroupManagerScriptService}.
 *
 * @version $Id$
 */
public class GroupManagerScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<GroupManagerScriptService> mocker =
        new MockitoComponentMockingRule<GroupManagerScriptService>(GroupManagerScriptService.class);

    /** Basic tests for {@link GroupManagerScriptService#getGroupsForUser(org.xwiki.model.reference.DocumentReference)}. */
    @Test
    public void getGroupsForUser() throws ComponentLookupException
    {
        Set<Group> groups = new LinkedHashSet<Group>();
        User user = mock(User.class);
        groups.add(mock(Group.class));
        GroupManager manager = this.mocker.getInstance(GroupManager.class);
        when(manager.getGroupsForUser(user)).thenReturn(groups);
        Assert.assertSame(groups, this.mocker.getComponentUnderTest().getGroupsForUser(user));
    }

    /** {@link GroupManagerScriptService#getEnabledFieldNames()} catches exception. */
    @Test
    public void getGroupsForUserWithException() throws ComponentLookupException
    {
        User user = mock(User.class);
        GroupManager manager = this.mocker.getInstance(GroupManager.class);
        when(manager.getGroupsForUser(user)).thenThrow(new NullPointerException());
        Assert.assertTrue(this.mocker.getComponentUnderTest().getGroupsForUser(user).isEmpty());
    }
}
