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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link BaseAuthorizationModule base} {@link AuthorizationModule} component.
 *
 * @version $Id$
 */
public class BaseAuthorizationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<AuthorizationModule>(BaseAuthorizationModule.class);

    @Mock
    private User user;

    @Mock
    private Right right;

    @Mock
    private DocumentReference doc;

    @Test
    public void defaultDecisionIsDeny() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void nullArgumentsAreIgnored() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(null, null, null));
    }

    @Test
    public void configurationIsSupported() throws ComponentLookupException
    {
        ConfigurationSource config = this.mocker.getInstance(ConfigurationSource.class, "restricted");

        when(config.getProperty("phenotips.security.authorization.allowAllAccessByDefault")).thenReturn(Boolean.TRUE);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));

        when(config.getProperty("phenotips.security.authorization.allowAllAccessByDefault")).thenReturn(Boolean.FALSE);
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void priorityIsLowest() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPriority());
    }
}
