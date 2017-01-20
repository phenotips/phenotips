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
package org.phenotips.data.internal.controller;

import org.phenotips.Constants;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;

/**
 * Handles the patient's external identifiers.
 *
 * @version $Id$
 * @since 1.3RC1
 */
@Component(roles = { PatientDataController.class })
@Named("labeled_eids")
@Singleton
public class LabeledExternalIdentifiersController extends AbstractComplexController<Map<String, String>>
{
    /** The XClass used for storing identifier data. */
    static final EntityReference IDENTIFIER_CLASS_REFERENCE = new EntityReference("LabeledIdentifierClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String IDENTIFIERS_STRING = "labeled_eids";

    private static final String CONTROLLER_NAME = IDENTIFIERS_STRING;

    private static final String INTERNAL_LABEL_KEY = "eidLabel";

    private static final String INTERNAL_VALUE_KEY = "eidValue";

    private static final List<String> INTERNAL_PROPERTIES_KEYS = ImmutableList.of(INTERNAL_LABEL_KEY,
        INTERNAL_VALUE_KEY);

    private static final String JSON_LABEL_KEY = "label";

    private static final String JSON_VALUE_KEY = "value";

    private static final List<String> JSON_PROPERTIES_KEYS = ImmutableList.of(JSON_LABEL_KEY,
        JSON_VALUE_KEY);

    private static final Map<String, String> JSON_TO_INTERNAL_PROPERTIES_MAP = ImmutableMap.of(
        JSON_LABEL_KEY, INTERNAL_LABEL_KEY,
        JSON_VALUE_KEY, INTERNAL_VALUE_KEY);

    @Inject
    private Logger logger;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected List<String> getBooleanFields()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getCodeFields()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getProperties()
    {
        return INTERNAL_PROPERTIES_KEYS;
    }

    @Override
    protected String getJsonPropertyName()
    {
        return IDENTIFIERS_STRING;
    }

    @Override
    public PatientData<Map<String, String>> load(final Patient patient)
    {
        try {
            final XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            final List<BaseObject> identifierClassXWikiObjects = doc.getXObjects(IDENTIFIER_CLASS_REFERENCE);

            if (CollectionUtils.isEmpty(identifierClassXWikiObjects)) {
                return null;
            }
            // Get data for all identifiers.
            final List<Map<String, String>> allIdentifiers = collectIdentifiers(identifierClassXWikiObjects);
            // Return as a patient data class, or null if no identifiers are recorded.
            return allIdentifiers.isEmpty() ? null : new IndexedPatientData<>(IDENTIFIERS_STRING, allIdentifiers);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen "
                + "error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    /**
     * Takes a list of {@link BaseObject} of {@code LabeledIdentifierClass} and returns a list of {@link Map}, where
     * each map contains properties for a {@code LabeledIdentifierClass}.
     *
     * @param identifierClassXWikiObjects the XWiki {@link BaseObject} of {@code LabeledIdentifierClass}
     * @return a list of {@link Map} of properties for each {@link BaseObject} of {@code LabeledIdentifierClass}
     */
    private List<Map<String, String>> collectIdentifiers(@Nonnull final List<BaseObject> identifierClassXWikiObjects)
    {
        final List<Map<String, String>> allIdentifiers = new ArrayList<>();
        for (final BaseObject identifierObject : identifierClassXWikiObjects) {
            if (!identifierIsEmpty(identifierObject)) {
                final Map<String, String> identifierProperties = collectIdentifierProperties(identifierObject);
                allIdentifiers.add(identifierProperties);
            }
        }
        return Collections.unmodifiableList(allIdentifiers);
    }

    /**
     * Takes a {@link BaseObject} of {@code LabeledIdentifierClass} and returns a {@link Map} of properties for the
     * given {@link BaseObject}.
     *
     * @param identifierObject the XWiki {@link BaseObject} of {@code LabeledIdentifierClass}
     */
    private Map<String, String> collectIdentifierProperties(@Nonnull final BaseObject identifierObject)
    {
        final Map<String, String> identifierProperties = new HashMap<>();
        for (final String property : INTERNAL_PROPERTIES_KEYS) {
            final String value = getFieldValue(identifierObject, property);
            if (StringUtils.isNotBlank(value)) {
                identifierProperties.put(property, value);
            }
        }
        return Collections.unmodifiableMap(identifierProperties);
    }

    /**
     * Takes a {@link BaseObject} of {@code LabeledIdentifierClass} and the name of some property of interest, and
     * returns the value for the provided property as string, or null if no such property exists.
     *
     * @param identifierObject the XWiki {@link BaseObject} of {@code LabeledIdentifierClass}
     * @param property the name of property of interest as string
     * @return the stored value for the given property, as string, null if no such property exists
     */
    private String getFieldValue(@Nonnull final BaseObject identifierObject, @Nonnull final String property)
    {
        final BaseStringProperty field = (BaseStringProperty) identifierObject.getField(property);
        return field == null ? null : field.getValue();
    }

    /**
     * Takes a {@link BaseObject} of {@code LabeledIdentifierClass} and returns true iff this object contains no data.
     *
     * @param identifierObject the XWiki {@link BaseObject} of {@code LabeledIdentifierClass}
     * @return true iff the {@link BaseObject} contains no data, false otherwise.
     */
    private boolean identifierIsEmpty(@Nullable final BaseObject identifierObject)
    {
        return identifierObject == null || identifierObject.getFieldList().isEmpty();
    }

    @Override
    public void writeJSON(@Nonnull final Patient patient, @Nonnull final JSONObject json,
        @Nullable final Collection<String> selectedFieldNames)
    {
        if (CollectionUtils.isNotEmpty(selectedFieldNames) && !selectedFieldNames.contains(IDENTIFIERS_STRING)) {
            return;
        }
        writeJSON(patient, json);
    }

    @Override
    public void writeJSON(@Nonnull final Patient patient, @Nonnull final JSONObject json)
    {
        final PatientData<Map<String, String>> identifiers = patient.getData(IDENTIFIERS_STRING);
        if (identifiers != null && identifiers.isIndexed() && identifiers.size() > 0) {
            json.put(getJsonPropertyName(), getIdentifiersAsJSONArray(identifiers));
        } else {
            json.put(getJsonPropertyName(), new JSONArray());
        }
    }

    /**
     * Takes a {@link PatientData} object containing a collection of {@link Map} for each identifier object stored in
     * patient and returns a {@link JSONArray} of the stored data.
     *
     * @param identifiers a {@link PatientData} object defining existing external identifiers for the patient
     * @return external identifiers data as {@link JSONArray}
     */
    private JSONArray getIdentifiersAsJSONArray(@Nonnull final PatientData<Map<String, String>> identifiers)
    {
        final JSONArray identifiersJSON = new JSONArray();
        final Iterator<Map<String, String>> iterator = identifiers.iterator();
        while (iterator.hasNext()) {
            final Map<String, String> identifier = iterator.next();
            if (StringUtils.isNotBlank(identifier.get(INTERNAL_LABEL_KEY))) {
                final JSONObject identifierJSON = getIdentifierJSON(identifier);
                identifiersJSON.put(identifierJSON);
            }
        }
        return identifiersJSON;
    }

    /**
     * Takes a {@link Map} containing identifier data, and returns this data as {@link JSONObject}.
     *
     * @param identifier a {@link Map} containing information for an external identifier
     * @return external identifier data as {@link JSONObject}
     */
    private JSONObject getIdentifierJSON(@Nonnull final Map<String, String> identifier)
    {
        final JSONObject identifierJSON = new JSONObject();
        // Retrieve data for properties that we want to store in json format.
        for (final String jsonKey : JSON_PROPERTIES_KEYS) {
            final String internalKey = JSON_TO_INTERNAL_PROPERTIES_MAP.get(jsonKey);
            if (identifier.get(internalKey) != null) {
                identifierJSON.put(jsonKey, identifier.get(internalKey));
            }
        }
        return identifierJSON;
    }

    @Override
    public PatientData<Map<String, String>> readJSON(@Nullable final JSONObject json)
    {
        return json == null || !json.has(getJsonPropertyName())
            ? null
            : buildIdentifiersData(json.optJSONArray(getJsonPropertyName()));
    }

    /**
     * Given a {@link JSONArray} containing all external identifiers for a patient, returns a {@link PatientData} class
     * with this data.
     *
     * @param identifiersJSON the {@link JSONArray} containing identifiers information
     * @return a {@link PatientData} class containing identifiers information
     */
    private PatientData<Map<String, String>> buildIdentifiersData(@Nullable final JSONArray identifiersJSON)
    {
        if (identifiersJSON == null) {
            return new IndexedPatientData<>(IDENTIFIERS_STRING, Collections.<Map<String, String>>emptyList());
        }

        final List<Map<String, String>> identifiers = new ArrayList<>();
        for (int i = 0; i < identifiersJSON.length(); i++) {
            final JSONObject identifierJSON = identifiersJSON.optJSONObject(i);
            if (isValidIdentifier(identifierJSON)) {
                identifiers.add(parseIdentifierProperties(identifierJSON));
            }
        }
        return new IndexedPatientData<>(IDENTIFIERS_STRING, Collections.unmodifiableList(identifiers));
    }

    /**
     * Given a {@link JSONObject} containing data for an external identifier, returns a {@link Map} with properties for
     * this external identifier.
     *
     * @param identifierJSON the {@link JSONObject} containing identifier information
     * @return a {@link Map} containing external identifier properties
     */
    private Map<String, String> parseIdentifierProperties(@Nonnull final JSONObject identifierJSON)
    {
        final Map<String, String> identifier = new HashMap<>();
        for (final String jsonKey : JSON_PROPERTIES_KEYS) {
            final String internalKey = JSON_TO_INTERNAL_PROPERTIES_MAP.get(jsonKey);
            if (identifierJSON.has(jsonKey) && identifierJSON.getString(jsonKey) != null) {
                identifier.put(internalKey, identifierJSON.getString(jsonKey));
            }
        }
        return Collections.unmodifiableMap(identifier);
    }

    /**
     * Returns true iff the {@link JSONObject} representation of the identifier is valid.
     *
     * @param identifierJSON the {@link JSONObject} containing identifier information
     * @return true iff the {@link JSONObject} representation of the identifier is valid
     */
    private boolean isValidIdentifier(@Nonnull final JSONObject identifierJSON)
    {
        // The identifier is valid if it has a non-empty label.
        return identifierJSON.has(JSON_LABEL_KEY) && StringUtils.isNotBlank(identifierJSON.getString(JSON_LABEL_KEY));
    }

    @Override
    public void save(@Nonnull final Patient patient, @Nullable final DocumentModelBridge doc)
    {
        final PatientData<Map<String, String>> identifiers = patient.getData(IDENTIFIERS_STRING);

        if (doc == null) {
            throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
        }

        if (identifiers != null && identifiers.isIndexed()) {
            ((XWikiDocument) doc).removeXObjects(IDENTIFIER_CLASS_REFERENCE);
            saveIdentifiers(identifiers, doc);
        }
    }

    /**
     * Saves the identifiers data specified in the {@link PatientData} class.
     *
     * @param identifiers a {@link PatientData} object defining existing external identifiers for the patient
     * @param doc the xWiki document for the patient
     */
    private void saveIdentifiers(@Nonnull final PatientData<Map<String, String>> identifiers,
        @Nonnull final DocumentModelBridge doc)
    {
        final Iterator<Map<String, String>> iterator = identifiers.iterator();
        while (iterator.hasNext()) {
            final Map<String, String> identifier = iterator.next();
            saveIdentifier(identifier, doc);
        }
    }

    /**
     * Saves the identifier data as specified in the {@link Map} of identifier properties.
     *
     * @param identifier a {@link Map} containing external identifier properties
     * @param doc the xWiki document for the patient
     */
    private void saveIdentifier(@Nonnull final Map<String, String> identifier, @Nonnull final DocumentModelBridge doc)
    {
        final XWikiContext context = this.xcontextProvider.get();
        try {
            final BaseObject xWikiObject = ((XWikiDocument) doc).newXObject(IDENTIFIER_CLASS_REFERENCE, context);
            saveIdentifierProperties(identifier, xWikiObject, context);
        } catch (final XWikiException ex) {
            this.logger.error("Failed to save a specific identifier: [{}]", ex.getMessage());
        }
    }

    /**
     * Saves the identifier properties specified in the {@link Map} to the provided {@link BaseObject}.
     *
     * @param identifier a {@link Map} containing external identifier properties
     * @param xWikiObject an xWiki {@link BaseObject} of {@code LabeledIdentifierClass} type
     * @param context the xWiki context
     */
    private void saveIdentifierProperties(@Nonnull final Map<String, String> identifier,
        @Nonnull final BaseObject xWikiObject, @Nonnull final XWikiContext context)
    {
        for (final String internalKey : INTERNAL_PROPERTIES_KEYS) {
            final String value = identifier.get(internalKey);
            if (value != null) {
                xWikiObject.set(internalKey, value, context);
            }
        }
    }
}
