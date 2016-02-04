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

import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

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
        String username = "username";
        User user = mock(User.class);
        UserManager userManager = this.mocker.getInstance(UserManager.class);
        when(userManager.getUser(username)).thenReturn(user);
        groups.add(mock(Group.class));
        GroupManager manager = this.mocker.getInstance(GroupManager.class);
        when(manager.getGroupsForUser(user)).thenReturn(groups);
        Assert.assertSame(groups, this.mocker.getComponentUnderTest().getGroupsForUser(username));
    }

    /** {@link GroupManagerScriptService#getEnabledFieldNames()} catches exception. */
    @Test
    public void getGroupsForUserWithException() throws ComponentLookupException
    {
        String username = "username";
        User user = mock(User.class);
        UserManager userManager = this.mocker.getInstance(UserManager.class);
        when(userManager.getUser(username)).thenReturn(user);
        GroupManager manager = this.mocker.getInstance(GroupManager.class);
        when(manager.getGroupsForUser(user)).thenThrow(new NullPointerException());
        Assert.assertTrue(this.mocker.getComponentUnderTest().getGroupsForUser(username).isEmpty());
    }

    @Test
    public void getGroupsForUserWhenUserNotFound() throws ComponentLookupException
    {
        String username = "username";
        UserManager userManager = this.mocker.getInstance(UserManager.class);
        when(userManager.getUser(username)).thenReturn(null);
        Assert.assertTrue(this.mocker.getComponentUnderTest().getGroupsForUser(username).isEmpty());
    }
}
