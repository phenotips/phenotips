/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.internal.controller;

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.internal.AbstractPhenoTipsOntologyProperty;

import org.xwiki.bridge.DocumentAccessBridge;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
 * custom functions for dealing with conversion to booleans, and ontology codes to human readable labels.
 *
 * @version $Id$
 * @since 1.0M10
 */
public abstract class AbstractComplexController<T> implements PatientDataController<T>
{
    /** Provides access to the underlying data storage. */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    /** Logging helper object. */
    @Inject
    protected Logger logger;

    @Override
    public PatientData<T> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException("The patient does not have a PatientClass");
            }
            Map<String, T> result = new HashMap<String, T>();
            for (String propertyName : getProperties()) {
                BaseProperty field = (BaseProperty) data.getField(propertyName);
                if (field != null) {
                    Object propertyValue = field.getValue();
                    /* If the controller only works with codes, store the Ontology Instances rather than Strings */
                    if (getCodeFields().contains(propertyName) && isCodeFieldsOnly()) {
                        List<org.phenotips.data.OntologyProperty> propertyValuesList =
                            new LinkedList<org.phenotips.data.OntologyProperty>();
                        for (Object id : (List) propertyValue) {
                            propertyValuesList.add(new OntologyProperty((String) id));
                        }
                        propertyValue = propertyValuesList;
                    }
                    result.put(propertyName, (T) propertyValue);
                }
            }
            return new DictionaryPatientData<T>(getName(), result);
        } catch (Exception e) {
            this.logger.error(
                "Could not find requested document or some unforeseen error has occurred during controller loading");
        }
        return null;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        PatientData<T> data = patient.getData(getName());
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

    protected abstract List<String> getBooleanFields();

    protected abstract List<String> getCodeFields();

    protected Boolean isCodeFieldsOnly()
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
            OntologyProperty term;
            if (code instanceof OntologyProperty) {
                term = (OntologyProperty) code;
            } else {
                term = new OntologyProperty(code.toString());
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
}

/**
 * There exists no class currently that would be able to covert an ontology code into a human readable format given only
 * a code string. Considering that there is a need for such functionality, there are 3 options: copy the code that
 * performs the function needed into the controller, create a class extending {@link
 * org.phenotips.data.internal.AbstractPhenoTipsOntologyProperty} in a separate file, or create such class here. Given
 * the fact the the {@link org.phenotips.data.internal.AbstractPhenoTipsOntologyProperty} is abstract only by having a
 * protected constructor, which fully satisfies the needed functionality, it makes the most sense to put {@link
 * OntologyProperty} here.
 */
class OntologyProperty extends AbstractPhenoTipsOntologyProperty
{
    public OntologyProperty(String id)
    {
        super(id);
    }
}
