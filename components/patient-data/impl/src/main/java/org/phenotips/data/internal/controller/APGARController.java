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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ObjectPropertyReference;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Handles the two APGAR scores.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component(roles = { PatientDataController.class })
@Named("apgar")
@Singleton
public class APGARController implements PatientDataController<Integer>
{
    /** The name of this data. */
    private static final String DATA_NAME = "apgar";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontext;

    @Override
    public PatientData<Integer> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            Map<String, Integer> result = new LinkedHashMap<>();
            for (String propertyName : getProperties()) {
                String value = data.getStringValue(propertyName);
                if (NumberUtils.isDigits(value)) {
                    result.put(propertyName, Integer.valueOf(value));
                }
            }
            return new DictionaryPatientData<>(DATA_NAME, result);
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
            final BaseObject dataHolder = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE, true,
                this.xcontext.get());
            final PatientData<Integer> data = patient.getData(getName());
            if (data == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    getProperties().forEach(property -> writePropertyData(dataHolder, property, null));
                }
            } else {
                if (!data.isNamed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                writeAPGARData(dataHolder, data, policy);
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save apgar data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Writes the {@code data} to {@code dataHolder} according to the specified {@code policy}.
     *
     * @param dataHolder the {@link BaseObject}
     * @param data a {@link PatientData} object containing APGAR information.
     * @param policy the selected {@link PatientWritePolicy}
     */
    private void writeAPGARData(
        @Nonnull final BaseObject dataHolder,
        @Nonnull final PatientData<Integer> data,
        @Nonnull final PatientWritePolicy policy)
    {
        final Predicate<String> propertyFilter = PatientWritePolicy.REPLACE.equals(policy)
            ? property -> true
            : data::containsKey;

        getProperties().stream()
            .filter(propertyFilter)
            .forEach(property -> writePropertyData(dataHolder, property, data.get(property)));
    }

    /**
     * Writes a {@code value} for a {@code property} to the {@code dataHolder}.
     *
     * @param dataHolder the {@link BaseObject} where data will be written
     * @param property the property of interest
     * @param value the value for {@code property} as {@link Integer}
     */
    private void writePropertyData(
        @Nonnull final BaseObject dataHolder,
        @Nonnull final String property,
        @Nullable final Integer value)
    {
        final String valueStr = value != null ? value.toString() : "unknown";
        @SuppressWarnings("unchecked")
        final BaseProperty<ObjectPropertyReference> field =
            (BaseProperty<ObjectPropertyReference>) dataHolder.getField(property);
        field.setValue(valueStr);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !hasAnySelected(selectedFieldNames)) {
            return;
        }
        PatientData<Integer> data = patient.getData(getName());
        if (data == null || !data.isNamed()) {
            if (selectedFieldNames != null && hasAnySelected(selectedFieldNames)) {
                json.put(DATA_NAME, new JSONObject());
            }
            return;
        }

        JSONObject container = json.optJSONObject(DATA_NAME);
        if (container == null) {
            json.put(DATA_NAME, new JSONObject());
            container = json.optJSONObject(DATA_NAME);
        }

        Iterator<Entry<String, Integer>> iterator = data.dictionaryIterator();
        if (!iterator.hasNext()) {
            return;
        }
        while (iterator.hasNext()) {
            Entry<String, Integer> item = iterator.next();
            container.put(item.getKey(), item.getValue());
        }
    }

    /**
     * Checks if any relevant field names were selected.
     *
     * @return true if relevant fields were selected, false otherwise
     */
    private boolean hasAnySelected(Collection<String> selectedFieldNames)
    {
        return selectedFieldNames.stream()
            .anyMatch(selectedFieldName -> StringUtils.startsWithIgnoreCase(selectedFieldName, getName()));
    }

    @Override
    public PatientData<Integer> readJSON(JSONObject json)
    {
        JSONObject container = json.optJSONObject(DATA_NAME);
        if (container != null) {
            Map<String, Integer> parsed = new LinkedHashMap<>();
            for (String propertyName : getProperties()) {
                try {
                    /* could be 'unknown' rather than an int */
                    String value = container.optString(propertyName);
                    if (NumberUtils.isDigits(value)) {
                        parsed.put(propertyName, Integer.valueOf(value));
                    }
                    if (StringUtils.isEmpty(value)) {
                        parsed.put(propertyName, null);
                    }
                } catch (Exception ex) {
                    // should never happen
                }
            }
            return new DictionaryPatientData<>(DATA_NAME, parsed);
        }
        return null;
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    protected List<String> getProperties()
    {
        List<String> list = new LinkedList<>();
        list.add("apgar1");
        list.add("apgar5");
        return list;
    }
}
