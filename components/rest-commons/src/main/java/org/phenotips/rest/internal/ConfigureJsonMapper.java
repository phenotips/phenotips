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

import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.module.SimpleModule;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.engine.Engine;
import org.restlet.engine.converter.ConverterHelper;
import org.restlet.ext.jackson.JacksonConverter;

/**
 * Configures the Jackson serializer used in the Restlet framework to know how to serialize classes from the
 * {@code org.json} library.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("ConfigureJsonMapper")
@Singleton
public class ConfigureJsonMapper extends AbstractEventListener
{
    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public ConfigureJsonMapper()
    {
        super("ConfigureJsonMapper", new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        for (ConverterHelper converter : Engine.getInstance().getRegisteredConverters()) {
            if (converter instanceof JacksonConverter) {
                JacksonConverter jconverter = (JacksonConverter) converter;
                SimpleModule m = new SimpleModule("org.json", new Version(1, 0, 0, null));
                m.addSerializer(JSONObject.class, new JSONObjectSerializer());
                m.addSerializer(JSONArray.class, new JSONArraySerializer());
                jconverter.getObjectMapper().registerModule(m);
            }
        }
    }

    class JSONObjectSerializer extends JsonSerializer<JSONObject>
    {
        @Override
        public void serialize(JSONObject value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            jgen.writeRawValue(value.toString());
        }
    }

    class JSONArraySerializer extends JsonSerializer<JSONArray>
    {
        @Override
        public void serialize(JSONArray value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            jgen.writeRawValue(value.toString());
        }
    }
}
