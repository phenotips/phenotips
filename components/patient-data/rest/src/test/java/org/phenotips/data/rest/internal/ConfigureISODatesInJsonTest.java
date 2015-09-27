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
package org.phenotips.data.rest.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.restlet.engine.Engine;
import org.restlet.engine.converter.ConverterHelper;
import org.restlet.ext.jackson.JacksonConverter;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.isA;

/**
 * Tests for the {@link ConfigureISODatesInJson} component.
 */
public class ConfigureISODatesInJsonTest
{
    @Rule
    public MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<EventListener>(ConfigureISODatesInJson.class);

    @Test
    public void checkConstruction() throws ComponentLookupException
    {
        EventListener testInstance = this.mocker.getComponentUnderTest();
        Assert.assertEquals("ConfigureISODatesInJson", testInstance.getName());
        Assert.assertThat(testInstance.getEvents(), hasItem(isA(ApplicationStartedEvent.class)));
    }

    @Test
    public void verifyOnEventConfiguresProperly() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().onEvent(null, null, null);

        for (ConverterHelper converter : Engine.getInstance().getRegisteredConverters()) {
            if (converter instanceof JacksonConverter) {
                JacksonConverter jconverter = (JacksonConverter) converter;
                jconverter.getObjectMapper().getSerializationConfig().isEnabled(Feature.WRITE_DATES_AS_TIMESTAMPS);
            }
        }
    }
}
