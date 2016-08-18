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
package org.phenotips.data.permissions.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;
import org.phenotips.groups.internal.DefaultGroup;
import org.phenotips.groups.internal.UsersAndGroups;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.users.User;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link Collaborator} implementation, {@link DefaultCollaborator}.
 *
 * @version $Id$
 */
public class DefaultCollaboratorTest
{
    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private UsersAndGroups usersAndGroups;

    @Mock
    private GroupManager groupManager;

    /** The user used as a collaborator. */
    private static final DocumentReference COLLABORATOR = new DocumentReference("xwiki", "XWiki", "hmccoy");

    private static final AccessLevel access = mock(AccessLevel.class);

    @Before
    public void setupUsersAndGroups() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(UsersAndGroups.class)).thenReturn(this.usersAndGroups);
        when(this.cm.getInstance(GroupManager.class)).thenReturn(this.groupManager);
    }

    /** Basic tests for {@link Collaborator#getCollaboratorType()}. */
    @Test
    public void getType() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access);
        when(this.usersAndGroups.getType(COLLABORATOR)).thenReturn("user", "group", "unknown", null);
        Assert.assertEquals("user", c.getCollaboratorType());
        Assert.assertEquals("group", c.getCollaboratorType());
        Assert.assertEquals("unknown", c.getCollaboratorType());
        Assert.assertNull(c.getCollaboratorType());
    }

    /** Basic tests for {@link Collaborator#isUser()}. */
    @Test
    public void isUser() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access);
        Mockito.when(this.usersAndGroups.getType(COLLABORATOR)).thenReturn("unknown", "user", "group", null);
        Assert.assertFalse(c.isUser());
        Assert.assertTrue(c.isUser());
        Assert.assertFalse(c.isUser());
        Assert.assertFalse(c.isUser());
    }

    /** Basic tests for {@link Collaborator#isGroup()}. */
    @Test
    public void isGroup() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access);
        Mockito.when(this.usersAndGroups.getType(COLLABORATOR)).thenReturn("unknown", "user", "group", null);
        Assert.assertFalse(c.isGroup());
        Assert.assertFalse(c.isGroup());
        Assert.assertTrue(c.isGroup());
        Assert.assertFalse(c.isGroup());
    }

    /** Basic tests for {@link Collaborator#getUser()}. */
    @Test
    public void getUser() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access);
        Assert.assertSame(COLLABORATOR, c.getUser());
    }

    /** {@link Collaborator#getUser()} returns null if no user was passed. */
    @Test
    public void getUserWithNull() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(null, null);
        Assert.assertNull(c.getUser());
    }

    /** Basic tests for {@link Collaborator#getUsername()}. */
    @Test
    public void getUsername() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access);
        Assert.assertEquals(COLLABORATOR.getName(), c.getUsername());
    }

    /** {@link Collaborator#getUsername()} returns null if no user was passed. */
    @Test
    public void getUsernameWithNull() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(null, null);
        Assert.assertNull(c.getUsername());
    }

    /** Basic tests for {@link Collaborator#getAccessLevel()}. */
    @Test
    public void getAccessLevel() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access);
        Assert.assertSame(access, c.getAccessLevel());
    }

    /** {@link Collaborator#getAccessLevel()} returns null if no access level was passed. */
    @Test
    public void getAccessLevelWithNull() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, null);
        Assert.assertNull(c.getAccessLevel());
    }

    /** Basic tests for {@link Collaborator#equals()}. */
    @Test
    public void equalsTest() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access);
        // Equals itself
        Assert.assertTrue(c.equals(c));
        // Doesn't equal null
        Assert.assertFalse(c.equals(null));
        // Equals an identical collaborator
        AccessLevel otherAccess = mock(AccessLevel.class);
        Collaborator other = new DefaultCollaborator(COLLABORATOR, access);
        Assert.assertTrue(c.equals(other));
        // Doesn't equal a collaborator with same user but different access
        other = new DefaultCollaborator(COLLABORATOR, otherAccess);
        Assert.assertFalse(c.equals(other));
        // Doesn't equal a collaborator with same access but different user
        other = new DefaultCollaborator(new DocumentReference("xwiki", "XWiki", "padams"), access);
        Assert.assertFalse(c.equals(other));
        // Doesn't equal a collaborator with different user and different access
        other = new DefaultCollaborator(new DocumentReference("xwiki", "XWiki", "padams"), otherAccess);
        Assert.assertFalse(c.equals(other));
        // Doesn't equal different types of objects
        Assert.assertFalse(c.equals("other"));
    }

    /** Basic tests for {@link Collaborator#hashCode()}. */
    @Test
    public void hashCodeTest() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access);
        AccessLevel otherAccess = mock(AccessLevel.class);
        Collaborator other = new DefaultCollaborator(COLLABORATOR, access);
        // Equals a different collaborator with the same user and access
        Assert.assertEquals(c.hashCode(), other.hashCode());
        Assert.assertFalse(c.equals(null));
        // Different hashcodes for different coordinates
        other = new DefaultCollaborator(COLLABORATOR, otherAccess);
        Assert.assertNotEquals(c.hashCode(), other.hashCode());
        other = new DefaultCollaborator(new DocumentReference("xwiki", "XWiki", "padams"), access);
        Assert.assertNotEquals(c.hashCode(), other.hashCode());
        other = new DefaultCollaborator(new DocumentReference("xwiki", "XWiki", "padams"), otherAccess);
        Assert.assertNotEquals(c.hashCode(), other.hashCode());
    }

    /** {@link Collaborator#toString()} is customized. */
    @Test
    public void toStringTest() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access);
        Mockito.when(access.toString()).thenReturn("edit");
        Assert.assertEquals("[xwiki:XWiki.hmccoy, edit]", c.toString());
    }

    @Test
    public void isUserIncludedTest()
    {
        User user = mock(User.class);

        DocumentReference userRef = new DocumentReference("xwiki", "XWiki", "user");
        Collaborator userC = new DefaultCollaborator(userRef, access);
        when(this.usersAndGroups.getType(userRef)).thenReturn(UsersAndGroups.USER);
        when(user.getProfileDocument()).thenReturn(userRef);
        Assert.assertTrue(userC.isUserIncluded(user));

        EntityReference entityReference = new EntityReference("name", EntityType.ATTACHMENT);
        Collaborator badCollaborator = new DefaultCollaborator(entityReference, access);
        when(this.usersAndGroups.getType(entityReference)).thenReturn(UsersAndGroups.GROUP);
        Assert.assertFalse(badCollaborator.isUserIncluded(user));

        Set<Group> groups = new HashSet<Group>();

        DocumentReference documentReference = new DocumentReference("xwiki", "Groups", "group");
        Collaborator groupC = new DefaultCollaborator(documentReference, access);
        when(this.usersAndGroups.getType(documentReference)).thenReturn(UsersAndGroups.GROUP);
        when(this.groupManager.getGroupsForUser(user)).thenReturn(groups);
        Assert.assertFalse(groupC.isUserIncluded(user));

        groups.add(new DefaultGroup(documentReference));
        Assert.assertTrue(groupC.isUserIncluded(user));
    }

    @Test
    public void getAllUserNamesTest1()
    {
        User user = mock(User.class);
        DocumentReference userRef = new DocumentReference("xwiki", "XWiki", "user");

        Collaborator userC = new DefaultCollaborator(userRef, access);
        when(this.usersAndGroups.getType(userRef)).thenReturn(UsersAndGroups.USER);
        when(user.getProfileDocument()).thenReturn(userRef);

        Collection<String> allUserNames = userC.getAllUserNames();
        Assert.assertEquals(1, allUserNames.size());
    }
}
