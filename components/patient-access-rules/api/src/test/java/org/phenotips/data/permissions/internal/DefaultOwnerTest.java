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

import org.phenotips.data.permissions.Owner;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

/**
 * Tests for the default {@link Owner} implementation, {@link DefaultOwner}.
 *
 * @version $Id$
 */
public class DefaultOwnerTest
{
    /** The user used as an owner for all tests. */
    private static final DocumentReference OWNER = new DocumentReference("xwiki", "XWiki", "hmccoy");

    private static final PatientAccessHelper helper = mock(PatientAccessHelper.class);

    /** Basic tests for {@link Owner#getType()}. */
    @Test
    public void getType() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(OWNER, helper);
        Mockito.when(helper.getType(OWNER)).thenReturn("user", "group", "unknown", null);
        Assert.assertEquals("user", o.getType());
        Assert.assertEquals("group", o.getType());
        Assert.assertEquals("unknown", o.getType());
        Assert.assertNull(o.getType());
    }

    /** Basic tests for {@link Owner#isUser()}. */
    @Test
    public void isUser() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(OWNER, helper);
        Mockito.when(helper.getType(OWNER)).thenReturn("unknown", "user", "group", null);
        Assert.assertFalse(o.isUser());
        Assert.assertTrue(o.isUser());
        Assert.assertFalse(o.isUser());
        Assert.assertFalse(o.isUser());
    }

    /** Basic tests for {@link Owner#isGroup()}. */
    @Test
    public void isGroup() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(OWNER, helper);
        Mockito.when(helper.getType(OWNER)).thenReturn("unknown", "user", "group", null);
        Assert.assertFalse(o.isGroup());
        Assert.assertFalse(o.isGroup());
        Assert.assertTrue(o.isGroup());
        Assert.assertFalse(o.isGroup());
    }

    /** Basic tests for {@link Owner#getUser()}. */
    @Test
    public void getUser() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(OWNER, helper);
        Assert.assertSame(OWNER, o.getUser());
    }

    /** {@link Owner#getUser()} returns null if no user was passed. */
    @Test
    public void getUserWithNull() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(null, helper);
        Assert.assertNull(o.getUser());
    }

    /** Basic tests for {@link Owner#getUsername()}. */
    @Test
    public void getUsername() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(OWNER, helper);
        Assert.assertEquals(OWNER.getName(), o.getUsername());
    }

    /** {@link Owner#getUsername()} returns null if no user was passed. */
    @Test
    public void getUsernameWithNull() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(null, helper);
        Assert.assertNull(o.getUsername());
    }

    /** Basic tests for {@link Owner#equals()}. */
    @Test
    public void equalsTest() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(OWNER, helper);
        // Equals itself
        Assert.assertTrue(o.equals(o));
        // Doesn't equal null
        Assert.assertFalse(o.equals(null));
        // Equals an identical owner
        Owner other = new DefaultOwner(OWNER, helper);
        Assert.assertTrue(o.equals(other));
        // Doesn't equal an owner with different user
        other = new DefaultOwner(new DocumentReference("xwiki", "XWiki", "padams"), helper);
        Assert.assertFalse(o.equals(other));
        // Doesn't equal different types of objects
        Assert.assertFalse(o.equals("other"));
    }

    /** Basic tests for {@link Owner#hashCode()}. */
    @Test
    public void hashCodeTest() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(OWNER, helper);
        Owner other = new DefaultOwner(OWNER, helper);
        // Same hashcode for a different owner with the same user
        Assert.assertEquals(o.hashCode(), other.hashCode());
        // Different hashcodes for different users
        other = new DefaultOwner(new DocumentReference("xwiki", "XWiki", "padams"), helper);
        Assert.assertNotEquals(o.hashCode(), other.hashCode());
        // Different hashcodes for user and guest
        other = new DefaultOwner(null, helper);
        Assert.assertNotEquals(o.hashCode(), other.hashCode());
        // Same hashcode for two guests
        o = new DefaultOwner(null, helper);
        Assert.assertEquals(o.hashCode(), other.hashCode());
    }

    /** {@link Owner#toString()} is customized. */
    @Test
    public void toStringTest() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(OWNER, helper);
        Assert.assertEquals("[xwiki:XWiki.hmccoy]", o.toString());
    }

    /** {@link Owner#toString()} uses "nobody" as the user when no user is set. */
    @Test
    public void toStringUsesNobodyForGuests() throws ComponentLookupException
    {
        Owner o = new DefaultOwner(null, helper);
        Assert.assertEquals("[nobody]", o.toString());
    }
}
