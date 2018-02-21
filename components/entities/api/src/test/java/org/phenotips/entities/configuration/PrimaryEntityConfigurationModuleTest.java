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
package org.phenotips.entities.configuration;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationBuilder;
import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationModule;

import org.xwiki.component.manager.ComponentLookupException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for the {@link PrimaryEntityConfigurationModule} base methods.
 *
 * @version $Id$
 */
public class PrimaryEntityConfigurationModuleTest
{
    @Test
    public void supportsRecordForwardsToSupportsEntityType() throws ComponentLookupException
    {
        PrimaryEntity entity = Mockito.mock(PrimaryEntity.class);
        Assert.assertFalse(new TestModule(false).supportsRecord(entity));
        Assert.assertTrue(new TestModule(true).supportsRecord(entity));
    }

    @Test
    public void supportsRecordReturnsFalseForNull() throws ComponentLookupException
    {
        Assert.assertFalse(new TestModule(false).supportsRecord(null));
        Assert.assertFalse(new TestModule(true).supportsRecord(null));
    }

    @Test
    public void processRecordForwardsToProcessType() throws ComponentLookupException
    {
        PrimaryEntity entity = Mockito.mock(PrimaryEntity.class);
        PrimaryEntityConfigurationBuilder config = Mockito.mock(PrimaryEntityConfigurationBuilder.class);
        Assert.assertSame(config, new TestModule(false).process(config, entity));
    }

    private static class TestModule extends PrimaryEntityConfigurationModule
    {
        private final boolean supports;

        TestModule(boolean supports)
        {
            this.supports = supports;
        }

        @Override
        public PrimaryEntityConfigurationBuilder process(PrimaryEntityConfigurationBuilder config)
        {

            return config;
        }

        @Override
        public int getPriority()
        {
            return 0;
        }

        @Override
        public boolean supportsEntityType(String recordType)
        {
            return this.supports;
        }
    }
}
