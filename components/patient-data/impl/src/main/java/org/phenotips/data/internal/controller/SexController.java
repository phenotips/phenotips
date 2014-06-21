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
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Handles the patient's date of birth and the exam date.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("sex")
@Singleton
public class SexController implements PatientDataController<String>
{
    private static final String DATA_NAME = "sex";

    private static final String INTERNAL_PROPERTY_NAME = "gender";

    private static final String EXPOSED_PROPERTY_NAME = DATA_NAME;

    private static final String SEX_MALE    = "M";
    private static final String SEX_FEMALE  = "F";
    private static final String SEX_UNKNOWN = "U";

    private static final String ERROR_MESSAGE_NO_PATIENT_CLASS = "The patient does not have a PatientClass";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Provides access to the current execution context. */
    @Inject
    private Execution execution;

    private String parseGender(String gender)
    {
        return (StringUtils.equals(SEX_FEMALE, gender) || StringUtils.equals(SEX_MALE, gender)) ? gender : SEX_UNKNOWN;
    }

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }
            List<ImmutablePair<String, String>> result = new LinkedList<ImmutablePair<String, String>>();
            String gender = parseGender(data.getStringValue(INTERNAL_PROPERTY_NAME));
            result.add(ImmutablePair.of(EXPOSED_PROPERTY_NAME, gender));
            return new SimpleNamedData<String>(DATA_NAME, result);
        } catch (Exception e) {
            this.logger.error("Failed to load patient gender: [{}]", e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            String gender = patient.<ImmutablePair<String, String>>getData(DATA_NAME).get(0).getValue();

            this.logger.warn("Saving gender value: [{}]", gender);

            data.setStringValue(INTERNAL_PROPERTY_NAME, gender);

            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            context.getWiki().saveDocument(doc, "Updated gender from JSON", true, context);
        } catch (Exception e) {
            this.logger.error("Failed to save patient gender: [{}]", e.getMessage());
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
        if (selectedFieldNames != null && !selectedFieldNames.contains(INTERNAL_PROPERTY_NAME)) {
            return;
        }

        PatientData<String> patientData = patient.<String>getData(DATA_NAME);
        if (patientData != null && patientData.isNamed()) {
            Iterator<String> values = patientData.iterator();
            Iterator<String> keys = patientData.keyIterator();
            while (keys.hasNext()) {
                json.put(keys.next(), values.next());
            }
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        if (!json.containsKey(DATA_NAME)) {
            // no supported data in provided JSON
            return null;
        }

        List<ImmutablePair<String, String>> result = new LinkedList<ImmutablePair<String, String>>();

        String gender = parseGender(json.getString(DATA_NAME));
        result.add(ImmutablePair.of(EXPOSED_PROPERTY_NAME, gender));

        return new SimpleNamedData<String>(DATA_NAME, result);
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }
}
