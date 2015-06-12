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
package org.phenotips.configuration.script;

import org.phenotips.configuration.ConsentManager;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;

import org.phenotips.configuration.internal.configured.ConfiguredConsentManager;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Provides access to {@code RecordConfiguration patient record configurations}.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Component
@Named("recordConfiguration")
@Singleton
public class RecordConfigurationManagerScriptService implements ScriptService
{
    /** The actual configuration manager. */
    @Inject
    private RecordConfigurationManager configuration;

    /** Provides access to the data. */
    @Inject
    private DocumentAccessBridge dab;

    /** Provides access to patients. */
    @Inject
    private PatientRepository patientRepository;

    /**
     * Retrieves the {@code RecordConfiguration patient record configuration} active for the current user.
     *
     * @return a valid configuration, either the global one or one configured, for example in one of the user's groups
     */
    public RecordConfiguration getActiveConfiguration()
    {
        return this.configuration.getActiveConfiguration();
    }

    public ConsentManager getConsentManager(DocumentReference patientReference)
    {
        Patient patient = null;
        try {
            DocumentModelBridge patientDoc = dab.getDocument(patientReference);
            patient = patientRepository.loadPatientFromDocument(patientDoc);
        } catch (Exception ex) {
            // do nothing
        }
        return new ConfiguredConsentManager(patient);
    }
}
