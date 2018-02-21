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
package org.phenotips.entities.configuration.script;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.configuration.PrimaryEntityConfiguration;
import org.phenotips.entities.configuration.PrimaryEntityConfigurationManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PrimaryEntityConfigurationManager} script service,
 * {@link PrimaryEntityConfigurationManagerScriptService}.
 *
 * @version $Id$
 */
public class PrimaryEntityConfigurationManagerScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<PrimaryEntityConfigurationManagerScriptService> mocker =
        new MockitoComponentMockingRule<>(
            PrimaryEntityConfigurationManagerScriptService.class);

    @Test
    public void getConfigurationForTypeForwardsCalls() throws ComponentLookupException
    {
        PrimaryEntityConfiguration c = mock(PrimaryEntityConfiguration.class);
        PrimaryEntityConfigurationManager config = this.mocker.getInstance(PrimaryEntityConfigurationManager.class);
        when(config.getConfiguration("patient")).thenReturn(c);
        Assert.assertSame(c, this.mocker.getComponentUnderTest().getConfiguration("patient"));
    }

    @Test
    public void getConfigurationForEntityForwardsCalls() throws ComponentLookupException
    {
        PrimaryEntity e = mock(PrimaryEntity.class);
        PrimaryEntityConfiguration c = mock(PrimaryEntityConfiguration.class);
        PrimaryEntityConfigurationManager config = this.mocker.getInstance(PrimaryEntityConfigurationManager.class);
        when(config.getConfiguration(e)).thenReturn(c);
        Assert.assertSame(c, this.mocker.getComponentUnderTest().getConfiguration(e));
    }
}
