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
package org.phenotips.security.audit.internal;

import org.phenotips.security.audit.spi.AuditEventProcessor;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
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

/**
 * Test for the {@link AuditEventProcessorsListProvider} component.
 *
 * @version $Id$
 */
public class AuditEventProcessorsListProviderTest
{

    @Rule
    public MockitoComponentMockingRule<Provider<List<AuditEventProcessor>>> mocker =
        new MockitoComponentMockingRule<>(AuditEventProcessorsListProvider.class);

    @Mock
    private AuditEventProcessor processor1;

    @Mock
    private AuditEventProcessor processor2;

    private ComponentManager componentManager;

    private List<AuditEventProcessor> processorsList;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.processorsList = Arrays.asList(this.processor1, this.processor2);
        this.componentManager = this.mocker.getInstance(ComponentManager.class, "wiki");
        doReturn(this.processorsList).when(this.componentManager).getInstanceList(AuditEventProcessor.class);
    }

    @Test
    public void allProcessorsAreReturned() throws Exception
    {
        List<AuditEventProcessor> actualList = this.mocker.getComponentUnderTest().get();

        Assert.assertEquals(2, actualList.size());
        Assert.assertTrue(actualList.contains(this.processor1));
        Assert.assertTrue(actualList.contains(this.processor2));

    }

    @Test(expected = RuntimeException.class)
    public void componentLookupExceptionIsCaughtAndRuntimeExceptionIsThrown() throws ComponentLookupException
    {
        doThrow(new ComponentLookupException("test")).when(this.componentManager).getInstanceList(
            AuditEventProcessor.class);
        this.mocker.getComponentUnderTest().get();
    }
}
