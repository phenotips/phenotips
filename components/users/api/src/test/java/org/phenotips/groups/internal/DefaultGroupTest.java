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

    @Test
    public void equalsTest()
    {
        DocumentReference a1 = new DocumentReference("xwiki", "Groups", "Group A");
        DocumentReference a2 = new DocumentReference("xwiki", "Groups", "Group A");
        DocumentReference a3 = new DocumentReference("xwiki", "Groups", "Group B");
        Assert.assertTrue(new DefaultGroup(a1).equals(new DefaultGroup(a2)));
        Assert.assertFalse(new DefaultGroup(a1).equals(new DefaultGroup(a3)));
        Assert.assertFalse(new DefaultGroup(a1).equals("not a group"));
    }

    @Test
    public void hashCodeTest()
    {
        DocumentReference a = new DocumentReference("xwiki", "Groups", "group A");
        Assert.assertTrue(new DefaultGroup(a).hashCode() == "group A".hashCode());
        Assert.assertFalse(new DefaultGroup(a).hashCode() == "aaa".hashCode());
    }
}
