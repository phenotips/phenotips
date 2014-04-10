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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleNamedData;

import org.xwiki.bridge.DocumentAccessBridge;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Base class for handling a collection of simple string values.
 *
 * @version $Id$
 * @since 1.0M10
 */
public abstract class AbstractSimpleController implements PatientDataController<ImmutablePair<String, String>>
{
    /** Provides access to the underlying data storage. */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public PatientData<ImmutablePair<String, String>> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException("The patient does not have a PatientClass");
            }
            List<ImmutablePair<String, String>> result = new LinkedList<ImmutablePair<String, String>>();
            for (String propertyName : getProperties()) {
                String value = data.getStringValue(propertyName);
                if (StringUtils.isNotBlank(value)) {
                    result.add(ImmutablePair.of(propertyName, value));
                }
            }
            return new SimpleNamedData<String>(getName(), result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document");
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        PatientData<ImmutablePair<String, String>> data = patient.getData(getName());
        if (data == null || data.isEmpty()) {
            return;
        }
        JSONObject container = json.getJSONObject(getJsonPropertyName());

        for (ImmutablePair<String, String> item : data) {
            if (selectedFieldNames == null || selectedFieldNames.contains(item.getKey())) {
                if (container == null || container.isNullObject()) {
                    // put() is placed here because we want to create the property iff at least one field is set/enabled
                    json.put(getJsonPropertyName(), new JSONObject());
                    container = json.getJSONObject(getJsonPropertyName());
                }
                container.put(item.getKey(), item.getValue());
            }
        }
    }

    @Override
    public PatientData<ImmutablePair<String, String>> readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }

    protected abstract List<String> getProperties();

    protected abstract String getJsonPropertyName();
}
