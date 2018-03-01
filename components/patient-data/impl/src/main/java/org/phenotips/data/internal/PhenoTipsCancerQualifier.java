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
package org.phenotips.data.internal;

import org.phenotips.data.CancerQualifier;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Implementation of patient data based on the XWiki data model, where cancer qualifier data is represented by
 * properties in objects of type {@code PhenoTips.CancerQualifierClass}.
 *
 * @version $Id$
 * @since 1.4
 */
public class PhenoTipsCancerQualifier extends AbstractPhenoTipsVocabularyProperty implements CancerQualifier
{
    /** All the permitted qualifier properties. */
    private static final CancerQualifierProperty[] PROPERTIES = CancerQualifierProperty.values();

    /** A map of JSON qualifier property name to property value. */
    private final Map<CancerQualifier.CancerQualifierProperty, Object> qualifierData;

    /** A logger for the property class. */
    private final Logger logger = LoggerFactory.getLogger(PhenoTipsCancerQualifier.class);

    /**
     * The constructor that takes the qualifier {@link BaseObject} as a parameter.
     *
     * @param qualifierObj the {@link BaseObject} that contains qualifier data
     * @throws IllegalArgumentException if no associated cancer is provided
     */
    PhenoTipsCancerQualifier(@Nonnull final BaseObject qualifierObj)
    {
        super((String) CancerQualifierProperty.CANCER.extractValue(qualifierObj));
        this.qualifierData = Arrays.stream(PROPERTIES)
            .collect(LinkedHashMap::new, (m, p) -> extractValueFromObj(qualifierObj, m, p), LinkedHashMap::putAll);
    }

    /**
     * The constructor that takes a {@link JSONObject} as a parameter.
     *
     * @param json the {@link JSONObject} that contains qualifier data
     * @throws IllegalArgumentException if no associated cancer is provided
     */
    PhenoTipsCancerQualifier(@Nonnull final JSONObject json)
    {
        super((String) CancerQualifierProperty.CANCER.extractValue(json));
        this.qualifierData = Arrays.stream(PROPERTIES)
            .collect(LinkedHashMap::new, (m, p) -> extractValueFromJson(json, m, p), LinkedHashMap::putAll);
    }

    /**
     * Writes cancer qualifier data to the provided {@code baseObject}.
     *
     * @param baseObject the {@link BaseObject} that will store cancer qualifier data
     * @param context the current {@link XWikiContext}
     */
    private void write(@Nonnull final BaseObject baseObject, @Nonnull final XWikiContext context)
    {
        this.qualifierData.forEach((property, value) -> property.writeValue(baseObject, value, context));
    }

    @Override
    public void write(@Nonnull final XWikiDocument doc, @Nonnull final XWikiContext context)
    {
        try {
            final BaseObject qualifierObj = doc.newXObject(CancerQualifier.CLASS_REFERENCE, context);
            write(qualifierObj, context);
        } catch (final XWikiException e) {
            this.logger.error("Failed to save qualifier for cancer: [{}]",
                StringUtils.defaultIfBlank(getId(), getName()));
        }
    }

    @Nullable
    @Override
    public Object getProperty(@Nonnull final CancerQualifierProperty property)
    {
        return this.qualifierData.get(property);
    }

    @Override
    @Nonnull
    public JSONObject toJSON()
    {
        return new JSONObject(this.qualifierData);
    }

    /**
     * Sets the {@code value} for {@code property} if the {@code value} is valid.
     *
     * @param property the {@link CancerQualifierProperty} of interest
     * @param value the value for the property
     */
    public void setProperty(@Nonnull final CancerQualifierProperty property, @Nullable final Object value)
    {
        // Cannot change the identifier.
        if (property != CancerQualifierProperty.CANCER && property.valueIsValid(value)) {
            if (value != null) {
                this.qualifierData.put(property, value);
            } else {
                this.qualifierData.remove(property);
            }
        }
    }

    /**
     * Extracts the property value from the provided {@code json}, and populates the {@code propertyMap} if there is a
     * value specified for the property.
     *
     * @param json the {@link JSONObject} that contains the values for qualifier properties
     * @param propertyMap the {@link Map} of qualifier properties to their values
     * @param property the {@link CancerQualifierProperty} property of interest
     */
    private void extractValueFromJson(@Nonnull final JSONObject json,
                                      @Nonnull final Map<CancerQualifier.CancerQualifierProperty, Object> propertyMap,
                                      @Nonnull final CancerQualifierProperty property)
    {
        final Object value = property.extractValue(json);
        if (value != null) {
            propertyMap.put(property, value);
        }
    }

    /**
     * Extracts the property value from the provided {@code qualifierObj}, and populates the {@code propertyMap} if
     * there is a value specified for the property.
     *
     * @param qualifierObj the {@link BaseObject} that contains the values for qualifier properties
     * @param propertyMap the {@link Map} of qualifier properties to their values
     * @param property the {@link CancerQualifierProperty} property of interest
     */
    private void extractValueFromObj(@Nonnull final BaseObject qualifierObj,
                                     @Nonnull final Map<CancerQualifier.CancerQualifierProperty, Object> propertyMap,
                                     @Nonnull final CancerQualifierProperty property)
    {
        final Object value = property.extractValue(qualifierObj);
        if (value != null) {
            propertyMap.put(property, value);
        }
    }

    @Override
    public boolean equals(@Nullable final Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PhenoTipsCancerQualifier that = (PhenoTipsCancerQualifier) o;

        return new EqualsBuilder()
            .append(this.qualifierData, that.qualifierData)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.qualifierData)
            .toHashCode();
    }
}
