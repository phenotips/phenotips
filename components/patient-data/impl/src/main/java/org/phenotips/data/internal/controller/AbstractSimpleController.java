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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Base class for handling a collection of simple string values.
 *
 * @version $Id$
 * @since 1.0M10
 */
public abstract class AbstractSimpleController implements PatientDataController<String>
{
    /** Provides access to the underlying data storage. */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the current execution context. */
    @Inject
    private Execution execution;

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException("The patient does not have a PatientClass");
            }
            Map<String, String> result = new LinkedHashMap<>();
            for (String propertyName : getProperties()) {
                String value = data.getStringValue(propertyName);
                if (StringUtils.isNotBlank(value)) {
                    result.put(propertyName, value);
                }
            }
            return new DictionaryPatientData<>(getName(), result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document");
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject xwikiDataObject = doc.getXObject(Patient.CLASS_REFERENCE);
            if (xwikiDataObject == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            PatientData<String> data = patient.<String>getData(this.getName());
            if (!data.isNamed()) {
                return;
            }
            for (String property : this.getProperties()) {
                xwikiDataObject.setStringValue(property, data.get(property));
            }

            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            String comment = String.format("Updated %s from JSON", this.getName());
            context.getWiki().saveDocument(doc, comment, true, context);
        } catch (Exception e) {
            this.logger.error("Failed to save {}: [{}]", this.getName(), e.getMessage());
        }
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
        JSONObject container = json.getJSONObject(getJsonPropertyName());

        while (dataIterator.hasNext()) {
            Entry<String, String> datum = dataIterator.next();
            String key = datum.getKey();
            if (selectedFieldNames == null || selectedFieldNames.contains(key)) {
                if (container == null || container.isNullObject()) {
                    // put() is placed here because we want to create the property iff at least one field is set/enabled
                    json.put(getJsonPropertyName(), new JSONObject());
                    container = json.getJSONObject(getJsonPropertyName());
                }
                container.put(key, datum.getValue());
            }
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        if (!json.containsKey(this.getJsonPropertyName())) {
            // no data supported by this controller is present in provided JSON
            return null;
        }
        Map<String, String> result = new LinkedHashMap<String, String>();

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
