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
package org.phenotips.data.events.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.events.PatientChangedEvent;
import org.phenotips.data.events.PatientCreatedEvent;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
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
 * Detects new patient records and fires a {@link PatientCreatedEvent} followed by a{@link PatientChangedEvent}.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("patientCreatedEventSource")
@Singleton
public class PatientCreatedEventSource implements EventListener
{
    @Inject
    private ObservationManager observationManager;

    @Inject
    private UserManager userManager;

    @Inject
    private PatientRepository repo;

    @Override
    public String getName()
    {
        return "patientCreatedEventSource";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new DocumentCreatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject patientRecordObj = doc.getXObject(Patient.CLASS_REFERENCE);
        if (patientRecordObj == null || "PatientTemplate".equals(doc.getDocumentReference().getName())) {
            return;
        }
        Patient patient = this.repo.load(doc);
        User user = this.userManager.getCurrentUser();
        this.observationManager.notify(new PatientCreatedEvent(patient, user), source);
        this.observationManager.notify(new PatientChangedEvent(patient, user), source);
    }
}
