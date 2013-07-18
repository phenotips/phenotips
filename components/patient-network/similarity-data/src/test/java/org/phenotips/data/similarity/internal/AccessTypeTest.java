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

import org.phenotips.data.similarity.AccessType;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link AccessType} enumeration.
 * 
 * @version $Id$
 */
public class AccessTypeTest
{
    /** Test the correct settings for the different access types. */
    @Test
    public void testAccessTypes()
    {
        Assert.assertTrue(AccessType.OWNED.isOpenAccess());
        Assert.assertFalse(AccessType.OWNED.isLimitedAccess());
        Assert.assertFalse(AccessType.OWNED.isPrivateAccess());
        Assert.assertEquals("owned", AccessType.OWNED.toString());

        Assert.assertTrue(AccessType.GROUP_OWNED.isOpenAccess());
        Assert.assertFalse(AccessType.GROUP_OWNED.isLimitedAccess());
        Assert.assertFalse(AccessType.GROUP_OWNED.isPrivateAccess());

        Assert.assertTrue(AccessType.PUBLIC.isOpenAccess());
        Assert.assertFalse(AccessType.PUBLIC.isLimitedAccess());
        Assert.assertFalse(AccessType.PUBLIC.isPrivateAccess());

        Assert.assertTrue(AccessType.SHARED.isOpenAccess());
        Assert.assertFalse(AccessType.SHARED.isLimitedAccess());
        Assert.assertFalse(AccessType.SHARED.isPrivateAccess());

        Assert.assertFalse(AccessType.MATCH.isOpenAccess());
        Assert.assertTrue(AccessType.MATCH.isLimitedAccess());
        Assert.assertFalse(AccessType.MATCH.isPrivateAccess());

        Assert.assertFalse(AccessType.PRIVATE.isOpenAccess());
        Assert.assertFalse(AccessType.PRIVATE.isLimitedAccess());
        Assert.assertTrue(AccessType.PRIVATE.isPrivateAccess());

        Assert.assertSame(AccessType.PUBLIC, AccessType.valueOf("PUBLIC"));
        Assert.assertEquals(6, AccessType.values().length);
    }
}
