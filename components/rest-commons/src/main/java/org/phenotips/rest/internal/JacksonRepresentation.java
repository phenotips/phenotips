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

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Representation based on the Jackson library. It can serialize and deserialize automatically in JSON, JSON binary
 * (Smile), XML, YAML and CSV. <br>
 * <br>
 * SECURITY WARNING: Using XML parsers configured to not prevent nor limit document type definition (DTD) entity
 * resolution can expose the parser to an XML Entity Expansion injection attack. .
 *
 * @param <T> The type to wrap.
 * @see <a href="http://jackson.codehaus.org/">Jackson project</a>
 * @see <a href="https://github.com/restlet/restlet-framework-java/wiki/XEE-security-enhancements">XML Entity Expansion
 *      injection attack</a>
 * @version $Id$
 * @since 1.5
 */
public class JacksonRepresentation<T> extends OutputRepresentation
{
    /** The (parsed) object to format. */
    private volatile T object;

    /** The object class to instantiate. */
    private volatile Class<T> objectClass;

    /** The modifiable Jackson object mapper. */
    private volatile ObjectMapper objectMapper;

    /** The modifiable Jackson object reader. */
    private volatile ObjectReader objectReader;

    /** The modifiable Jackson object writer. */
    private volatile ObjectWriter objectWriter;

    /** The representation to parse. */
    private volatile Representation representation;

    /**
     * Constructor.
     *
     * @param mediaType The target media type.
     * @param object The object to format.
     */
    @SuppressWarnings("unchecked")
    public JacksonRepresentation(MediaType mediaType, T object)
    {
        super(mediaType);
        this.object = object;
        this.objectClass = (Class<T>) ((object == null) ? null : object
            .getClass());
        this.representation = null;
        this.objectMapper = null;
        this.objectReader = null;
        this.objectWriter = null;
    }

    /**
     * Constructor.
     *
     * @param representation The representation to parse.
     * @param objectClass The object class to instantiate.
     */
    public JacksonRepresentation(Representation representation,
        Class<T> objectClass)
    {
        super(representation.getMediaType());
        this.object = null;
        this.objectClass = objectClass;
        this.representation = representation;
        this.objectMapper = null;
        this.objectReader = null;
        this.objectWriter = null;
    }

    /**
     * Constructor for the JSON media type. .
     *
     * @param object The object to format.
     */
    public JacksonRepresentation(T object)
    {
        this(MediaType.APPLICATION_JSON, object);
    }

    /**
     * Creates a Jackson object mapper based on a media type. It supports JSON, JSON Smile, XML, YAML and CSV. .
     *
     * @return The Jackson object mapper.
     */
    private ObjectMapper createObjectMapper()
    {
        ObjectMapper result = null;

        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(Feature.AUTO_CLOSE_TARGET, false);
        result = new ObjectMapper(jsonFactory);
        result.setSerializationInclusion(Include.NON_NULL);
        result.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        registerOrgJsonMapper(result);

        return result;
    }

    private void registerOrgJsonMapper(ObjectMapper mapper)
    {
        SimpleModule m = new SimpleModule("org.json", new Version(1, 0, 0, null, "org.json", "json"));
        m.addSerializer(JSONObject.class, new JSONObjectSerializer());
        m.addSerializer(JSONArray.class, new JSONArraySerializer());
        mapper.registerModule(m);
    }

    /**
     * Creates a Jackson object reader based on a mapper. Has a special handling for CSV media types. .
     *
     * @return The Jackson object reader.
     */
    private ObjectReader createObjectReader()
    {
        return getObjectMapper().readerFor(getObjectClass());
    }

    /**
     * Creates a Jackson object writer based on a mapper. Has a special handling for CSV media types. .
     *
     * @return The Jackson object writer.
     */
    private ObjectWriter createObjectWriter()
    {
        return getObjectMapper().writerFor(getObjectClass());
    }

    /**
     * Returns the wrapped object, deserializing the representation with Jackson if necessary. .
     *
     * @return The wrapped object.
     * @throws IOException thrown when accessing the representation
     */
    public T getObject() throws IOException
    {
        T result = null;

        if (this.object != null) {
            result = this.object;
        } else if (this.representation != null) {
            result = getObjectReader().readValue(
                this.representation.getStream());
        }

        return result;
    }

    /**
     * Returns the object class to instantiate. .
     *
     * @return The object class to instantiate.
     */
    private Class<T> getObjectClass()
    {
        return this.objectClass;
    }

    /**
     * Returns the modifiable Jackson object mapper. Useful to customize mappings. .
     *
     * @return The modifiable Jackson object mapper.
     */
    private ObjectMapper getObjectMapper()
    {
        if (this.objectMapper == null) {
            this.objectMapper = createObjectMapper();
        }

        return this.objectMapper;
    }

    /**
     * Returns the modifiable Jackson object reader. Useful to customize deserialization.
     *
     * @return The modifiable Jackson object reader.
     */
    private ObjectReader getObjectReader()
    {
        if (this.objectReader == null) {
            this.objectReader = createObjectReader();
        }

        return this.objectReader;
    }

    /**
     * Returns the modifiable Jackson object writer. Useful to customize serialization.
     *
     * @return The modifiable Jackson object writer.
     */
    private ObjectWriter getObjectWriter()
    {
        if (this.objectWriter == null) {
            this.objectWriter = createObjectWriter();
        }

        return this.objectWriter;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException
    {
        if (this.representation != null) {
            this.representation.write(outputStream);
        } else if (this.object != null) {
            getObjectWriter().writeValue(outputStream, this.object);
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
