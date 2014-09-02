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
import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

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

    /** Provides access to the document behind the patient record. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public PatientData<Integer> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException("The patient does not have a PatientClass");
            }
            Map<String, Integer> result = new LinkedHashMap<>();
            for (String propertyName : getProperties()) {
                String value = data.getStringValue(propertyName);
                if (NumberUtils.isDigits(value)) {
                    result.put(propertyName, Integer.valueOf(value));
                }
            }
            return new DictionaryPatientData<Integer>(DATA_NAME, result);
        } catch (Exception e) {
            this.logger.error("Could not read requested document");
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
        if (selectedFieldNames != null && !selectedFieldNames.contains(getName())) {
            return;
        }
        PatientData<Integer> data = patient.getData(getName());
        if (data == null || !data.isNamed()) {
            return;
        }

        Iterator<Entry<String, Integer>> iterator = data.dictionaryIterator();
        if (!iterator.hasNext()) {
            return;
        }

        JSONObject container = json.getJSONObject(DATA_NAME);
        if (container == null || container.isNullObject()) {
            json.put(DATA_NAME, new JSONObject());
            container = json.getJSONObject(DATA_NAME);
        }
        while (iterator.hasNext()) {
            Entry<String, Integer> item = iterator.next();
            container.put(item.getKey(), item.getValue());
        }
    }

    @Override
    public PatientData<Integer> readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
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
