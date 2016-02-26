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
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.locks.DocumentLock;
import org.xwiki.locks.LockModule;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link LockModuleListProvider} component
 */
public class LockModuleListProviderTest
{

    @Rule
    public MockitoComponentMockingRule<Provider<List<LockModule>>> mocker =
        new MockitoComponentMockingRule<Provider<List<LockModule>>>(LockModuleListProvider.class);

    @Mock
    private LockModule lowPriorityModule;

    @Mock
    private LockModule mediumPriorityModule;

    @Mock
    private LockModule highPriorityModule;

    private ComponentManager componentManager;

    private List<LockModule> moduleList;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        when(this.lowPriorityModule.getPriority()).thenReturn(1);
        when(this.mediumPriorityModule.getPriority()).thenReturn(2);
        when(this.highPriorityModule.getPriority()).thenReturn(3);

        this.moduleList = new LinkedList<>();
        this.componentManager = ((ComponentManager) this.mocker).getInstance(ComponentManager.class, "wiki");
        doReturn(this.moduleList).when(this.componentManager).getInstanceList(LockModule.class);
    }

    @Test
    public void modulesAreSortedByPriority() throws Exception
    {
        this.moduleList.add(this.mediumPriorityModule);
        this.moduleList.add(this.lowPriorityModule);
        this.moduleList.add(this.highPriorityModule);

        List<LockModule> expectedList = Arrays.asList(
            this.highPriorityModule, this.mediumPriorityModule, this.lowPriorityModule);
        List<LockModule> actualList = this.mocker.getComponentUnderTest().get();

        Assert.assertThat(actualList, is(expectedList));

    }

    @Test
    public void modulesWithSamePriorityGetSortedByName() throws Exception
    {
        LockModule aModule = new AModule();
        LockModule bModule = new BModule();
        LockModule cModule = new CModule();
        this.moduleList.add(bModule);
        this.moduleList.add(aModule);
        this.moduleList.add(cModule);

        List<LockModule> expectedList = Arrays.asList(
            aModule, bModule, cModule);
        List<LockModule> actualList = this.mocker.getComponentUnderTest().get();

        Assert.assertThat(actualList, is(expectedList));
    }

    @Test(expected = RuntimeException.class)
    public void componentLookupExceptionIsCaughtAndRuntimeExceptionIsThrown() throws ComponentLookupException
    {
        doThrow(new ComponentLookupException("test")).when(this.componentManager).getInstanceList(
            LockModule.class);
        this.mocker.getComponentUnderTest().get();
    }

    private static class AModule implements LockModule
    {
        @Override
        public int getPriority()
        {
            return 100;
        }

        @Override
        public DocumentLock getLock(DocumentReference document)
        {
            return null;
        }
    }

    private static class BModule extends AModule
    {
        @Override
        public DocumentLock getLock(DocumentReference document)
        {
            return null;
        }
    }

    private static class CModule extends BModule
    {
        // All the methods of B are reused
    }
}
