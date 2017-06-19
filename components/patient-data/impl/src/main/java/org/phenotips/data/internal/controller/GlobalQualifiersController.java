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
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Handles the patient's global qualifiers, such as global age of onset.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("global-qualifiers")
@Singleton
public class GlobalQualifiersController implements PatientDataController<List<VocabularyTerm>>
{
    private static final String DATA_NAME = "global-qualifiers";

    private static final String ID_NAME = "id";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private VocabularyManager vocabularyManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public PatientData<List<VocabularyTerm>> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            Map<String, List<VocabularyTerm>> result = new LinkedHashMap<>();
            for (String propertyName : getProperties()) {
                PropertyInterface propertyValue = data.get(propertyName);
                List<VocabularyTerm> holder = new LinkedList<>();
                if (propertyValue instanceof StringProperty) {
                    String propertyValueString = ((StringProperty) propertyValue).getValue();
                    addTerms(propertyValueString, holder);
                } else if (propertyValue instanceof ListProperty) {
                    for (String item : ((ListProperty) propertyValue).getList()) {
                        addTerms(item, holder);
                    }
                }
                result.put(propertyName, holder);
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
            final XWikiContext context = this.xcontextProvider.get();
            final BaseObject xobject = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE, true, context);
            final PatientData<List<VocabularyTerm>> data = patient.getData(getName());
            if (data == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    getProperties().forEach(p -> xobject.set(p, null, context));
                }
            } else {
                if (!data.isNamed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveQualifiersData(patient, xobject, data, policy, context);
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save global qualifiers data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Saves the provided qualifiers {@code data}.
     *
     * @param patient the {@link Patient} object being modified
     * @param xobject the {@link BaseObject}
     * @param data the new {@link PatientData} to store
     * @param policy the {@link PatientWritePolicy} according to which data will be saved
     * @param context the {@link XWikiContext}
     */
    private void saveQualifiersData(
        @Nonnull final Patient patient,
        @Nonnull final BaseObject xobject,
        @Nonnull final PatientData<List<VocabularyTerm>> data,
        @Nonnull final PatientWritePolicy policy,
        @Nonnull final XWikiContext context)
    {
        final BaseClass xclass = xobject.getXClass(context);
        final Predicate<String> propertyFilter;
        final PatientData<List<VocabularyTerm>> storedData;
        if (PatientWritePolicy.MERGE.equals(policy)) {
            storedData = load(patient);
            propertyFilter = data::containsKey;
        } else {
            storedData = null;
            propertyFilter = PatientWritePolicy.REPLACE.equals(policy) ? p -> true : data::containsKey;
        }
        getProperties().stream()
                .filter(propertyFilter)
                .forEach(property -> savePropertyData(property, xobject, xclass, data, storedData, context));
    }

    /**
     * Saves the {@code data} for a specific {@code property}.
     *
     * @param property the property of interest
     * @param xobject the {@link BaseObject}
     * @param xclass the {@link BaseClass}
     * @param data the new {@link PatientData} to store
     * @param storedData the {@link PatientData} already stored in patient
     * @param context the {@link XWikiContext}
     */
    private void savePropertyData(
        @Nonnull final String property,
        @Nonnull final BaseObject xobject,
        @Nonnull final BaseClass xclass,
        @Nonnull final PatientData<List<VocabularyTerm>> data,
        @Nullable final PatientData<List<VocabularyTerm>> storedData,
        @Nonnull final XWikiContext context)
    {
        final PropertyClass xpropertyClass = (PropertyClass) xclass.get(property);
        final List<VocabularyTerm> terms = data.get(property);
        if (xpropertyClass != null) {
            final PropertyInterface xproperty = xpropertyClass.newProperty();
            if (xproperty instanceof BaseStringProperty) {
                saveSingleValueData(property, xobject, terms, context);
            } else if (xproperty instanceof ListProperty) {
                final List<VocabularyTerm> storedTerms = (storedData != null) ? storedData.get(property) : null;
                saveListData(property, xobject, terms, storedTerms, context);
            }
        }
    }

    /**
     * Saves the {@code property} with a single value.
     *
     * @param property the property of interest
     * @param xobject the {@link BaseObject}
     * @param terms the list of {@link VocabularyTerm} objects specified for the property; should contain one term
     * @param context the {@link XWikiContext}
     */
    private void saveSingleValueData(
        @Nonnull final String property,
        @Nonnull final BaseObject xobject,
        @Nullable final List<VocabularyTerm> terms,
        @Nonnull final XWikiContext context)
    {
        // If no terms provided, set to null. Otherwise, convert to a list of term IDs (should only have one ID) and
        // extract the first ID.
        final String value = (terms == null || terms.isEmpty())
            ? null
            : terms.stream().map(VocabularyTerm::getId).collect(Collectors.toList()).get(0);
        xobject.set(property, value, context);
    }

    /**
     * Saves the {@code property} with a list for value.
     *
     * @param property the property of interest
     * @param xobject the {@link BaseObject}
     * @param terms list of {@link VocabularyTerm} objects specified for the property
     * @param storedTerms list of {@link VocabularyTerm} objects stored in patient; set to null if not relevant
     * @param context the {@link XWikiContext}
     */
    private void saveListData(
        @Nonnull final String property,
        @Nonnull final BaseObject xobject,
        @Nullable final List<VocabularyTerm> terms,
        @Nullable final List<VocabularyTerm> storedTerms,
        @Nonnull final XWikiContext context)
    {
        // A list of merged identifiers.
        final List<String> value = buildMergedQualifiersList(storedTerms, terms);
        xobject.set(property, value, context);
    }

    /**
     * Returns a list of global qualifiers, merging {@code stored qualifiers}, if any, and {@code qualifers}.
     *
     * @param stored {@link PatientData} already stored in patient
     * @param qualifiers {@link PatientData} to save for patient
     * @return a merged list of global qualifiers
     */
    private List<String> buildMergedQualifiersList(
        @Nullable final List<VocabularyTerm> stored,
        @Nullable final List<VocabularyTerm> qualifiers)
    {
        // If there are no qualifiers stored, then just return a list of new qualifier ids.
        if (CollectionUtils.isEmpty(stored)) {
            return CollectionUtils.isNotEmpty(qualifiers)
                ? qualifiers.stream()
                    .map(VocabularyTerm::getId)
                    .collect(Collectors.toList())
                : null;
        }
        // There are some stored qualifiers, merge them.
        final Set<String> qualifierValues = Stream.of(stored, qualifiers)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .map(VocabularyTerm::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return qualifierValues.isEmpty() ? null : new ArrayList<>(qualifierValues);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        PatientData<List<VocabularyTerm>> data = patient.getData(DATA_NAME);
        if (data == null) {
            return;
        }
        Iterator<Entry<String, List<VocabularyTerm>>> iterator = data.dictionaryIterator();
        while (iterator.hasNext()) {
            Entry<String, List<VocabularyTerm>> datum = iterator.next();
            if (selectedFieldNames == null || selectedFieldNames.contains(datum.getKey())) {
                List<VocabularyTerm> terms = datum.getValue();
                if (terms == null || terms.isEmpty()) {
                    if (selectedFieldNames != null && selectedFieldNames.contains(datum.getKey())) {
                        json.put(datum.getKey(), new JSONArray());
                    }
                    continue;
                }
                JSONArray elements = new JSONArray();
                for (VocabularyTerm term : terms) {
                    JSONObject element = new JSONObject();
                    element.put(ID_NAME, term.getId());
                    element.put("label", term.getName());
                    elements.put(element);
                }
                json.put(datum.getKey(), elements);
            }
        }
    }

    @Override
    public PatientData<List<VocabularyTerm>> readJSON(JSONObject json)
    {
        try {
            Map<String, List<VocabularyTerm>> result = new HashMap<>();
            for (String property : this.getProperties()) {
                JSONArray elements = json.optJSONArray(property);
                if (elements != null) {
                    List<VocabularyTerm> propertyTerms = new LinkedList<>();
                    Iterator<Object> elementsIterator = elements.iterator();
                    while (elementsIterator.hasNext()) {
                        JSONObject element = (JSONObject) elementsIterator.next();
                        String termId = element.optString(ID_NAME);
                        if (termId != null) {
                            VocabularyTerm term = this.vocabularyManager.resolveTerm(termId);
                            propertyTerms.add(term);
                        }
                    }
                    result.put(property, propertyTerms);
                } else {
                    result.put(property, null);
                }
            }
            return new DictionaryPatientData<>(DATA_NAME, result);
        } catch (Exception ex) {
            // must be in a wrong format
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
        return Arrays.asList("global_age_of_onset", "global_mode_of_inheritance");
    }

    private void addTerms(String item, List<VocabularyTerm> holder)
    {
        if (StringUtils.isNotBlank(item)) {
            VocabularyTerm term = this.vocabularyManager.resolveTerm(item);
            if (term != null) {
                holder.add(term);
            }
        }
    }
}
