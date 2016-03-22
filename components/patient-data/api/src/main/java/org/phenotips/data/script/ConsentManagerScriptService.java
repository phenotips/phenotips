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

package org.phenotips.data.script;

import org.phenotips.data.ConsentManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONArray;

/**
 * API that provides access to patient consents.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
@Component
@Named("patientconsents")
@Singleton
public class ConsentManagerScriptService implements ScriptService
{
    @Inject
    private ConsentManager consentManager;

    /**
     * Checks if a specific consent is given for the patient record. If consentId is not configured
     * in the system returns {@code false} regardless of patient consent status.
     *
     * @param patientId record in which to test consent
     * @param consentId of consent which is to be checked
     * @return {@code true} if the consent was granted for the patient, otherwise {@code false}
     */
    public boolean hasConsent(String patientId, String consentId)
    {
        return this.consentManager.hasConsent(patientId, consentId);
    }

    /**
     * Returns the list of all consents configured in the system, with "granted"/"not granted" status
     * for the given patient for each consent.
     *
     * @param patientId record on which consents are granted (or not granted)
     * @return All the consents configured in the system with granted (for the patient) status set for each.
     *         Returns null if patientId is not a valid id.
     */
    public JSONArray getAllConsentsForPatient(String patientId)
    {
        return this.consentManager.toJSON(this.consentManager.getAllConsentsForPatient(patientId));
    }
}
