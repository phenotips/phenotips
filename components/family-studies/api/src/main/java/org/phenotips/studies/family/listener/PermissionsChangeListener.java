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
import org.phenotips.data.events.PatientChangedEvent;
import org.phenotips.data.events.PatientDeletedEvent;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * When a patient record's permissions are changed or a patient record is deleted, if that patient belongs to a family,
 * the family's permissions should change also.
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
    private FamilyRepository familyRepository;

    @Inject
    private PatientRepository patientRepository;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PermissionsChangeListener()
    {
        super("family-studies-permissions-listener", new PatientChangedEvent(), new PatientDeletedEvent());
    }

    // TODO: test!
    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument xwikiDoc = (XWikiDocument) source;
        String patientId = xwikiDoc.getDocumentReference().getName();
        Patient patient = this.patientRepository.getPatientById(patientId);
        Family family = this.familyRepository.getFamilyForPatient(patient);

        if (family == null) {
            return;
        }

        if (event instanceof PatientDeletedEvent) {
            family.removeMember(patient);

            // TODO delete family if no members left?
        } else {
            // if member was deleted (true branch of if), this is done in removing member.
            family.updatePermissions();
        }
    }
}
