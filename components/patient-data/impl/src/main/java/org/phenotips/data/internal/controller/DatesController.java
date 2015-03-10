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

import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;

import java.text.DateFormat;
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

import org.slf4j.Logger;

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
@Named("dates")
@Singleton
public class DatesController implements PatientDataController<Date>
{
    private static final String DATA_NAME = "dates";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private RecordConfigurationManager configurationManager;

    @Override
    public PatientData<Date> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException("The patient does not have a PatientClass");
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
        DateFormat dateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

        PatientData<Date> datesData = patient.getData(DATA_NAME);
        if (datesData == null) {
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
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }

    protected List<String> getProperties()
    {
        return Arrays.asList("date_of_birth", "date_of_death", "exam_date");
    }
}
