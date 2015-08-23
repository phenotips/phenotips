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

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangingEvent;

import org.xwiki.component.annotation.Component;

import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Removes the date of death object iff "unknown date of death" checkbox is checked.
 *
 * @version $Id$
 */
@Component
@Named("patient-deathdate-updater")
@Singleton
public class PatientDeathdateUpdater extends AbstractEventListener
{
    /** Needed for getting access to the request. */
    @Inject
    private Container container;

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
            return;
        }
        boolean unknownDeathDate = getParameter("date_of_death_unknown", patientRecordObj.getNumber());
        if (unknownDeathDate)
        {
            patientRecordObj.setDateValue("date_of_death", null);
            patientRecordObj.setDateValue("date_of_death_entered", null);
        }
    }

    /**
     * Read a property from the request.
     *
     * @param propertyName the name of the property as it would appear in the class, for example
     *            {@code age_of_onset_years}
     * @param objectNumber the object's number
     * @return the value sent in the request, or false if not set
     */
    private boolean getParameter(String propertyName, int objectNumber)
    {
        String parameterName = Constants.CODE_SPACE + ".PatientClass_" + objectNumber + "_" + propertyName;
        Request request = this.container.getRequest();
        if (request == null) {
            return false;
        }
        String value = (String) request.getProperty(parameterName);
        if (!StringUtils.isNumeric(value)) {
            return false;
        }
        return (Integer.valueOf(value) == 1);
    }
}
