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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * Handles fields for solved patient records, including solved status, PubMed ID, gene symbol, and notes.
 *
 * @version $Id$
 * @since 1.2M2
 */
@Component(roles = { PatientDataController.class })
@Named("solved")
@Singleton
public class SolvedController extends AbstractSimpleController implements Initializable
{
    private static final String SOLVED_STRING = "solved";

    private static final String DATA_NAME = SOLVED_STRING;

    private static final String INTERNAL_PROPERTY_NAME = SOLVED_STRING;

    private static final String STATUS_KEY = SOLVED_STRING;

    private static final String STATUS_SOLVED = SOLVED_STRING;

    private static final String STATUS_UNSOLVED = "unsolved";

    private static final String STATUS_UNKNOWN = "";

    private static Map<String, String> fields = new LinkedHashMap<String, String>();

    @Override
    public void initialize() throws InitializationException
    {
        fields.put(STATUS_KEY, "status");
        fields.put("solved__pubmed_id", "pubmed_id");
        fields.put("solved__notes", "notes");
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    @Override
    protected String getJsonPropertyName()
    {
        return INTERNAL_PROPERTY_NAME;
    }

    protected String getJsonPropertyName(String property)
    {
        String name = fields.get(property);
        if (name == null) {
            name = property;
        }
        return name;
    }

    @Override
    protected List<String> getProperties()
    {
        Set<String> properties = fields.keySet();
        return new ArrayList<String>(properties);
    }

    private String parseSolvedStatus(String status)
    {
        if ("1".equals(status)) {
            return STATUS_SOLVED;
        } else if ("0".equals(status)) {
            return STATUS_UNSOLVED;
        } else {
            return STATUS_UNKNOWN;
        }
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
                // Parse value
                String value = datum.getValue();
                if (STATUS_KEY.equals(key)) {
                    value = parseSolvedStatus(value);
                }

                if (StringUtils.isNotBlank(value)) {
                    // Get internal property name
                    String name = getJsonPropertyName(key);
                    container.put(name, value);
                }
            }
        }
    }

    @Override
    public void save(Patient patient)
    {
        throw new UnsupportedOperationException();
    }

}
