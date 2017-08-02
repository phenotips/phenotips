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
package org.phenotips.configuration.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.spi.RecordConfigurationModule;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class RecordConfigurationModuleProviderTest
{
    @Rule
    public MockitoComponentMockingRule<Provider<List<RecordConfigurationModule>>> mocker =
        new MockitoComponentMockingRule<>(
            RecordConfigurationModuleProvider.class);

    @Mock
    private RecordConfigurationModule lowPriorityModule;

    @Mock
    private RecordConfigurationModule mediumPriorityModule;

    @Mock
    private RecordConfigurationModule highPriorityModule;

    private ComponentManager cm;

    private List<RecordConfigurationModule> moduleList;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        when(this.lowPriorityModule.getPriority()).thenReturn(1);
        when(this.mediumPriorityModule.getPriority()).thenReturn(2);
        when(this.highPriorityModule.getPriority()).thenReturn(3);

        this.moduleList = new LinkedList<>();
        this.cm = this.mocker.getInstance(ComponentManager.class, "wiki");
        doReturn(this.moduleList).when(this.cm).getInstanceList(RecordConfigurationModule.class);
    }

    @Test
    public void modulesAreSortedByPriority() throws Exception
    {
        this.moduleList.add(this.mediumPriorityModule);
        this.moduleList.add(this.lowPriorityModule);
        this.moduleList.add(this.highPriorityModule);

        List<RecordConfigurationModule> expectedList = Arrays.asList(this.lowPriorityModule, this.mediumPriorityModule,
            this.highPriorityModule);
        List<RecordConfigurationModule> actualList = this.mocker.getComponentUnderTest().get();

        Assert.assertEquals(expectedList, actualList);
    }

    @Test
    public void modulesWithSamePriorityAreSortedByName() throws Exception
    {
        RecordConfigurationModule aModule = new AModule();
        RecordConfigurationModule bModule = new BModule();
        RecordConfigurationModule cModule = new CModule();
        this.moduleList.add(bModule);
        this.moduleList.add(aModule);
        this.moduleList.add(cModule);

        List<RecordConfigurationModule> expectedList = Arrays.asList(aModule, bModule, cModule);
        List<RecordConfigurationModule> actualList = this.mocker.getComponentUnderTest().get();

        Assert.assertEquals(expectedList, actualList);
    }

    @Test(expected = RuntimeException.class)
    public void componentLookupExceptionIsCaughtAndRuntimeExceptionIsThrown() throws ComponentLookupException
    {
        doThrow(new ComponentLookupException("test")).when(this.cm).getInstanceList(RecordConfigurationModule.class);
        this.mocker.getComponentUnderTest().get();
    }

    private static class AModule implements RecordConfigurationModule
    {
        @Override
        public int getPriority()
        {
            return 0;
        }

        @Override
        public RecordConfiguration process(RecordConfiguration config)
        {
            return null;
        }

        @Override
        public boolean supportsRecordType(String recordType)
        {
            return true;
        }
    }

    private static class BModule extends AModule
    {
        // Reuses methods of A
    }

    private static class CModule extends BModule
    {
        // Reuses methods of B
    }
}
