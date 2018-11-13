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
import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.engine.converter.ConverterHelper;
import org.restlet.engine.resource.VariantInfo;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Resource;

/**
 * Converter between JSON and Representation based on Jackson.
 *
 * @version $Id$
 * @since 1.5
 */
public class JacksonConverter extends ConverterHelper
{
    /** Variant with media type application/json. */
    private static final VariantInfo VARIANT_JSON = new VariantInfo(
        MediaType.APPLICATION_JSON);

    /**
     * Creates the marshaling {@link JacksonRepresentation}.
     *
     * @param <T> the type of object to represent
     * @param mediaType The target media type.
     * @param source The source object to marshal.
     * @return The marshaling {@link JacksonRepresentation}.
     */
    protected <T> JacksonRepresentation<T> create(MediaType mediaType, T source)
    {
        return new JacksonRepresentation<>(mediaType, source);
    }

    /**
     * Creates the unmarshaling {@link JacksonRepresentation}.
     *
     * @param <T> the type of object to represent
     * @param source The source representation to unmarshal.
     * @param objectClass The object class to instantiate.
     * @return The unmarshaling {@link JacksonRepresentation}.
     */
    protected <T> JacksonRepresentation<T> create(Representation source,
        Class<T> objectClass)
    {
        return new JacksonRepresentation<>(source, objectClass);
    }

    @Override
    public List<Class<?>> getObjectClasses(Variant source)
    {
        List<Class<?>> result = null;

        if (isCompatible(source)) {
            result = addObjectClass(result, Object.class);
            result = addObjectClass(result, JacksonRepresentation.class);
        }

        return result;
    }

    @Override
    public List<VariantInfo> getVariants(Class<?> source)
    {
        List<VariantInfo> result = null;

        if (source != null) {
            result = addVariant(result, VARIANT_JSON);
        }

        return result;
    }

    /**
     * Indicates if the given variant is compatible with the media types supported by this converter.
     *
     * @param variant The variant.
     * @return True if the given variant is compatible with the media types supported by this converter.
     */
    protected boolean isCompatible(Variant variant)
    {
        return (variant != null) && VARIANT_JSON.isCompatible(variant);
    }

    @Override
    public float score(Object source, Variant target, Resource resource)
    {
        float result = -1.0F;

        if (source instanceof JacksonRepresentation<?>) {
            result = 1.0F;
        } else if (target != null && isCompatible(target)) {
            result = 0.8F;
        }

        return result;
    }

    @Override
    public <T> float score(Representation source, Class<T> target,
        Resource resource)
    {
        float result = -1.0F;

        if (source instanceof JacksonRepresentation<?>) {
            result = 1.0F;
        } else if ((target != null)
            && JacksonRepresentation.class.isAssignableFrom(target)) {
            result = 1.0F;
        } else if (isCompatible(source)) {
            result = 0.8F;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T toObject(Representation source, Class<T> target, Resource resource) throws IOException
    {
        Object result = null;

        // The source for the Jackson conversion
        JacksonRepresentation<?> jacksonSource = null;
        if (source instanceof JacksonRepresentation) {
            jacksonSource = (JacksonRepresentation<?>) source;
        } else if (isCompatible(source)) {
            jacksonSource = create(source, target);
        }

        if (jacksonSource != null) {
            // Handle the conversion
            if ((target != null)
                && JacksonRepresentation.class.isAssignableFrom(target)) {
                result = jacksonSource;
            } else {
                result = jacksonSource.getObject();
            }
        }

        return (T) result;
    }

    @Override
    public Representation toRepresentation(Object source, Variant target,
        Resource resource)
    {
        Representation result = null;

        if (source instanceof JacksonRepresentation) {
            result = (JacksonRepresentation<?>) source;
        } else {
            if (target.getMediaType() == null) {
                target.setMediaType(MediaType.APPLICATION_JSON);
            }
            if (isCompatible(target)) {
                result = create(target.getMediaType(), source);
            }
        }

        return result;
    }

    @Override
    public <T> void updatePreferences(List<Preference<MediaType>> preferences,
        Class<T> entity)
    {
        updatePreferences(preferences, MediaType.APPLICATION_JSON, 1.0F);
    }
}
