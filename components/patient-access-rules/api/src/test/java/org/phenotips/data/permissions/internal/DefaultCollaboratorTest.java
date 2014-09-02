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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

/**
 * Tests for the default {@link Collaborator} implementation, {@link DefaultCollaborator}.
 *
 * @version $Id$
 */
public class DefaultCollaboratorTest
{
    /** The user used as a collaborator. */
    private static final DocumentReference COLLABORATOR = new DocumentReference("xwiki", "XWiki", "hmccoy");

    private static final AccessLevel access = mock(AccessLevel.class);

    private static final PatientAccessHelper helper = mock(PatientAccessHelper.class);

    /** Basic tests for {@link Collaborator#getType()}. */
    @Test
    public void getType() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access, helper);
        Mockito.when(helper.getType(COLLABORATOR)).thenReturn("user", "group", "unknown", null);
        Assert.assertEquals("user", c.getType());
        Assert.assertEquals("group", c.getType());
        Assert.assertEquals("unknown", c.getType());
        Assert.assertNull(c.getType());
    }

    /** Basic tests for {@link Collaborator#isUser()}. */
    @Test
    public void isUser() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access, helper);
        Mockito.when(helper.getType(COLLABORATOR)).thenReturn("unknown", "user", "group", null);
        Assert.assertFalse(c.isUser());
        Assert.assertTrue(c.isUser());
        Assert.assertFalse(c.isUser());
        Assert.assertFalse(c.isUser());
    }

    /** Basic tests for {@link Collaborator#isGroup()}. */
    @Test
    public void isGroup() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access, helper);
        Mockito.when(helper.getType(COLLABORATOR)).thenReturn("unknown", "user", "group", null);
        Assert.assertFalse(c.isGroup());
        Assert.assertFalse(c.isGroup());
        Assert.assertTrue(c.isGroup());
        Assert.assertFalse(c.isGroup());
    }

    /** Basic tests for {@link Collaborator#getUser()}. */
    @Test
    public void getUser() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access, helper);
        Assert.assertSame(COLLABORATOR, c.getUser());
    }

    /** {@link Collaborator#getUser()} returns null if no user was passed. */
    @Test
    public void getUserWithNull() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(null, null, helper);
        Assert.assertNull(c.getUser());
    }

    /** Basic tests for {@link Collaborator#getUsername()}. */
    @Test
    public void getUsername() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access, helper);
        Assert.assertEquals(COLLABORATOR.getName(), c.getUsername());
    }

    /** {@link Collaborator#getUsername()} returns null if no user was passed. */
    @Test
    public void getUsernameWithNull() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(null, null, helper);
        Assert.assertNull(c.getUsername());
    }

    /** Basic tests for {@link Collaborator#getAccessLevel()}. */
    @Test
    public void getAccessLevel() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access, helper);
        Assert.assertSame(access, c.getAccessLevel());
    }

    /** {@link Collaborator#getAccessLevel()} returns null if no access level was passed. */
    @Test
    public void getAccessLevelWithNull() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, null, helper);
        Assert.assertNull(c.getAccessLevel());
    }

    /** Basic tests for {@link Collaborator#equals()}. */
    @Test
    public void equalsTest() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access, helper);
        // Equals itself
        Assert.assertTrue(c.equals(c));
        // Doesn't equal null
        Assert.assertFalse(c.equals(null));
        // Equals an identical collaborator
        AccessLevel otherAccess = mock(AccessLevel.class);
        Collaborator other = new DefaultCollaborator(COLLABORATOR, access, helper);
        Assert.assertTrue(c.equals(other));
        // Doesn't equal a collaborator with same user but different access
        other = new DefaultCollaborator(COLLABORATOR, otherAccess, helper);
        Assert.assertFalse(c.equals(other));
        // Doesn't equal a collaborator with same access but different user
        other = new DefaultCollaborator(new DocumentReference("xwiki", "XWiki", "padams"), access, helper);
        Assert.assertFalse(c.equals(other));
        // Doesn't equal a collaborator with different user and different access
        other = new DefaultCollaborator(new DocumentReference("xwiki", "XWiki", "padams"), otherAccess, helper);
        Assert.assertFalse(c.equals(other));
        // Doesn't equal different types of objects
        Assert.assertFalse(c.equals("other"));
    }

    /** Basic tests for {@link Collaborator#hashCode()}. */
    @Test
    public void hashCodeTest() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access, helper);
        AccessLevel otherAccess = mock(AccessLevel.class);
        Collaborator other = new DefaultCollaborator(COLLABORATOR, access, helper);
        // Equals a different collaborator with the same user and access
        Assert.assertEquals(c.hashCode(), other.hashCode());
        Assert.assertFalse(c.equals(null));
        // Different hashcodes for different coordinates
        other = new DefaultCollaborator(COLLABORATOR, otherAccess, helper);
        Assert.assertNotEquals(c.hashCode(), other.hashCode());
        other = new DefaultCollaborator(new DocumentReference("xwiki", "XWiki", "padams"), access, helper);
        Assert.assertNotEquals(c.hashCode(), other.hashCode());
        other = new DefaultCollaborator(new DocumentReference("xwiki", "XWiki", "padams"), otherAccess, helper);
        Assert.assertNotEquals(c.hashCode(), other.hashCode());
    }

    /** {@link Collaborator#toString()} is customized. */
    @Test
    public void toStringTest() throws ComponentLookupException
    {
        Collaborator c = new DefaultCollaborator(COLLABORATOR, access, helper);
        Mockito.when(access.toString()).thenReturn("edit");
        Assert.assertEquals("[xwiki:XWiki.hmccoy, edit]", c.toString());
    }

}
