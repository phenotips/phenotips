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
package org.phenotips.data.permissions.rest.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.restlet.engine.Engine;
import org.restlet.engine.converter.ConverterHelper;
import org.restlet.ext.jackson.JacksonConverter;


/* todo. figure out if this class should stay. it is a duplicate of a class in patient-data */


/**
 * Configures the Jackson serializer used in the Restlet framework to print dates as ISO-formatted string instead of
 * simple numerical timestamps.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("ConfigureISODatesInJson")
@Singleton
public class ConfigureISODatesInJson extends AbstractEventListener
{
    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public ConfigureISODatesInJson()
    {
        super("ConfigureISODatesInJson", new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        for (ConverterHelper converter : Engine.getInstance().getRegisteredConverters()) {
            if (converter instanceof JacksonConverter) {
                JacksonConverter jconverter = (JacksonConverter) converter;
                jconverter.getObjectMapper().configure(Feature.WRITE_DATES_AS_TIMESTAMPS, false);
            }
        }
    }
}
