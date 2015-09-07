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
import org.phenotips.data.VocabularyProperty;
import org.phenotips.data.internal.AbstractPhenoTipsVocabularyProperty;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Base class for handling data in different types of objects (String, List, etc) and preserving the object type. Has
 * custom functions for dealing with conversion to booleans, and vocabulary codes to human readable labels.
 *
 * @param <T> the type of data being managed by this component, usually {@code String}, but other types are possible,
 *            even more complex types
 * @version $Id$
 * @since 1.0RC1
 */
public abstract class AbstractComplexController<T> implements PatientDataController<T>
{
    /** Provides access to the underlying data storage. */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    @SuppressWarnings("unchecked")
    public PatientData<T> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(getXClassReference());
            if (data == null) {
                return null;
            }
            Map<String, T> result = new LinkedHashMap<String, T>();
            for (String propertyName : getProperties()) {
                BaseProperty<ObjectPropertyReference> field =
                    (BaseProperty<ObjectPropertyReference>) data.getField(propertyName);
                if (field != null) {
                    Object propertyValue = field.getValue();
                    /* If the controller only works with codes, store the Vocabulary Instances rather than Strings */
                    if (getCodeFields().contains(propertyName) && isCodeFieldsOnly()) {
                        List<VocabularyProperty> propertyValuesList = new LinkedList<>();
                        List<String> terms = (List<String>) propertyValue;
                        for (String termId : terms) {
                            propertyValuesList.add(new QuickVocabularyProperty(termId));
                        }
                        propertyValue = propertyValuesList;
                    }
                    result.put(propertyName, (T) propertyValue);
                }
            }
            return new DictionaryPatientData<>(getName(), result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        PatientData<T> data = patient.getData(getName());
        if (data == null) {
            return;
        }
        Iterator<Map.Entry<String, T>> iterator = data.dictionaryIterator();
        if (iterator == null || !iterator.hasNext()) {
            return;
        }
        JSONObject container = json.getJSONObject(getJsonPropertyName());

        while (iterator.hasNext()) {
            Map.Entry<String, T> item = iterator.next();
            if (selectedFieldNames == null || selectedFieldNames.contains(item.getKey())) {
                if (container == null || container.isNullObject()) {
                    // put() is placed here because we want to create the property iff at least one field is set/enabled
                    json.put(getJsonPropertyName(), new JSONObject());
                    container = json.getJSONObject(getJsonPropertyName());
                }
                String itemKey = item.getKey();
                container.put(itemKey, format(itemKey, item.getValue()));
            }
        }
    }

    /** @return list of fields which should be resolved to booleans */
    protected abstract List<String> getBooleanFields();

    /**
     * @return list of fields which contain HPO codes, and therefore additional data can be obtained, such as human
     *         readable name
     */
    protected abstract List<String> getCodeFields();

    /**
     * In case all fields are code fields, then the controller can store data in memory as vocabulary objects rather
     * than strings.
     *
     * @return true if all fields contain HPO codes
     */
    protected boolean isCodeFieldsOnly()
    {
        return false;
    }

    /**
     * Checks if a the value needs to be formatted and then calls the appropriate function.
     *
     * @param key the key under which the value will be stored in JSON
     * @param value the value which possibly needs to be formatted
     * @return the formatted object or the original value
     */
    @SuppressWarnings("unchecked")
    private Object format(String key, Object value)
    {
        if (value == null) {
            return null;
        }
        if (getBooleanFields().contains(key)) {
            return booleanConvert(value.toString());
        } else if (getCodeFields().contains(key)) {
            return codeToHumanReadable((List<T>) value);
        } else {
            return value;
        }
    }

    private Boolean booleanConvert(String integerValue)
    {
        if (StringUtils.equals("0", integerValue)) {
            return false;
        } else if (StringUtils.equals("1", integerValue)) {
            return true;
        } else {
            return null;
        }
    }

    private JSONArray codeToHumanReadable(List<T> codes)
    {
        JSONArray labeledList = new JSONArray();
        for (T code : codes) {
            QuickVocabularyProperty term;
            if (code instanceof QuickVocabularyProperty) {
                term = (QuickVocabularyProperty) code;
            } else {
                term = new QuickVocabularyProperty(code.toString());
            }
            labeledList.add(term.toJSON());
        }
        return labeledList;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void save(Patient patient)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PatientData<T> readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }

    protected abstract List<String> getProperties();

    protected abstract String getJsonPropertyName();

    /**
     * The XClass used for storing data managed by this controller. By default, data is stored in the main
     * {@code PhenoTips.PatientClass} object that defines the patient record. Override this method if a different type
     * of XObject is used.
     *
     * @return a local reference (without the wiki reference) pointing to the XDocument containing the target XClass
     * @since 1.2RC1
     */
    protected EntityReference getXClassReference()
    {
        return Patient.CLASS_REFERENCE;
    }

    /**
     * There exists no class currently that would be able to covert a vocabulary code into a human readable format given
     * only a code string. Considering that there is a need for such functionality, there are 3 options: copy the code
     * that performs the function needed into the controller, create a class extending
     * {@link org.phenotips.data.internal.AbstractPhenoTipsVocabularyProperty} in a separate file, or create such class
     * here. Given the fact the the {@link org.phenotips.data.internal.AbstractPhenoTipsVocabularyProperty} is abstract
     * only by having a protected constructor, which fully satisfies the needed functionality, it makes the most sense
     * to put {@link QuickVocabularyProperty} here.
     */
    protected static final class QuickVocabularyProperty extends AbstractPhenoTipsVocabularyProperty
    {
        public QuickVocabularyProperty(String id)
        {
            super(id);
        }
    }
}
