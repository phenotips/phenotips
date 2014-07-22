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
package org.phenotips.security.authorization.internal;

import org.phenotips.security.authorization.AuthorizationModule;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link AuthorizationService} component, {@link DefaultAuthorizationService}.
 *
 * @version $Id$
 */
public class DefaultAuthorizationServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationService> mocker =
        new MockitoComponentMockingRule<AuthorizationService>(DefaultAuthorizationService.class);

    @Mock
    private User user;

    @Mock
    private Right access;

    @Mock
    private DocumentReference document;

    private Provider<List<AuthorizationModule>> modules;

    @Mock
    private AuthorizationModule lowPriorityModule;

    @Mock
    private AuthorizationModule mediumPriorityModule;

    @Mock
    private AuthorizationModule highPriorityModule;

    @Before
    public void setupMocks() throws ComponentLookupException
    {
        // FIXME This should be done in MockitoComponentMockingRule automatically
        MockitoAnnotations.initMocks(this);
        this.modules =
            this.mocker.getInstance(new DefaultParameterizedType(null, Provider.class, new DefaultParameterizedType(
                null, List.class, AuthorizationModule.class)));
        resetMocks();
    }

    @Test
    public void defaultDecisionIsDeny() throws ComponentLookupException
    {
        when(this.modules.get()).thenReturn(Collections.<AuthorizationModule>emptyList());
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
    }

    @Test
    public void moduleDecisionIsUsed() throws ComponentLookupException
    {
        when(this.modules.get()).thenReturn(Collections.singletonList(this.lowPriorityModule));

        when(this.lowPriorityModule.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));

        when(this.lowPriorityModule.hasAccess(this.user, this.access, this.document)).thenReturn(false);
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));

        when(this.lowPriorityModule.hasAccess(this.user, this.access, this.document)).thenReturn(null);
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
    }

    @Test
    public void modulesAreCascadedUntilNonNullIsReturned() throws ComponentLookupException
    {
        when(this.modules.get()).thenReturn(
            Arrays.asList(this.lowPriorityModule, this.mediumPriorityModule, this.highPriorityModule));

        // By default all modules return null
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        InOrder order = Mockito.inOrder(this.lowPriorityModule, this.mediumPriorityModule, this.highPriorityModule);
        order.verify(this.highPriorityModule).hasAccess(this.user, this.access, this.document);
        order.verify(this.mediumPriorityModule).hasAccess(this.user, this.access, this.document);
        order.verify(this.lowPriorityModule).hasAccess(this.user, this.access, this.document);

        // The last module queried is the low priority module
        resetMocks();
        when(this.lowPriorityModule.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        order = Mockito.inOrder(this.lowPriorityModule, this.mediumPriorityModule, this.highPriorityModule);
        order.verify(this.highPriorityModule).hasAccess(this.user, this.access, this.document);
        order.verify(this.mediumPriorityModule).hasAccess(this.user, this.access, this.document);
        order.verify(this.lowPriorityModule).hasAccess(this.user, this.access, this.document);

        // Then the middle priority one
        resetMocks();
        when(this.mediumPriorityModule.hasAccess(this.user, this.access, this.document)).thenReturn(false);
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        order = Mockito.inOrder(this.lowPriorityModule, this.mediumPriorityModule, this.highPriorityModule);
        order.verify(this.highPriorityModule).hasAccess(this.user, this.access, this.document);
        order.verify(this.mediumPriorityModule).hasAccess(this.user, this.access, this.document);
        order.verify(this.lowPriorityModule, never()).hasAccess(this.user, this.access, this.document);

        // And finally the high priority one
        resetMocks();
        when(this.highPriorityModule.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        order = Mockito.inOrder(this.lowPriorityModule, this.mediumPriorityModule, this.highPriorityModule);
        order.verify(this.highPriorityModule).hasAccess(this.user, this.access, this.document);
        order.verify(this.mediumPriorityModule, never()).hasAccess(this.user, this.access, this.document);
        order.verify(this.lowPriorityModule, never()).hasAccess(this.user, this.access, this.document);
    }

    @Test
    public void exceptionsInModulesAreIgnored() throws ComponentLookupException
    {
        when(this.modules.get()).thenReturn(Arrays.asList(this.lowPriorityModule, this.highPriorityModule));

        when(this.highPriorityModule.hasAccess(this.user, this.access, this.document)).thenThrow(
            new NullPointerException());
        when(this.lowPriorityModule.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
    }

    @Test
    public void modulesWithSamePriorityGetSortedByName() throws ComponentLookupException
    {
        when(this.modules.get()).thenReturn(
            Arrays.<AuthorizationModule>asList(new BModule(), new AModule(), new CModule()));
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
    }

    private void resetMocks()
    {
        Mockito.reset(this.lowPriorityModule, this.mediumPriorityModule, this.highPriorityModule);
        when(this.lowPriorityModule.getPriority()).thenReturn(1);
        when(this.mediumPriorityModule.getPriority()).thenReturn(2);
        when(this.highPriorityModule.getPriority()).thenReturn(3);
        when(this.lowPriorityModule.hasAccess(this.user, this.access, this.document)).thenReturn(null);
        when(this.mediumPriorityModule.hasAccess(this.user, this.access, this.document)).thenReturn(null);
        when(this.highPriorityModule.hasAccess(this.user, this.access, this.document)).thenReturn(null);
    }

    private static class AModule implements AuthorizationModule
    {
        @Override
        public int getPriority()
        {
            return 0;
        }

        @Override
        public Boolean hasAccess(User user, Right access, DocumentReference document)
        {
            return Boolean.TRUE;
        }
    }

    private static class BModule extends AModule
    {
        @Override
        public Boolean hasAccess(User user, Right access, DocumentReference document)
        {
            return Boolean.FALSE;
        }
    }

    private static class CModule extends BModule
    {
        // All the methods of B are reused
    }
}
