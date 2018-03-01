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

import org.phenotips.data.Cancer;
import org.phenotips.data.CancerQualifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Implementation of patient data based on the XWiki data model, where cancer data is represented by properties in
 * objects of type {@code PhenoTips.CancerClass}.
 *
 * @version $Id$
 * @since 1.4
 */
public class PhenoTipsCancer extends AbstractPhenoTipsVocabularyProperty implements Cancer
{
    /** The qualifiers JSON key. */
    private static final String QUALIFIERS_KEY = "qualifiers";

    /** An array of properties recorded for this cancer. */
    private static final CancerProperty[] PROPERTIES = CancerProperty.values();

    /** A map of JSON cancer property name to property value. */
    private final Map<Cancer.CancerProperty, Object> cancerData;

    /** A set of qualifier objects for this cancer. */
    private Set<CancerQualifier> qualifiers;

    /** A logger for the property class. */
    private final Logger logger = LoggerFactory.getLogger(PhenoTipsCancer.class);

    /**
     * Constructor that copies the data from a {@code cancerObject}.
     *
     * @param doc the {@link XWikiDocument} where the data is stored
     * @param cancerObject the cancer {@link BaseObject}
     * @throws IllegalArgumentException if the cancer ID is null
     */
    public PhenoTipsCancer(@Nonnull final XWikiDocument doc, @Nonnull final BaseObject cancerObject)
    {
        super((String) CancerProperty.CANCER.extractValue(cancerObject));
        this.cancerData = Arrays.stream(PROPERTIES)
            .collect(LinkedHashMap::new, (m, p) -> extractValueFromObj(cancerObject, m, p), LinkedHashMap::putAll);
        this.qualifiers = extractQualifiersFromDoc(doc);
    }

    /**
     * Constructor that copies the data from a {@code json}.
     *
     * @param json the cancer {@link JSONObject}
     * @throws IllegalArgumentException if the cancer ID is null
     */
    public PhenoTipsCancer(@Nonnull final JSONObject json)
    {
        super((String) CancerProperty.CANCER.extractValue(json));
        this.cancerData = Arrays.stream(PROPERTIES)
            .collect(LinkedHashMap::new, (m, p) -> extractValueFromJson(json, m, p), LinkedHashMap::putAll);
        this.qualifiers = extractQualifiersFromJson(json);
    }

    @Override
    public boolean isAffected()
    {
        return (Boolean) this.cancerData.getOrDefault(CancerProperty.AFFECTED, false);
    }

    @Override
    @Nonnull
    public Collection<CancerQualifier> getQualifiers()
    {
        return Collections.unmodifiableSet(this.qualifiers);
    }

    @Nullable
    @Override
    public Object getProperty(@Nonnull final Cancer.CancerProperty property)
    {
        return this.cancerData.get(property);
    }

    @Override
    @Nonnull
    public JSONObject toJSON()
    {
        final JSONArray qualifiersArray = new JSONArray();
        this.qualifiers.forEach(qualifier -> qualifiersArray.put(qualifier.toJSON()));
        return new JSONObject(this.cancerData).put(NAME_JSON_KEY_NAME, getName()).put(QUALIFIERS_KEY, qualifiersArray);
    }

    @Nonnull
    @Override
    public Cancer mergeData(@Nonnull final Cancer cancer)
    {
        if (!isSameCancer(cancer)) {
            throw new IllegalArgumentException("Cannot merge cancer objects with different identifiers");
        }
        Arrays.asList(PROPERTIES).forEach(property -> setProperty(property, cancer.getProperty(property)));
        // There is no good way to differentiate between qualifiers, so just add all new qualifiers to collection.
        addQualifiers(cancer.getQualifiers());
        return this;
    }

    /**
     * Writes cancer data to {@code baseObject}.
     *
     * @param baseObject the {@link BaseObject} that will store cancer data
     * @param context the current {@link XWikiContext}
     */
    private void write(@Nonnull final BaseObject baseObject, final @Nonnull XWikiContext context)
    {
        this.cancerData.forEach((property, value) -> property.writeValue(baseObject, value, context));
    }

    @Override
    public void write(@Nonnull final XWikiDocument doc, @Nonnull final XWikiContext context)
    {
        try {
            final BaseObject cancerObject = doc.newXObject(Cancer.CLASS_REFERENCE, context);
            write(cancerObject, context);
            getQualifiers().forEach(qualifier -> qualifier.write(doc, context));
        } catch (final XWikiException e) {
            this.logger.error("Failed to save qualifier for cancer: [{}]",
                StringUtils.defaultIfBlank(getId(), getName()));
        }
    }

    /**
     * Sets the {@code qualifiers} for the cancer.
     *
     * @param qualifiers the collection of {@link CancerQualifier} objects associated with the current cancer
     */
    public void setQualifiers(@Nullable final Collection<CancerQualifier> qualifiers)
    {
        this.qualifiers = CollectionUtils.isNotEmpty(qualifiers)
            ? qualifiers.stream().filter(Objects::nonNull).collect(Collectors.toSet())
            : new HashSet<>();
    }

    /**
     * Adds specified {@code qualifiers} to the existing set of {@link #getQualifiers()}.
     *
     * @param qualifiers the collection of {@link CancerQualifier} objects to add to the existing list
     */
    public void addQualifiers(@Nullable final Collection<CancerQualifier> qualifiers)
    {
        if (CollectionUtils.isNotEmpty(qualifiers)) {
            qualifiers.forEach(this::addQualifier);
        }
    }

