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

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class for handling a collection of simple string values.
 *
 * @version $Id$
 * @since 1.0M10
 */
public abstract class AbstractSimpleController implements PatientDataController<String>
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            Map<String, String> result = new LinkedHashMap<>();
            for (String propertyName : getProperties()) {
                String value = data.getStringValue(propertyName);
                result.put(propertyName, value);
            }
            return new DictionaryPatientData<>(getName(), result);
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        save(patient, PatientWritePolicy.UPDATE);
    }

    @Override
    public void save(@Nonnull final Patient patient, @Nonnull final PatientWritePolicy policy)
    {
        try {
            final BaseObject xwikiDataObject = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE, true,
                contextProvider.get());
            final PatientData<String> data = patient.getData(getName());
            if (data == null) {
                // For replace policy, if no controller data is provided, everything that's stored should be removed.
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    getProperties().forEach(property -> saveFieldValue(xwikiDataObject, property, null));
                }
            } else {
                // Only start processing data if it has correct format.
                if (!data.isNamed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveControllerData(xwikiDataObject, data, policy);
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save controller data: {}", ex.getMessage(), ex);
        }


    }

    /**
     * Saves {@code data}, according to the provided {@code policy}. For any controller extending this class,
     * {@link PatientWritePolicy#UPDATE} and {@link PatientWritePolicy#MERGE} are equivalent.
     *
     * @param xwikiDataObject the XWiki {@link BaseObject data object}
     * @param data the {@link PatientData} object containing data that needs to be saved
     * @param policy the policy, according to which patient data should be saved
     */
    private void saveControllerData(
        @Nonnull final BaseObject xwikiDataObject,
        @Nonnull final PatientData<String> data,
        @Nonnull final PatientWritePolicy policy)
    {
        // The predicate, according to which fields will be filtered. For REPLACE policy, all fields should be altered,
        // whereas for MERGE and UPDATE, only the fields that have data should be changed.
        final Predicate<String> propertyFilter = PatientWritePolicy.REPLACE.equals(policy)
            ? property -> true
            : property -> containsProperty(data, property);
        // For each selected property, set values.
        getProperties().stream()
            .filter(propertyFilter)
            .forEach(property -> saveFieldValue(xwikiDataObject, property, getValueForProperty(data, property)));
    }

    /**
     * Sets the {@code value} for a {@code property} field in {@code xwikiDataObject}.
     *
     * @param xwikiDataObject the {@link BaseObject} where data will be saved
     * @param property the property of interest
     * @param value the value for the {@code property} of interest
     */
    void saveFieldValue(
        @Nonnull final BaseObject xwikiDataObject,
        @Nonnull final String property,
        @Nullable final String value)
    {
        xwikiDataObject.setStringValue(property, value);
    }

    /**
     * Gets the value for the provided {@code property}.
     *
     * @param data the {@link PatientData} to be saved
     * @param property the property of interest
     * @return the value for the {@code property} of interest, or null if no such value
     */
    String getValueForProperty(@Nonnull final PatientData<String> data, @Nonnull final String property)
    {
        return data.get(property);
    }

    /**
     * Returns true iff {@code data} contains the specified {@code property}.
     *
     * @param data the {@link PatientData} to save
     * @param property the property of interest
     * @return true iff {@code data} contains {@code property}
     */
    boolean containsProperty(@Nonnull final PatientData<String> data, @Nonnull final String property)
    {
        return data.containsKey(property);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        PatientData<String> data = patient.getData(getName());
        if (data == null || !data.isNamed()) {
            return;
        }

        Iterator<Entry<String, String>> dataIterator = data.dictionaryIterator();
        JSONObject container = json.optJSONObject(getJsonPropertyName());

        while (dataIterator.hasNext()) {
            Entry<String, String> datum = dataIterator.next();
            String key = datum.getKey();
            if (selectedFieldNames == null || selectedFieldNames.contains(key)) {
                if (container == null) {
                    // put() is placed here because we want to create the property iff at least one field is set/enabled
                    json.put(getJsonPropertyName(), new JSONObject());
                    container = json.optJSONObject(getJsonPropertyName());
                }
                container.put(key, datum.getValue());
            }
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        if (!json.has(this.getJsonPropertyName())) {
            // no data supported by this controller is present in provided JSON
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();

        // since the loader always returns dictionary data, this should always be a block.
        Object jsonBlockObject = json.get(this.getJsonPropertyName());
        if (!(jsonBlockObject instanceof JSONObject)) {
            return null;
        }
        JSONObject jsonBlock = (JSONObject) jsonBlockObject;
        for (String property : this.getProperties()) {
            if (jsonBlock.has(property)) {
                result.put(property, jsonBlock.getString(property));
            }
        }

        return new DictionaryPatientData<>(this.getName(), result);
    }

    protected abstract List<String> getProperties();

    protected abstract String getJsonPropertyName();
}
