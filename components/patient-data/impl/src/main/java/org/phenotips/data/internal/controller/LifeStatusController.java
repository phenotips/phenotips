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
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Handles the patient's life status: alive or deceased.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component(roles = { PatientDataController.class })
@Named("lifeStatus")
@Singleton
public class LifeStatusController implements PatientDataController<String>
{
    private static final String DATA_NAME = "life_status";

    private static final String PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME = "date_of_death_unknown";

    private static final String PATIENT_DATEOFDEATH_FIELDNAME = DatesController.PATIENT_DATEOFDEATH_FIELDNAME;

    private static final String PATIENT_DATEOFDEATH_ENTERED_FIELDNAME = "date_of_death_entered";

    private static final String ALIVE = "alive";

    private static final String DECEASED = "deceased";

    private static final Set<String> ALL_LIFE_STATES = new HashSet<String>(Arrays.asList(ALIVE, DECEASED));

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontext;

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }

            String lifeStatus = ALIVE;
            Date date = data.getDateValue(PATIENT_DATEOFDEATH_FIELDNAME);
            String dodEntered = data.getStringValue(PATIENT_DATEOFDEATH_ENTERED_FIELDNAME);
            if (date != null || (StringUtils.isNotBlank(dodEntered) && !"{}".equals(dodEntered))) {
                lifeStatus = DECEASED;
            } else {
                // check if "unknown death date" checkbox is checked
                Integer deathDateUnknown = data.getIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME);
                if (deathDateUnknown == 1) {
                    lifeStatus = DECEASED;
                }
            }
            return new SimpleValuePatientData<String>(DATA_NAME, lifeStatus);
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

            PatientData<String> lifeStatus = patient.getData(DATA_NAME);
            PatientData<Date> dates = patient.getData("dates");

            Integer deathDateUnknown = 0;
            if (lifeStatus != null && DECEASED.equals(lifeStatus.getValue())) {
                deathDateUnknown = 1;
            }
            // check if date_of_death is set - if it is unknown_death_date should be unset
            if (dates != null && dates.isNamed() && dates.get(PATIENT_DATEOFDEATH_FIELDNAME) != null) {
                deathDateUnknown = 0;
            }

            data.setIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME, deathDateUnknown);

            this.xcontext.get().getWiki().saveDocument(doc, "Updated life status from JSON", true, this.xcontext.get());
        } catch (Exception e) {
            this.logger.error("Failed to save life status: [{}]", e.getMessage());
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
        if (selectedFieldNames != null && !selectedFieldNames.contains(DATA_NAME)) {
            return;
        }

        PatientData<String> lifeStatusData = patient.getData(DATA_NAME);
        if (lifeStatusData == null) {
            return;
        }
        json.put(DATA_NAME, lifeStatusData.getValue());
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        String propertyValue = json.optString(DATA_NAME, null);
        if (propertyValue != null) {
            // validate - only accept listed values
            if (ALL_LIFE_STATES.contains(propertyValue)) {
                return new SimpleValuePatientData<String>(DATA_NAME, propertyValue);
            }
        }
        return null;
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }
}
