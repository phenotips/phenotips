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
package org.phenotips.data.events.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.events.PatientDeletingEvent;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Detects the deletion of patient records and fires {@link PatientDeletingEvent}s.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("patientDeletingEventSource")
@Singleton
public class PatientDeletingEventSource implements EventListener
{
    @Inject
    private ObservationManager observationManager;

    @Inject
    private UserManager userManager;

    @Inject
    private DocumentAccessBridge dab;

    @Inject
    private PatientRepository repo;

    @Override
    public String getName()
    {
        return "patientDeletingEventSource";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new DocumentDeletingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        if (doc == null || "PatientTemplate".equals(doc.getDocumentReference().getName())) {
            return;
        }
        XWikiDocument odoc;
        try {
            odoc = (XWikiDocument) this.dab.getDocument(doc.getDocumentReference());
        } catch (Exception e) {
            return;
        }

        BaseObject patientRecordObj = odoc.getXObject(Patient.CLASS_REFERENCE);
        if (patientRecordObj == null) {
            return;
        }
        Patient patient = this.repo.loadPatientFromDocument(odoc);
        User user = this.userManager.getCurrentUser();
        CancelableEvent patientEvent = new PatientDeletingEvent(patient, user);
        this.observationManager.notify(patientEvent, odoc);
        if (patientEvent.isCanceled()) {
            // FIXME DocumentDeletingEvent is not cancelable yet!
            // ((CancelableEvent) event).cancel();
        }
    }
}
