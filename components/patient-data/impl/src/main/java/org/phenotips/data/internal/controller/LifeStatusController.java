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
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.component.annotation.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

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

    private static final String ALIVE = "alive";

    private static final String DECEASED = "deceased";

    private static final Set<String> ALL_LIFE_STATES = new HashSet<>(Arrays.asList(ALIVE, DECEASED));

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontext;

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }

            String lifeStatus = data.getStringValue(DATA_NAME);

            return new SimpleValuePatientData<>(DATA_NAME, lifeStatus);
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        save(patient, PatientWritePolicy.UPDATE);
    }

    @Override
    public void save(@Nonnull final Patient patient, @Nonnull final PatientWritePolicy policy)
    {
        try {
            final BaseObject dataHolder = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE, true,
                this.xcontext.get());
            final PatientData<String> lifeStatus = patient.getData(DATA_NAME);
            if (lifeStatus == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    // Set to a default value.
                    dataHolder.setStringValue(DATA_NAME, ALIVE);
                }
            } else {
                dataHolder.setStringValue(DATA_NAME, lifeStatus.getValue());
            }

        } catch (final Exception ex) {
            this.logger.error("Failed to save life status data: {}", ex.getMessage(), ex);
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
            if (selectedFieldNames != null && selectedFieldNames.contains(DATA_NAME)) {
                json.put(DATA_NAME, ALIVE);
            }
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
                return new SimpleValuePatientData<>(DATA_NAME, propertyValue);
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
