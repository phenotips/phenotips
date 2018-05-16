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
package org.phenotips.data.securestorage.events;

import org.phenotips.data.events.PatientDeletingEvent;
import org.phenotips.data.securestorage.SecureStorageManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Removes associated push history from database for the patient that was deleted.
 *
 * @version $Id$
 */
@Component
@Named("patient-push-history-remover")
@Singleton
public class PatientPushHistoryRemover extends AbstractEventListener
{
    @Inject
    private SecureStorageManager pushStorageManager;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PatientPushHistoryRemover()
    {
        super("patient-push-history-remover", new PatientDeletingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        String patientId = doc.getDocumentReference().getName();
        this.pushStorageManager.deletePatientPushInfo(patientId);
    }
}
