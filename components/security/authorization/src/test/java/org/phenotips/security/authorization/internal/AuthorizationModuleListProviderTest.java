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
package org.phenotips.security.authorization.internal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.security.authorization.AuthorizationModule;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 *  Test for the {@link AuthorizationModuleListProvider} component
 */
public class AuthorizationModuleListProviderTest {

    @Rule
    public MockitoComponentMockingRule<Provider<List<AuthorizationModule>>> mocker =
        new MockitoComponentMockingRule<Provider<List<AuthorizationModule>>>(AuthorizationModuleListProvider.class);

    @Mock
    private AuthorizationModule lowPriorityModule;

    @Mock
    private AuthorizationModule mediumPriorityModule;

    @Mock
    private AuthorizationModule highPriorityModule;

    private ComponentManager componentManager;

    private List<AuthorizationModule> moduleList;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        when(this.lowPriorityModule.getPriority()).thenReturn(1);
        when(this.mediumPriorityModule.getPriority()).thenReturn(2);
        when(this.highPriorityModule.getPriority()).thenReturn(3);

        this.moduleList = new LinkedList<>();
        this.componentManager = this.mocker.getInstance(ComponentManager.class, "wiki");
        doReturn(this.moduleList).when(this.componentManager).getInstanceList(AuthorizationModule.class);
    }

    @Test
    public void modulesAreSortedByPriority() throws Exception
    {
        this.moduleList.add(this.mediumPriorityModule);
        this.moduleList.add(this.lowPriorityModule);
        this.moduleList.add(this.highPriorityModule);

        List<AuthorizationModule> expectedList = Arrays.asList(
            this.highPriorityModule, this.mediumPriorityModule, this.lowPriorityModule);
        List<AuthorizationModule> actualList = this.mocker.getComponentUnderTest().get();

        Assert.assertThat(actualList, is(expectedList));

    }

    @Test
    public void modulesWithSamePriorityGetSortedByName() throws Exception
    {
        AuthorizationModule aModule = new AModule();
        AuthorizationModule bModule = new BModule();
        AuthorizationModule cModule = new CModule();
        this.moduleList.add(bModule);
        this.moduleList.add(aModule);
        this.moduleList.add(cModule);

        List<AuthorizationModule> expectedList = Arrays.asList(
            aModule, bModule, cModule);
        List<AuthorizationModule> actualList = this.mocker.getComponentUnderTest().get();

        Assert.assertThat(actualList, is(expectedList));
    }

    @Test(expected = RuntimeException.class)
    public void componentLookupExceptionIsCaughtAndRuntimeExceptionIsThrown() throws ComponentLookupException
    {
        doThrow(new ComponentLookupException("test")).when(this.componentManager).getInstanceList(
            AuthorizationModule.class);
        this.mocker.getComponentUnderTest().get();
    }

    private static class AModule implements AuthorizationModule
    {
        @Override
        public int getPriority()
        {
            return 0;
        }

        @Override
        public Boolean hasAccess(User user, Right access, EntityReference entity)
        {
            return Boolean.TRUE;
        }
    }

    private static class BModule extends AModule
    {
        @Override
        public Boolean hasAccess(User user, Right access, EntityReference entity)
        {
            return Boolean.FALSE;
        }
    }

    private static class CModule extends BModule
    {
        // All the methods of B are reused
    }
}
