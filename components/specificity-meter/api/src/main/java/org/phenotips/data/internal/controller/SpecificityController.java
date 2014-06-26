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
import org.phenotips.data.PatientSpecificity;
import org.phenotips.data.PatientSpecificityService;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.stability.Unstable;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.processors.JsonValueProcessor;

/**
 * Exposes the patient profile specificity.
 *
 * @version $Id$
 * @since 1.0M12
 */
@Component(roles = { PatientDataController.class })
@Named("specificity")
@Singleton
@Unstable
public class SpecificityController implements PatientDataController<Object>, Initializable
{
    /** The name of the data module exposed by this class. */
    private static final String NAME = "specificity";

    /** The actual service performing the specificity computation. */
    @Inject
    private PatientSpecificityService service;

    /** Special JSON configuration that formats {@code Date}s using the ISO8601 format. */
    private JsonConfig jsonConfig = new JsonConfig();

    @Override
    public void initialize() throws InitializationException
    {
        this.jsonConfig.registerJsonValueProcessor(Date.class, new JsonValueProcessor()
        {
            @Override
            public Object processObjectValue(String key, Object value, JsonConfig jsonConfig)
            {
                return processArrayValue(value, jsonConfig);
            }

            @Override
            public Object processArrayValue(Object value, JsonConfig jsonConfig)
            {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT).format(value);
            }
        });

    }

    @Override
    public PatientData<Object> load(Patient patient)
    {
        PatientSpecificity spec = this.service.getSpecificity(patient);
        if (spec != null) {
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("score", spec.getScore());
            data.put("date", spec.getComputationDate());
            data.put("server", spec.getComputingMethod());
            return new DictionaryPatientData<Object>(NAME, data);
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        // Nothing to save, the score is always computed
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames == null || selectedFieldNames.contains(NAME)) {
            PatientData<Object> specificity = patient.getData(NAME);
            if (specificity != null && specificity.isNamed()) {
                JSONObject result = new JSONObject();

                Iterator<Entry<String, Object>> data = specificity.dictionaryIterator();
                while (data.hasNext()) {
                    Entry<String, Object> datum = data.next();
                    result.element(datum.getKey(), datum.getValue(), this.jsonConfig);
                }
                json.put(NAME, result);
            }
        }
    }

    @Override
    public PatientData<Object> readJSON(JSONObject json)
    {
        // No need to read this, the score is not persisted
        return null;
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
