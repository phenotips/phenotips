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
package org.phenotips.entities.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.component.manager.ComponentLookupException;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class LazyPrimaryEntityIteratorTest
{
    @Mock
    private PrimaryEntityManager<PrimaryEntity> manager;

    @Mock
    private PrimaryEntity e1;

    @Mock
    private PrimaryEntity e2;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        when(this.manager.get("Entity01")).thenReturn(this.e1);
        when(this.manager.get("Entity02")).thenReturn(this.e2);
    }

    @Test
    public void emptyInputGivesEmptyIterator() throws ComponentLookupException
    {
        List<String> input = new LinkedList<>();
        LazyPrimaryEntityIterator<PrimaryEntity> iterator = new LazyPrimaryEntityIterator<>(input, this.manager);
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void basicFunctionalityTests() throws NoSuchElementException
    {
        List<String> input = new LinkedList<>();
        input.add("Entity01");
        input.add("Entity02");
        input.add("Entity03");

        LazyPrimaryEntityIterator<PrimaryEntity> iterator = new LazyPrimaryEntityIterator<>(input, this.manager);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(this.e1, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(this.e2, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertNull(iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removeThrowsUnsupportedOperationException() throws UnsupportedOperationException
    {
        List<String> input = new LinkedList<>();
        LazyPrimaryEntityIterator<PrimaryEntity> iterator = new LazyPrimaryEntityIterator<>(input, this.manager);
        iterator.remove();
    }
}
