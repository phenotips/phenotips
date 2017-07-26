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
package org.phenotips.configuration.script;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link RecordConfigurationManager} script service, {@link RecordConfigurationManagerScriptService}.
 *
 * @version $Id$
 */
public class RecordConfigurationManagerScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<RecordConfigurationManagerScriptService> mocker =
        new MockitoComponentMockingRule<>(
            RecordConfigurationManagerScriptService.class);

    /** Basic test for {@link RecordConfigurationManagerScriptService#getActiveConfiguration()}. */
    @Test
    public void getActiveConfiguration() throws ComponentLookupException
    {
        RecordConfiguration c = mock(RecordConfiguration.class);
        RecordConfigurationManager config = this.mocker.getInstance(RecordConfigurationManager.class);
        when(config.getActiveConfiguration()).thenReturn(c);
        Assert.assertSame(c, this.mocker.getComponentUnderTest().getActiveConfiguration());
    }
}
