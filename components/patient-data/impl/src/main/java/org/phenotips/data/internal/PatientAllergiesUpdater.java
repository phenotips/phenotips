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

import org.phenotips.data.events.PatientChangingEvent;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.Event;

import java.util.LinkedList;

import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Removes allergies list iff NKDA field (no known drug allergies) is set to true.
 *
 * @version $Id$
 */
@Component
@Named("patient-allergies-updater")
@Singleton
public class PatientAllergiesUpdater extends AbstractPatientDataUpdater
{
    private static final String NKDA = "NKDA";

    private static final String ALLERGIES = "allergies";

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PatientAllergiesUpdater()
    {
        super("patient-allergies-updater", new PatientChangingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        BaseObject patientRecordObj = getPatientRecord(source);
        if (patientRecordObj == null) {
            return;
        }

        boolean nkda = getParameter(NKDA, patientRecordObj.getNumber());
        if (nkda)
        {
            patientRecordObj.setStringListValue(ALLERGIES, new LinkedList<String>());
        }
    }
}
