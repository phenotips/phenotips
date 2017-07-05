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
package org.phenotips.data.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangingEvent;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Removes the date of death object iff "life_status" is "alive".
 *
 * @version $Id$
 */
@Component
@Named("patient-deathdate-updater")
@Singleton
public class PatientDeathdateUpdater extends AbstractEventListener
{
    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PatientDeathdateUpdater()
    {
        super("patient-deathdate-updater", new PatientChangingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject patientRecordObj = doc.getXObject(Patient.CLASS_REFERENCE);
        if (patientRecordObj == null) {
            // No patient, nothing to do
            return;
        }
        if (("alive").equals(patientRecordObj.getStringValue("life_status"))) {
            patientRecordObj.setDateValue("date_of_death", null);
            patientRecordObj.setStringValue("date_of_death_entered", null);
        }
    }
}
