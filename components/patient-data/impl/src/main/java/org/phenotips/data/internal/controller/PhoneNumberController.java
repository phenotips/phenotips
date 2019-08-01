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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.json.JSONObject;
import org.phenotips.data.*;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles the patient's contact info i.e phone number.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component(roles = {PatientDataController.class})
@Named("phoneNumber")
@Singleton
public class PhoneNumberController implements PatientDataController<String> {
    private static final String DATA_NAME = "phone_number";

    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontext;

    @Override
    public PatientData<String> load(Patient patient) {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }

            String phoneNumber = data.getStringValue(DATA_NAME);

            return new SimpleValuePatientData<>(DATA_NAME, phoneNumber);
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient) {
        save(patient, PatientWritePolicy.UPDATE);
    }

    @Override
    public void save(@Nonnull final Patient patient, @Nonnull final PatientWritePolicy policy) {
        try {
            final BaseObject dataHolder = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE_INFO, true,
                    this.xcontext.get());
            final PatientData<String> phoneNumber = patient.getData(DATA_NAME);
            if (phoneNumber != null) {
                dataHolder.setStringValue(DATA_NAME, phoneNumber.getValue());
            }

        } catch (final Exception ex) {
            this.logger.error("Failed to save phone number: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json) {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames) {
        if (selectedFieldNames != null && !selectedFieldNames.contains(DATA_NAME)) {
            return;
        }
        BaseObject dataHolder = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE_INFO);
        String lifeStatusData = dataHolder.getStringValue(DATA_NAME);
        if (lifeStatusData != null) {
            json.put(DATA_NAME, lifeStatusData);
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json) {
        String propertyValue = json.optString(DATA_NAME, null);
        if (propertyValue != null) {
            return new SimpleValuePatientData<>(DATA_NAME, propertyValue);
        }
        return null;
    }

    @Override
    public String getName() {
        return DATA_NAME;
    }
}
