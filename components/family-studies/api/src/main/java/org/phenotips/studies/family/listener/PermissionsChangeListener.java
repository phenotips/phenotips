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
package org.phenotips.studies.family.listener;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.events.PatientRightsUpdatedEvent;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * When a patient record's permissions are changed, if that patient belongs to a family, the family's permissions should
 * change also.
 *
 * @version $Id$
 * @since 1.0M
 */
@Component
@Named("family-studies-permissions-listener")
@Singleton
public class PermissionsChangeListener extends AbstractEventListener
{
    @Inject
    @Named("Family")
    private FamilyRepository familyRepository;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private Logger logger;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PermissionsChangeListener()
    {
        super("family-studies-patient-permissions-listener", new PatientRightsUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String patientId = ((PatientRightsUpdatedEvent) event).getPatientId();

        this.logger.debug("Updating familypermissions for patient [{}]", patientId);

        Patient patient = this.patientRepository.get(patientId);
        if (patient == null) {
            return;
        }
        Family family = this.familyRepository.getFamilyForPatient(patient);
        if (family == null) {
            return;
        }
        this.familyRepository.updateFamilyPermissions(family);
    }
}
