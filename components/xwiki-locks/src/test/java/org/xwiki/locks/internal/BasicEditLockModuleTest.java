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
package org.xwiki.locks.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.locks.LockModule;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Tests for the {@link BaseLockModule base} {@link LockModule} component.
 *
 * @version $Id$
 */
public class BasicEditLockModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<LockModule> mocker =
        new MockitoComponentMockingRule<LockModule>(BasicEditLockModule.class);

    @Mock
    private User user;

    @Mock
    private DocumentReference doc;

    @Test
    public void priorityIsLowest() throws ComponentLookupException
    {
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getPriority());
    }
}
