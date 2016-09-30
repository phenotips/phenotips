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
import org.phenotips.studies.family.FamilyTools;

import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Detects the deletion of a family (and modifies the members' records accordingly) and of a patient (and modifies the
 * family accordingly).
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
    private FamilyTools familyTools;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private Logger logger;

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
        XWikiContext context = this.provider.get();
        XWiki xwiki = context.getWiki();
        XWikiDocument document = (XWikiDocument) source;
        if (document == null) {
            return;
        }

        String documentId = document.getDocumentReference().getName();
        try {
            Family family = this.familyTools.getFamilyById(documentId);
            if (family != null) {
                this.familyTools.forceRemoveAllMembers(family);
            } else {
                // a patient has been removed - remove it from the family, if she has one
                Patient patient = this.patientRepository.get(documentId);
                if (patient == null) {
                    return;
                }
                family = this.familyTools.getFamilyForPatient(documentId);
                if (family == null) {
                    return;
                }
                this.familyTools.removeMember(documentId);

                // clear the proband field if and only if the deleted patient was indeed the proband
                DocumentReference familyDocumentRef = family.getDocumentReference();
                XWikiDocument familyDocument = xwiki.getDocument(familyDocumentRef, context);
                BaseObject familyClassObject = familyDocument.getXObject(Family.CLASS_REFERENCE);
                if (familyClassObject.getStringValue("proband_id").equals(patient.getDocument().toString())) {
                    familyClassObject.setStringValue("proband_id", "");
                    xwiki.saveDocument(familyDocument, "Proband was deleted", true, context);
                }
            }
        } catch (XWikiException e) {
            this.logger.error("Failed to access the document: {}", e.getMessage(), e);
        }
    }
}
