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
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ObjectPropertyReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.StringProperty;

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

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private VocabularyManager vocabularyManager;

    @Override
    public PatientData<List<VocabularyTerm>> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            Map<String, List<VocabularyTerm>> result = new LinkedHashMap<>();
            for (String propertyName : getProperties()) {
                Object propertyValue = data.get(propertyName);
                List<VocabularyTerm> holder = new LinkedList<>();
                if (propertyValue instanceof StringProperty) {
                    String propertyValueString = data.getStringValue(propertyName);
                    addTerms(propertyValueString, holder);
                } else if (propertyValue instanceof DBStringListProperty) {
                    for (String item : ((DBStringListProperty) propertyValue).getList()) {
                        addTerms(item, holder);
                    }
                }
                result.put(propertyName, holder);
            }

            return new DictionaryPatientData<>(DATA_NAME, result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        try {
            PatientData<List<VocabularyTerm>> data = patient.getData(this.getName());
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject dataHolder = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null || dataHolder == null) {
                return;
            }
            for (String propertyName : getProperties()) {
                List<VocabularyTerm> terms = data.get(propertyName);
                if (terms != null && !terms.isEmpty()) {
                    BaseProperty<ObjectPropertyReference> field =
                        (BaseProperty<ObjectPropertyReference>) dataHolder.getField(propertyName);
                    if (field != null) {
                        String fieldType = field.getClassType();
                        if (StringUtils.equals(fieldType, "com.xpn.xwiki.objects.StringProperty")) {
                        /* there should be only one term present; just taking the head of the list */
                            field.setValue(termsToXWikiFormat(terms).get(0));
                        } else if (StringUtils.equals(fieldType, "com.xpn.xwiki.objects.DBStringListProperty")) {
                            ((DBStringListProperty) field).setList(termsToXWikiFormat(terms));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // nothing to do
        }
    }

    private List<String> termsToXWikiFormat(List<VocabularyTerm> terms)
    {
        List<String> ids = new LinkedList<>();
        for (VocabularyTerm term : terms) {
            ids.add(term.getId());
        }
        return ids;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        Iterator<Entry<String, List<VocabularyTerm>>> data =
            patient.<List<VocabularyTerm>>getData(DATA_NAME).dictionaryIterator();
        while (data.hasNext()) {
            Entry<String, List<VocabularyTerm>> datum = data.next();
            if (selectedFieldNames == null || selectedFieldNames.contains(datum.getKey())) {
                List<VocabularyTerm> terms = datum.getValue();
                if (terms == null || terms.isEmpty()) {
                    continue;
                }
                JSONArray elements = new JSONArray();
                for (VocabularyTerm term : terms) {
                    JSONObject element = new JSONObject();
                    element.put("id", term.getId());
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
                    while(elementsIterator.hasNext()) {
                        JSONObject element = (JSONObject) elementsIterator.next();
                        String termId = element.optString("id");
                        if (termId != null) {
                            VocabularyTerm term = vocabularyManager.resolveTerm(termId);
                            propertyTerms.add(term);
                        }
                    }
                    result.put(property, propertyTerms);
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
