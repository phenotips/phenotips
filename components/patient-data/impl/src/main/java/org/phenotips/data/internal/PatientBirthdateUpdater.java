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
package org.phenotips.data.internal;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangingEvent;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Update the Date of birth aggregated property whenever the birth year, month and day properties are specified in the
 * request.
 *
 * @version $Id$
 */
@Component
@Named("patient-birthdate-updater")
@Singleton
public class PatientBirthdateUpdater extends AbstractEventListener
{
    /** Needed for getting access to the request. */
    @Inject
    private Container container;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PatientBirthdateUpdater()
    {
        super("patient-birthdate-updater", new PatientChangingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject patientRecordObj = doc.getXObject(Patient.CLASS_REFERENCE);
        if (patientRecordObj == null) {
            return;
        }
        String targetPropertyName = "date_of_birth";
        int year = getParameter("date_of_birth_year", patientRecordObj.getNumber());
        int month = getParameter("date_of_birth_month", patientRecordObj.getNumber());
        int day = getParameter("date_of_birth_day", patientRecordObj.getNumber());
        if (year == -1 || month == -1) {
            // No values specified in the request, skip this step
            return;
        }
        if (year == -2 || month == -2) {
            // Clear value specified in request, clear birth date.
            patientRecordObj.setDateValue(targetPropertyName, null);
            return;
        }
        if (day <= 0) {
            day = 1;
        }
        Date birthdate = patientRecordObj.getDateValue(targetPropertyName);
        Date newBirthdate = new GregorianCalendar(year, month, day).getTime();
        if (!newBirthdate.equals(birthdate)) {
            patientRecordObj.setDateValue(targetPropertyName, newBirthdate);
        }
    }

    /**
     * Read a property from the request.
     *
     * @param propertyName the name of the property as it would appear in the class, for example
     *            {@code age_of_onset_years}
     * @param objectNumber the object's number
     * @return the value sent in the request, or {@code 0} if the property is missing
     */
    private int getParameter(String propertyName, int objectNumber)
    {
        String parameterName = Constants.CODE_SPACE + ".PatientClass_" + objectNumber + "_" + propertyName;
        Request request = this.container.getRequest();
        if (request == null) {
            return -1;
        }
        String value = (String) request.getProperty(parameterName);
        if ("-".equals(value)) {
            return -2;
        }
        if (!StringUtils.isNumeric(value)) {
            return -1;
        }
        return Integer.valueOf(value);
    }
}
