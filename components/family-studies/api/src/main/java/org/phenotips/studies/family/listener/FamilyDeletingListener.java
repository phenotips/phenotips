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
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Detects the deletion of a family (and modifies the members' records accordingly)
 * and of a patient (and modifies the family accordingly).
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("familyDeletingListener")
@Singleton
public class FamilyDeletingListener implements EventListener
{
    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private PatientRepository patientRepository;

    @Override
    public String getName()
    {
        return "familyDeletingListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new DocumentDeletingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        if (document == null) {
            return;
        }

        String documentId = document.getDocumentReference().getName();

        Family family = this.familyRepository.getFamilyById(documentId);
        if (family != null) {
            // a family has been removed - unlink all patients
            List<Patient> members = family.getMembers();
            for (Patient patient : members) {
                family.removeMember(patient);
            }
        } else {
            // a patient has been removed - remove it from the family, if she has one
            Patient patient = this.patientRepository.getPatientById(documentId);
            if (patient != null) {
                family = this.familyRepository.getFamilyForPatient(patient);
                if (family != null) {
                    family.removeMember(patient);
                }
            }
        }
    }
}
