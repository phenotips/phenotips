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
package org.phenotips.groups.internal;

import org.phenotips.groups.Group;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the default {@link Group} implementation, {@link DefaultGroup}.
 *
 * @version $Id$
 */
public class DefaultGroupTest
{
    /** Basic tests for {@link DefaultGroup#getReference()}. */
    @Test
    public void getReference() throws ComponentLookupException, QueryException
    {
        Group g = new DefaultGroup(null);
        Assert.assertNull(g.getReference());

        DocumentReference a = new DocumentReference("xwiki", "Groups", "Group A");
        Assert.assertEquals(a, new DefaultGroup(a).getReference());
    }

    /** There's a nicer toString implementation showing the group name. */
    @Test
    public void toStringTest()
    {
        DocumentReference a = new DocumentReference("xwiki", "Groups", "Group A");
        Assert.assertTrue(new DefaultGroup(a).toString().contains("Group A"));
    }
}
