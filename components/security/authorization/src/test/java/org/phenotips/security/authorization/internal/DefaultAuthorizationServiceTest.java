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

import org.mockito.InOrder;
import org.phenotips.security.authorization.AuthorizationModule;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.inject.Provider;

import static org.mockito.Mockito.doReturn;
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

    @Mock
    private AuthorizationModule moduleOne;

    @Mock
    private AuthorizationModule moduleTwo;

    @Mock
    private AuthorizationModule moduleThree;

    @Mock
    private Provider<List<AuthorizationModule>> modules;

    private List<AuthorizationModule> moduleList;

    @Before
    public void setupMocks() throws Exception
    {
        // FIXME This should be done in MockitoComponentMockingRule automatically
        MockitoAnnotations.initMocks(this);
        resetMocks();
        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "modules", this.modules);
    }

    @Test
    public void defaultDecisionIsDeny() throws ComponentLookupException
    {
        doReturn(new LinkedList<>()).when(this.modules).get();
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
    }

    @Test
    public void moduleDecisionIsUsed() throws Exception
    {
        this.moduleList = Collections.singletonList(this.moduleOne);
        doReturn(this.moduleList).when(this.modules).get();

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
        this.moduleList = Arrays.asList(this.moduleOne, this.moduleTwo, this.moduleThree);
        doReturn(this.moduleList).when(this.modules).get();

        // By default all modules return null
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        InOrder order = Mockito.inOrder(this.moduleOne, this.moduleTwo, this.moduleThree);
        order.verify(this.moduleOne).hasAccess(this.user, this.access, this.document);
        order.verify(this.moduleTwo).hasAccess(this.user, this.access, this.document);
        order.verify(this.moduleThree).hasAccess(this.user, this.access, this.document);

        resetMocks();
        when(this.moduleOne.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        order = Mockito.inOrder(this.moduleOne, this.moduleTwo, this.moduleThree);
        order.verify(this.moduleOne).hasAccess(this.user, this.access, this.document);
        order.verify(this.moduleTwo, never()).hasAccess(this.user, this.access, this.document);
        order.verify(this.moduleThree, never()).hasAccess(this.user, this.access, this.document);

        resetMocks();
        when(this.moduleTwo.hasAccess(this.user, this.access, this.document)).thenReturn(false);
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        order = Mockito.inOrder(this.moduleOne, this.moduleTwo, this.moduleThree);
        order.verify(this.moduleOne).hasAccess(this.user, this.access, this.document);
        order.verify(this.moduleTwo).hasAccess(this.user, this.access, this.document);
        order.verify(this.moduleThree, never()).hasAccess(this.user, this.access, this.document);

        resetMocks();
        when(this.moduleThree.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
        order = Mockito.inOrder(this.moduleOne, this.moduleTwo, this.moduleThree);
        order.verify(this.moduleOne).hasAccess(this.user, this.access, this.document);
        order.verify(this.moduleTwo).hasAccess(this.user, this.access, this.document);
        order.verify(this.moduleThree).hasAccess(this.user, this.access, this.document);
    }

    @Test
    public void firstNonNullDecisionIsReturned() throws Exception
    {
        this.moduleList = Arrays.asList(this.moduleOne, this.moduleTwo);
        doReturn(this.moduleList).when(this.modules).get();

        when(this.moduleOne.hasAccess(this.user, this.access, this.document)).thenReturn(true);
        when(this.moduleTwo.hasAccess(this.user, this.access, this.document)).thenReturn(false);

        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));

        when(this.moduleOne.hasAccess(this.user, this.access, this.document)).thenReturn(false);
        when(this.moduleTwo.hasAccess(this.user, this.access, this.document)).thenReturn(true);

        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.access, this.document));
    }

    @Test
    public void exceptionsInModulesAreIgnored() throws Exception
    {
        this.moduleList = Arrays.asList(this.moduleOne, this.moduleTwo);
        doReturn(this.moduleList).when(this.modules).get();

        when(this.moduleOne.hasAccess(this.user, this.access, this.document)).thenThrow(
                new NullPointerException());
        when(this.moduleTwo.hasAccess(this.user, this.access, this.document)).thenReturn(true);
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
