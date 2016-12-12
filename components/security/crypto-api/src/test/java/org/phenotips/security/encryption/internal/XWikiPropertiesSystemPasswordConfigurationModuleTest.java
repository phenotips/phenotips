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
package org.phenotips.security.encryption.internal;

import org.phenotips.security.encryption.SystemPasswordConfiguration;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class XWikiPropertiesSystemPasswordConfigurationModuleTest
{
    @Rule
    public MockitoComponentMockingRule<SystemPasswordConfiguration.ConfigurationModule> mocker =
        new MockitoComponentMockingRule<SystemPasswordConfiguration.ConfigurationModule>(
            XWikiPropertiesSystemPasswordConfigurationModule.class);

    @Mock
    private ConfigurationSource config;

    @Mock
    private SystemPasswordConfiguration pW;

    private static final String PROPERTY = "crypto.encryption.systemPassword";

    @Before
    public void initMocks()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getsCorrectSystemPassword() throws ComponentLookupException
    {
        when(this.config.getProperty(PROPERTY)).thenReturn(this.pW);
        Assert.assertEquals("xwikiproperties", this.mocker.getComponentUnderTest().getSystemPassword());
    }
}
