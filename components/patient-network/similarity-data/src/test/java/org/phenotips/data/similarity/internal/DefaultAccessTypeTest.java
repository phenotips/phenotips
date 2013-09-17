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
package org.phenotips.data.similarity.internal;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.OwnerAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.permissions.internal.MatchAccessLevel;

import org.xwiki.component.manager.ComponentLookupException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link AccessType} enumeration.
 * 
 * @version $Id$
 */
public class DefaultAccessTypeTest
{
    private final AccessLevel view = new ViewAccessLevel();

    private final AccessLevel match = new MatchAccessLevel();

    @Test
    public void testPrivateAccess() throws ComponentLookupException
    {
        AccessLevel access = new NoAccessLevel();
        AccessType a = new DefaultAccessType(access, this.view, this.match);
        Assert.assertFalse(a.isOpenAccess());
        Assert.assertFalse(a.isLimitedAccess());
        Assert.assertTrue(a.isPrivateAccess());
        Assert.assertSame(access, a.getAccessLevel());
        Assert.assertEquals("none", a.toString());
    }

    @Test
    public void testMatchAccess() throws ComponentLookupException
    {
        AccessLevel access = new MatchAccessLevel();
        AccessType a = new DefaultAccessType(access, this.view, this.match);
        Assert.assertFalse(a.isOpenAccess());
        Assert.assertTrue(a.isLimitedAccess());
        Assert.assertFalse(a.isPrivateAccess());
        Assert.assertSame(access, a.getAccessLevel());
        Assert.assertEquals("match", a.toString());
    }

    @Test
    public void testViewAccess() throws ComponentLookupException
    {
        AccessLevel access = new ViewAccessLevel();
        AccessType a = new DefaultAccessType(access, this.view, this.match);
        Assert.assertTrue(a.isOpenAccess());
        Assert.assertFalse(a.isLimitedAccess());
        Assert.assertFalse(a.isPrivateAccess());
        Assert.assertSame(access, a.getAccessLevel());
        Assert.assertEquals("view", a.toString());
    }

    @Test
    public void testOwnerAccess() throws ComponentLookupException
    {
        AccessLevel access = new OwnerAccessLevel();
        AccessType a = new DefaultAccessType(access, this.view, this.match);
        Assert.assertTrue(a.isOpenAccess());
        Assert.assertFalse(a.isLimitedAccess());
        Assert.assertFalse(a.isPrivateAccess());
        Assert.assertSame(access, a.getAccessLevel());
        Assert.assertEquals("owner", a.toString());
    }
}