    /**
     * Adds a specified {@code qualifier} to the existing set of {@link #getQualifiers()}.
     *
     * @param qualifier the {@link CancerQualifier} to add to the existing list
     */
    public void addQualifier(@Nullable final CancerQualifier qualifier)
    {
        CollectionUtils.addIgnoreNull(this.qualifiers, qualifier);
    }

    /**
     * Sets the {@code value} for {@code property} if the {@code value} is valid.
     *
     * @param property the {@link CancerQualifier.CancerQualifierProperty} of interest
     * @param value the value for the property
     */
    public void setProperty(@Nonnull final Cancer.CancerProperty property, @Nullable final Object value)
    {
        // Should not be able to reset the identifier.
        if (property != CancerProperty.CANCER && property.valueIsValid(value)) {
            if (value != null) {
                this.cancerData.put(property, value);
            } else {
                this.cancerData.remove(property);
            }
        }
    }

    /**
     * Extracts the property value from the provided {@code json}, and populates the {@code propertyMap} if there is a
     * value specified for the property.
     *
     * @param json the {@link JSONObject} that contains the values for cancer properties
     * @param propertyMap the {@link Map} of cancer properties to their values
     * @param property the {@link Cancer.CancerProperty} property of interest
     */
    private void extractValueFromJson(@Nonnull final JSONObject json,
                                      @Nonnull final Map<Cancer.CancerProperty, Object> propertyMap,
                                      @Nonnull final Cancer.CancerProperty property)
    {
        final Object value = property.extractValue(json);
        if (value != null) {
            propertyMap.put(property, value);
        }
    }

    /**
     * Extracts the property value from the provided {@code cancerObj}, and populates the {@code propertyMap} if
     * there is a value specified for the property.
     *
     * @param cancerObj the {@link BaseObject} that contains the values for cancer properties
     * @param propertyMap the {@link Map} of cancer properties to their values
     * @param property the {@link Cancer.CancerProperty} property of interest
     */
    private void extractValueFromObj(@Nonnull final BaseObject cancerObj,
                                     @Nonnull final Map<Cancer.CancerProperty, Object> propertyMap,
                                     @Nonnull final Cancer.CancerProperty property)
    {
        final Object value = property.extractValue(cancerObj);
        if (value != null) {
            propertyMap.put(property, value);
        }
    }

    /**
     * Extracts cancer qualifiers from the provided {@code json}.
     *
     * @param json the {@link JSONObject} that contains cancer data
     * @return a set of {@link CancerQualifier} objects associated with the current cancer
     * @throws org.json.JSONException if qualifiers JSON have invalid format
     */
    @Nonnull
    private Set<CancerQualifier> extractQualifiersFromJson(@Nonnull final JSONObject json)
    {
        final JSONArray qualifiersArray = json.optJSONArray(QUALIFIERS_KEY);
        return qualifiersArray == null || qualifiersArray.length() == 0
            ? new HashSet<>()
            : IntStream.range(0, qualifiersArray.length())
                .mapToObj(qualifiersArray::getJSONObject)
                .map(this::wrapQualifier)
                .collect(Collectors.toSet());
    }

    /**
     * Extracts cancer qualifiers from the provided {@code doc}.
     *
     * @param doc the current {@link XWikiDocument}
     * @return a set of {@link CancerQualifier} objects associated with the current cancer
     */
    @Nonnull
    private Set<CancerQualifier> extractQualifiersFromDoc(@Nonnull final XWikiDocument doc)
    {
        final List<BaseObject> qualifierXWikiObjects = doc.getXObjects(CancerQualifier.CLASS_REFERENCE);
        return qualifierXWikiObjects.stream()
            .filter(Objects::nonNull)
            .filter(qualifierObj -> !qualifierObj.getFieldList().isEmpty())
            .filter(this::hasRightId)
            .map(PhenoTipsCancerQualifier::new)
            .collect(Collectors.toSet());
    }

    /**
     * Returns true iff the {@code qualifierObj} is associated with the current cancer.
     *
     * @param qualifierObj the qualifier object being inspected
     * @return true iff the qualifier object is for this cancer
     */
    private boolean hasRightId(@Nonnull final BaseObject qualifierObj)
    {
        final String property = CancerQualifier.CancerQualifierProperty.CANCER.getProperty();
        return StringUtils.equals(StringUtils.defaultIfBlank(getId(), getName()),
            qualifierObj.getStringValue(property));
    }

    /**
     * Returns true iff {@code cancer} and this cancer are the same.
     *
     * @param cancer the {@link Cancer} object being compared to this
     * @return true iff the two objects are the same
     */
    private boolean isSameCancer(@Nonnull final Cancer cancer)
    {
        final String thisId = getId();
        return StringUtils.isNotBlank(thisId)
                ? Objects.equals(thisId, cancer.getId())
                : Objects.equals(getName(), cancer.getName());
    }

    /**
     * Wraps the qualifier JSON data associated with this cancer in a {@link CancerQualifier} object.
     *
     * @param qualifierJson the JSON for a qualifier associated with this cancer
     * @return a {@link CancerQualifier} object containing the JSON data
     */
    @Nonnull
    private CancerQualifier wrapQualifier(@Nonnull final JSONObject qualifierJson)
    {
        if (!qualifierJson.has(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty())) {
            qualifierJson.put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(),
                StringUtils.defaultIfBlank(getId(), getName()));
        }
        return new PhenoTipsCancerQualifier(qualifierJson);
    }
}
