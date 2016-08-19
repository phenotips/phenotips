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
package org.phenotips.rest.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.restlet.engine.Engine;
import org.restlet.engine.converter.ConverterHelper;
import org.restlet.ext.jackson.JacksonConverter;

/**
 * Configures the Jackson serializer used in the Restlet framework to skip null fields.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("ConfigureNonNullFieldsInJson")
@Singleton
public class ConfigureNonNullFieldsInJson extends AbstractEventListener
{
    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public ConfigureNonNullFieldsInJson()
    {
        super("ConfigureNonNullFieldsInJson", new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        for (ConverterHelper converter : Engine.getInstance().getRegisteredConverters()) {
            if (converter instanceof JacksonConverter) {
                JacksonConverter jconverter = (JacksonConverter) converter;
                jconverter.getObjectMapper().setSerializationInclusion(Inclusion.NON_NULL);
            }
        }
    }
}
