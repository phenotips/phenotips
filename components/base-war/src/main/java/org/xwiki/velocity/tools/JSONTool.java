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
package org.xwiki.velocity.tools;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import net.sf.json.JSON;
import net.sf.json.JSONException;
import net.sf.json.JSONSerializer;

/**
 * Velocity tool to facilitate serialization of Java objects to the JSON format.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class JSONTool
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JSONTool.class);

    /**
     * Serialize a Java object to the JSON format.
     * <p>
     * Examples:
     * <ul>
     * <li>numbers and boolean values: 23, 13.5, true, false</li>
     * <li>strings: "one\"two'three" (quotes included)</li>
     * <li>arrays and collections: [1, 2, 3]</li>
     * <li>maps: {"number": 23, "boolean": false, "string": "value"}</li>
     * <li>beans: {"enabled": true, "name": "XWiki"} for a bean that has #isEnabled() and #getName() getters</li>
     * </ul>
     *
     * @param object the object to be serialized to the JSON format
     * @return the JSON-verified string representation of the given object
     */
    public String serialize(Object object)
    {
        try {
            ObjectMapper mapper = new ObjectMapper();
            SimpleModule m = new SimpleModule("org.json.* serializer", new Version(1, 0, 0, "", "org.json", "json"));
            m.addSerializer(JSONObject.class, new JSONObjectSerializer());
            m.addSerializer(JSONArray.class, new JSONArraySerializer());
            mapper.registerModule(m);

            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize object to JSON", e);
        }

        return null;
    }

    /**
     * Parse JSON {@link String} into an {@link Object}.
     *
     * @param str the string to parse
     * @return the {@link Object} resolved from the string (usually a Map or a List)
     * @since 9.9RC1
     */
    public Object fromString(String str)
    {
        if (StringUtils.isNotBlank(str)) {
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                return objectMapper.readValue(str, Object.class);
            } catch (Exception e) {
                LOGGER.info("Failed to parse JSON [{}]: ", StringUtils.abbreviate(str, 32),
                    ExceptionUtils.getRootCauseMessage(e));

                return null;
            }
        }

        return null;
    }

    /**
     * Parse a serialized JSON into a real JSONObject. Only valid strings can be parsed, and doesn't support
     * JSONP. If the argument is not valid JSON, then {@code null} is returned.
     *
     * @param json the string to parse, must be valid JSON
     * @return the parsed JSON Object, or {@code null} if the argument is not a valid JSON
     * @since 1.3M4
     */
    public JSONObject parseToJSONObject(String json)
    {
        try {
            return new JSONObject(json);
        } catch (org.json.JSONException ex) {
            LOGGER.info("Tried to parse invalid JSON: [{}], exception was: {}",
                StringUtils.abbreviate(json, 32), ExceptionUtils.getRootCauseMessage(ex));
            return null;
        }
    }

    /**
     * Parse a serialized JSON into a real JSONArray. Only valid strings can be parsed, and doesn't support
     * JSONP. If the argument is not valid JSON, then {@code null} is returned.
     *
     * @param json the string to parse, must be valid JSON
     * @return the parsed JSON Array, or {@code null} if the argument is not a valid JSON array
     * @since 1.3M4
     */
    public JSONArray parseToJSONArray(String json)
    {
        try {
            return new JSONArray(json);
        } catch (org.json.JSONException ex) {
            LOGGER.info("Tried to parse invalid JSON: [{}], exception was: {}",
                StringUtils.abbreviate(json, 32), ExceptionUtils.getRootCauseMessage(ex));
            return null;
        }
    }

    class JSONObjectSerializer extends JsonSerializer<JSONObject>
    {
        @Override
        public void serialize(JSONObject value, JsonGenerator jgen, SerializerProvider provider) throws IOException
        {
            jgen.writeRawValue(value.toString());
        }
    }

    class JSONArraySerializer extends JsonSerializer<JSONArray>
    {
        @Override
        public void serialize(JSONArray value, JsonGenerator jgen, SerializerProvider provider) throws IOException
        {
            jgen.writeRawValue(value.toString());
        }
    }

    // Deprecated

    /**
     * Parse a serialized JSON into a real JSON object. Only valid JSON strings can be parsed, and doesn't support
     * JSONP. If the argument is not valid JSON, then {@code null} is returned.
     *
     * @param json the string to parse, must be valid JSON
     * @return the parsed JSON, either a {@link net.sf.json.JSONObject} or a {@link net.sf.json.JSONArray}, or
     *         {@code null} if the argument is not a valid JSON
     * @since 5.2M1
     * @deprecated since 9.9RC1, use {@link #fromString(String)} instead
     */
    @Deprecated
    public JSON parse(String json)
    {
        try {
            return JSONSerializer.toJSON(json);
        } catch (JSONException ex) {
            LOGGER.info("Tried to parse invalid JSON: [{}], exception was: {}", StringUtils.abbreviate(json, 32),
                ex.getMessage());
            return null;
        }
    }
}
