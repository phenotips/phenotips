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

import org.phenotips.security.authorization.AuthorizationModule;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import java.lang.reflect.Type;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
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

    @Mock
    private AuthorizationModule moduleOne;

    @Mock
    private AuthorizationModule moduleTwo;

    @Mock
    private AuthorizationModule moduleThree;

    @Before
    public void setupMocks() throws Exception
    {
        // FIXME This should be done in MockitoComponentMockingRule automatically
        MockitoAnnotations.initMocks(this);
        resetMocks();
    }

    @Test
    public void defaultDecisionIsDeny() throws ComponentLookupException
    {
        for (ComponentDescriptor<?> cd : this.mocker.getComponentDescriptorList((Type) AuthorizationModule.class)) {
            this.mocker.unregisterComponent(cd);
        }
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
    }

    @Test
    public void moduleDecisionIsUsed() throws Exception
    {
        this.mocker.registerComponent(AuthorizationModule.class, "low", this.moduleOne);

        when(this.moduleOne.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));

        when(this.moduleOne.hasAccess(this.user, this.access, this.document)).thenReturn(false);
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));

        when(this.moduleOne.hasAccess(this.user, this.access, this.document)).thenReturn(null);
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
    }

    @Test
    public void modulesAreCascadedUntilNonNullIsReturned() throws Exception
    {
        this.mocker.registerComponent(AuthorizationModule.class, "one", this.moduleOne);
        this.mocker.registerComponent(AuthorizationModule.class, "two", this.moduleTwo);
        this.mocker.registerComponent(AuthorizationModule.class, "three", this.moduleThree);

        // By default all modules return null
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        verify(this.moduleThree).hasAccess(this.user, this.access, this.document);
        verify(this.moduleTwo).hasAccess(this.user, this.access, this.document);
        verify(this.moduleOne).hasAccess(this.user, this.access, this.document);

        resetMocks();
        when(this.moduleOne.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        verify(this.moduleOne).hasAccess(this.user, this.access, this.document);

        resetMocks();
        when(this.moduleTwo.hasAccess(this.user, this.access, this.document)).thenReturn(false);
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        verify(this.moduleTwo).hasAccess(this.user, this.access, this.document);

        resetMocks();
        when(this.moduleThree.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        verify(this.moduleThree).hasAccess(this.user, this.access, this.document);
    }

    @Test
    public void exceptionsInModulesAreIgnored() throws Exception
    {
        this.mocker.registerComponent(AuthorizationModule.class, "one", this.moduleOne);
        this.mocker.registerComponent(AuthorizationModule.class, "two", this.moduleTwo);

        when(this.moduleTwo.hasAccess(this.user, this.access, this.document)).thenThrow(
                new NullPointerException());
        when(this.moduleOne.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
    }

    private void resetMocks()
    {
        Mockito.reset(this.moduleOne, this.moduleTwo, this.moduleThree);
        when(this.moduleOne.hasAccess(this.user, this.access, this.document)).thenReturn(null);
        when(this.moduleTwo.hasAccess(this.user, this.access, this.document)).thenReturn(null);
        when(this.moduleThree.hasAccess(this.user, this.access, this.document)).thenReturn(null);
    }
}
