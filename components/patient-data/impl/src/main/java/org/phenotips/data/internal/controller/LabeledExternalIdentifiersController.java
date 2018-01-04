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
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the patient's external identifiers.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = { PatientDataController.class })
@Named("labeled_eids")
@Singleton
public class LabeledExternalIdentifiersController implements PatientDataController<Pair<String, String>>
{
    /** The XClass used for storing identifier data. */
    static final EntityReference IDENTIFIER_CLASS_REFERENCE = new EntityReference("LabeledIdentifierClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String IDENTIFIERS_STRING = "labeled_eids";

    private static final String CONTROLLER_NAME = IDENTIFIERS_STRING;

    private static final String INTERNAL_LABEL_KEY = "label";

    private static final String INTERNAL_VALUE_KEY = "value";

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
    public PatientData<Pair<String, String>> load(final Patient patient)
    {
        try {
            final XWikiDocument doc = patient.getXDocument();
            final List<BaseObject> eidXWikiObjects = doc.getXObjects(IDENTIFIER_CLASS_REFERENCE);

            if (CollectionUtils.isEmpty(eidXWikiObjects)) {
                return null;
            }
            // Get data for all identifiers.
            final List<Pair<String, String>> allIdentifiers = toIdentifiers(eidXWikiObjects);
            // Return as a patient data class, or null if no identifiers are recorded.
            return allIdentifiers.isEmpty() ? null : new IndexedPatientData<>(CONTROLLER_NAME, allIdentifiers);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen "
                + "error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    /**
     * Takes a list of {@link BaseObject} of {@code LabeledIdentifierClass} and returns a list of {@link Pair}, where
     * each pair connects a label and its corresponding value.
     *
     * @param identifierClassXWikiObjects the XWiki {@link BaseObject} of {@code LabeledIdentifierClass}
     * @return a list of {@link Pair}s of identifiers for each non-empty {@link BaseObject} of
     *         {@code LabeledIdentifierClass}
     */
    private List<Pair<String, String>> toIdentifiers(@Nonnull final List<BaseObject> identifierClassXWikiObjects)
    {
        final List<Pair<String, String>> allIdentifiers = identifierClassXWikiObjects
            .stream()
            .filter(Objects::nonNull)
            .map(this::toIdentifier)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return Collections.unmodifiableList(allIdentifiers);
    }

    /**
     * Takes a {@link BaseObject} of {@code LabeledIdentifierClass} and returns a {@link Pair} connecting a label and
     * its corresponding value.
     *
     * @param identifierObject the XWiki {@link BaseObject} of {@code LabeledIdentifierClass}
     * @return a {@link Pair} with the label on the left (key) and the value on the right (value), either of which may
     *         be blank, but not both; if both the label and the value are empty, {@code null} is returned
     */
    private Pair<String, String> toIdentifier(@Nonnull final BaseObject identifierObject)
    {
        String label = identifierObject.getStringValue(INTERNAL_LABEL_KEY);
        String value = identifierObject.getStringValue(INTERNAL_VALUE_KEY);
        if (StringUtils.isAllBlank(label, value)) {
            return null;
        }
        return Pair.of(label, value);
    }

    @Override
    public void writeJSON(@Nonnull final Patient patient, @Nonnull final JSONObject json,
        @Nullable final Collection<String> selectedFieldNames)
    {
        if (CollectionUtils.isEmpty(selectedFieldNames) || selectedFieldNames.contains(IDENTIFIERS_STRING)) {
            writeJSON(patient, json);
        }
    }

    @Override
    public void writeJSON(@Nonnull final Patient patient, @Nonnull final JSONObject json)
    {
        final PatientData<Pair<String, String>> identifiers = patient.getData(IDENTIFIERS_STRING);
        final JSONArray eidsJSON = (identifiers != null && identifiers.isIndexed() && identifiers.size() > 0)
            ? toJSON(identifiers)
            : null;
        json.putOpt(IDENTIFIERS_STRING, eidsJSON);
    }

    /**
     * Takes a {@link PatientData} object containing a list of {@link Pair}s for each identifier object stored in
     * patient and returns a {@link JSONArray} of the stored data.
     *
     * @param identifiers a {@link PatientData} object defining existing external identifiers for the patient
     * @return external identifiers data as a {@link JSONArray}
     */
    private JSONArray toJSON(@Nonnull final PatientData<Pair<String, String>> identifiers)
    {
        final JSONArray identifiersJSON = new JSONArray();
        for (final Pair<String, String> identifier : identifiers) {
            final JSONObject identifierJSON = toJSON(identifier);
            if (identifierJSON != null) {
                identifiersJSON.put(identifierJSON);
            }
        }
        if (identifiersJSON.length() == 0) {
            return null;
        }
        return identifiersJSON;
    }

    /**
     * Takes a {@link Pair} containing identifier data, and returns this data as a {@link JSONObject}.
     *
     * @param identifier a {@link Pair} containing information for an external identifier
     * @return external identifier data as {@link JSONObject}
     */
    private JSONObject toJSON(@Nonnull final Pair<String, String> identifier)
    {
        if (StringUtils.isAllBlank(identifier.getKey(), identifier.getValue())) {
            return null;
        }
        final JSONObject identifierJSON = new JSONObject();
        identifierJSON.put(INTERNAL_LABEL_KEY, identifier.getKey());
        identifierJSON.put(INTERNAL_VALUE_KEY, identifier.getValue());
        return identifierJSON;
    }

    @Override
    public PatientData<Pair<String, String>> readJSON(@Nullable final JSONObject json)
    {
        if (json == null || !json.has(IDENTIFIERS_STRING)) {
            return null;
        }
        List<Pair<String, String>> identifiers = toIdentifiers(json.optJSONArray(IDENTIFIERS_STRING));
        if (identifiers == null || identifiers.isEmpty()) {
            return null;
        }
        return new IndexedPatientData<>(CONTROLLER_NAME, identifiers);
    }

    /**
     * Given a {@link JSONArray} containing all external identifiers for a patient, returns a list of {@link Pair}s,
     * where each pair connects a label and its corresponding value.
     *
     * @param identifiersJSON the {@link JSONArray} containing identifiers information
     * @return a list of {@link Pair}s of identifiers for each non-empty {@link JSONObject}
     */
    private List<Pair<String, String>> toIdentifiers(@Nullable final JSONArray identifiersJSON)
    {
        if (identifiersJSON == null) {
            return null;
        }

        final List<Pair<String, String>> identifiers =
            IntStream.range(0, identifiersJSON.length())
                .mapToObj(identifiersJSON::optJSONObject)
                .map(this::toIdentifier)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return Collections.unmodifiableList(identifiers);
    }

    /**
     * Given a {@link JSONObject} containing data for an external identifier, returns a {@link Pair} connecting a label
     * and its corresponding value.
     *
     * @param identifierJSON the {@link JSONObject} containing identifier information
     * @return a {@link Pair} with the label on the left (key) and the value on the right (value), either of which may
     *         be blank, but not both; if both the label and the value are empty, {@code null} is returned
     */
    private Pair<String, String> toIdentifier(@Nonnull final JSONObject identifierJSON)
    {
        String label = identifierJSON.optString(INTERNAL_LABEL_KEY);
        String value = identifierJSON.optString(INTERNAL_VALUE_KEY);
        if (StringUtils.isAllBlank(label, value)) {
            return null;
        }
        return Pair.of(label, value);
    }

    @Override
    public void save(@Nonnull final Patient patient)
    {
        save(patient, PatientWritePolicy.UPDATE);
    }

    @Override
    public void save(@Nonnull final Patient patient, @Nonnull final PatientWritePolicy policy)
    {
        try {
            final XWikiDocument doc = patient.getXDocument();
            final PatientData<Pair<String, String>> identifiers = patient.getData(IDENTIFIERS_STRING);
            if (identifiers == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    doc.removeXObjects(IDENTIFIER_CLASS_REFERENCE);
                }
            } else {
                if (!identifiers.isIndexed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveIdentifiers(doc, patient, identifiers, policy);
            }

        } catch (final Exception ex) {
            this.logger.error("Failed to save labeled external identifiers data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Saves the identifiers data specified in the {@link PatientData} class.
     *
     * @param doc the XWiki document for the patient
     * @param patient the patient object with data to be saved
     * @param identifiers a {@link PatientData} object defining existing external identifiers for the patient
     * @param doc the XWiki document for the patient
     */
    private void saveIdentifiers(
        @Nonnull final XWikiDocument doc,
        @Nonnull final Patient patient,
        @Nonnull final PatientData<Pair<String, String>> identifiers,
        @Nonnull final PatientWritePolicy policy)
    {
        if (PatientWritePolicy.MERGE.equals(policy)) {
            final List<Pair<String, String>> mergedEids = getMergedIdentifiers(load(patient), identifiers);
            doc.removeXObjects(IDENTIFIER_CLASS_REFERENCE);
            mergedEids.forEach(identifier -> saveIdentifier(identifier, doc));
        } else {
            doc.removeXObjects(IDENTIFIER_CLASS_REFERENCE);
            identifiers.forEach(identifier -> saveIdentifier(identifier, doc));
        }
    }

    /**
     * Create a list of labeled external identifiers that merges existing and updated external identifier data.
     *
     * @param storedEids the external labeled identifiers stored in patient
     * @param eids the external labeled identifiers to add to patient
     * @return a list of {@link Pair}s of identifiers
     */
    private List<Pair<String, String>> getMergedIdentifiers(
        @Nullable final PatientData<Pair<String, String>> storedEids,
        @Nonnull final PatientData<Pair<String, String>> eids)
    {
        return new ArrayList<>(Stream.of(storedEids, eids)
            .filter(Objects::nonNull)
            .flatMap(s -> StreamSupport.stream(s.spliterator(), false))
            .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * Saves the identifier data as specified in the {@link Pair} of identifier properties.
     *
     * @param identifier a {@link Pair} containing information for an external identifier
     * @param doc the XWiki document for the patient
     */
    private void saveIdentifier(@Nonnull final Pair<String, String> identifier, @Nonnull final XWikiDocument doc)
    {
        final XWikiContext context = this.xcontextProvider.get();
        try {
            final BaseObject xobject = doc.newXObject(IDENTIFIER_CLASS_REFERENCE, context);
            saveIdentifierProperties(identifier, xobject, context);
        } catch (final XWikiException ex) {
            this.logger.error("Failed to save a specific identifier: [{}]", ex.getMessage());
        }
    }

    /**
     * Saves the identifier properties specified in the {@link Pair} to the provided {@link BaseObject}.
     *
     * @param identifier a {@link Pair} containing information for an external identifier
     * @param xobject an XWiki {@link BaseObject} of {@code LabeledIdentifierClass} type
     * @param context the XWiki context
     */
    private void saveIdentifierProperties(@Nonnull final Pair<String, String> identifier,
        @Nonnull final BaseObject xobject, @Nonnull final XWikiContext context)
    {
        final String label = identifier.getKey();
        if (label != null) {
            xobject.set(INTERNAL_LABEL_KEY, label, context);
        }
        final String value = identifier.getValue();
        if (value != null) {
            xobject.set(INTERNAL_VALUE_KEY, value, context);
        }
    }
}
