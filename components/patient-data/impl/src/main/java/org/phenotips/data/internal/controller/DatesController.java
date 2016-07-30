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

import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the patient's date of birth and the exam date.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("dates")
@Singleton
public class DatesController implements PatientDataController<Date>
{
    protected static final String PATIENT_DATEOFDEATH_FIELDNAME = "date_of_death";

    protected static final String PATIENT_DATEOFDEATHENTERED_FIELDNAME = "date_of_death_entered";

    protected static final String PATIENT_DATEOFBIRTH_FIELDNAME = "date_of_birth";

    protected static final String PATIENT_DATEOFBIRTHENTERED_FIELDNAME = "date_of_birth_entered";

    protected static final String PATIENT_EXAMDATE_FIELDNAME = "exam_date";

    private static final String DATA_NAME = "dates";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private RecordConfigurationManager configurationManager;

    /** Provides access to the current execution context. */
    @Inject
    private Execution execution;

    @Override
    public PatientData<Date> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            Map<String, Date> result = new LinkedHashMap<String, Date>();
            for (String propertyName : getProperties()) {
                Date date = data.getDateValue(propertyName);
                if (date != null) {
                    result.put(propertyName, date);
                }
            }
            return new DictionaryPatientData<Date>(DATA_NAME, result);
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
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            PatientData<Date> dates = patient.getData(DATA_NAME);
            if (!dates.isNamed()) {
                return;
            }
            for (String property : this.getProperties()) {
                Date propertyValue = dates.get(property);
                if (propertyValue != null) {
                    data.setDateValue(property, dates.get(property));
                }
            }

            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            context.getWiki().saveDocument(doc, "Updated dates from JSON", true, context);
        } catch (Exception e) {
            this.logger.error("Failed to save dates: [{}]", e.getMessage());
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
        DateFormat dateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

        PatientData<Date> datesData = patient.getData(DATA_NAME);
        if (datesData == null || !datesData.isNamed()) {
            return;
        }

        Iterator<Entry<String, Date>> data = datesData.dictionaryIterator();
        while (data.hasNext()) {
            Entry<String, Date> datum = data.next();
            if (selectedFieldNames == null || selectedFieldNames.contains(datum.getKey())) {
                json.put(datum.getKey(), dateFormat.format(datum.getValue()));
            }
        }
    }

    @Override
    public PatientData<Date> readJSON(JSONObject json)
    {
        DateFormat dateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

        Map<String, Date> result = new LinkedHashMap<>();
        for (String property : this.getProperties()) {
            if (json.has(property)) {
                Object propertyValue = json.get(property);
                if (propertyValue != null) {
                    try {
                        result.put(property, dateFormat.parse(propertyValue.toString()));
                    } catch (ParseException ex) {
                        // nothing to do.
                    }
                }
            }
        }
        if (result.isEmpty()) {
            return null;
        } else {
            return new DictionaryPatientData<>(DATA_NAME, result);
        }
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    protected List<String> getProperties()
    {
        return Arrays.asList(PATIENT_DATEOFBIRTH_FIELDNAME, PATIENT_DATEOFBIRTHENTERED_FIELDNAME,
            PATIENT_DATEOFDEATH_FIELDNAME, PATIENT_DATEOFDEATHENTERED_FIELDNAME, PATIENT_EXAMDATE_FIELDNAME);
    }
}
