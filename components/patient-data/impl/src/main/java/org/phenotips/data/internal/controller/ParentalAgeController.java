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
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the parent's age at the estimated date of delivery.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component(roles = { PatientDataController.class })
@Named("parentalAge")
@Singleton
public class ParentalAgeController implements PatientDataController<Integer>
{
    /** The XClass used for storing parental information. */
    public static final EntityReference CLASS_REFERENCE =
        new EntityReference("ParentalInformationClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String MATERNAL_AGE = "maternal_age";

    private static final String PATERNAL_AGE = "paternal_age";

    private static final String ENABLING_FIELD_NAME = "parentalAge";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** The current request context, needed when working with the XWiki old core. */
    @Inject
    private Provider<XWikiContext> xcontext;

    @Override
    public String getName()
    {
        return "parentalAge";
    }

    @Override
    public PatientData<Integer> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(getXClassReference());
            if (data == null) {
                this.logger.debug("No parental information for patient [{}]", patient.getId());
                return null;
            }
            Map<String, Integer> result = new LinkedHashMap<>();
            for (String property : getProperties()) {
                int age = data.getIntValue(property, Integer.MIN_VALUE);
                if (age != Integer.MIN_VALUE) {
                    result.put(property, age);
                }
            }
            if (!result.isEmpty()) {
                return new DictionaryPatientData<>(getName(), result);
            }
        } catch (Exception ex) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, ex.getMessage());
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
            final XWikiContext context = this.xcontext.get();
            final BaseObject dataHolder = patient.getXDocument().getXObject(getXClassReference(), true, context);
            final PatientData<Integer> data = patient.getData(getName());
            if (data == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    getProperties().forEach(property -> dataHolder.set(property, null, context));
                }
            } else {
                if (!data.isNamed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveParentalAgeData(dataHolder, data, policy, context);
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save parental age data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Saves the provided parental age {@code data} into the {@code dataHolder}, according to the provided
     * {@code policy}.
     *
     * @param dataHolder the {@link BaseObject} that will hold the data
     * @param data the parental age data {@link PatientData} object containing new data
     * @param policy the {@link PatientWritePolicy} according to which patient data will be saved
     * @param context the {@link XWikiContext} object
     */
    private void saveParentalAgeData(
        @Nonnull final BaseObject dataHolder,
        @Nonnull final PatientData<Integer> data,
        @Nonnull final PatientWritePolicy policy,
        @Nonnull final XWikiContext context)
    {
        final Predicate<String> propertyFilter = PatientWritePolicy.REPLACE.equals(policy)
            ? property -> true
            : data::containsKey;

        getProperties().stream()
            .filter(propertyFilter)
            .forEach(property -> dataHolder.set(property, data.get(property), context));
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(ENABLING_FIELD_NAME)) {
            return;
        }

        PatientData<Integer> data = patient.getData(getName());
        if (data == null || !data.isNamed() || data.size() == 0) {
            if (selectedFieldNames != null && selectedFieldNames.contains(ENABLING_FIELD_NAME)) {
                json.put(getJsonPropertyName(), new JSONObject());
            }
            return;
        }

        JSONObject container = json.optJSONObject(getJsonPropertyName());
        if (container == null) {
            container = new JSONObject();
        }

        for (String propertyName : this.getProperties()) {
            if (data.get(propertyName) != null) {
                container.put(propertyName, data.get(propertyName));
            }
        }

        if (container.length() > 0) {
            json.put(getJsonPropertyName(), container);
        }
    }

    @Override
    public PatientData<Integer> readJSON(JSONObject json)
    {
        if (json == null || json.length() == 0) {
            return null;
        }
        JSONObject data = json.optJSONObject(getJsonPropertyName());
        if (data == null || data.length() == 0) {
            return null;
        }
        Map<String, Integer> result = new LinkedHashMap<>();

        for (String property : getProperties()) {
            if (data.has(property)) {
                int age = data.optInt(property, Integer.MIN_VALUE);
                if (age != Integer.MIN_VALUE) {
                    result.put(property, age);
                }
            }
        }
        return new DictionaryPatientData<>(getName(), result);
    }

    protected EntityReference getXClassReference()
    {
        return CLASS_REFERENCE;
    }

    protected List<String> getProperties()
    {
        return Arrays.asList(MATERNAL_AGE, PATERNAL_AGE);
    }

    private String getJsonPropertyName()
    {
        return "prenatal_perinatal_history";
    }
}
