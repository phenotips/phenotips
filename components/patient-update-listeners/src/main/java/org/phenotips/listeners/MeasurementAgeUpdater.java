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
package org.phenotips.listeners;

import org.phenotips.Constants;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.joda.time.Months;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Update the Age property whenever the birth date or measurement date properties are changed.
 *
 * @version $Id$
 */
@Component
@Named("measurement-age-updater")
@Singleton
public class MeasurementAgeUpdater implements EventListener
{
    /** The name of the XProperty holding the age at the time of measurement, which will be updated by this listener. */
    private static final String AGE_PROPERTY_NAME = "age";

    /** The name of the XProperty holding the date when the measurement occurred. */
    private static final String DATE_PROPERTY_NAME = "date";

    @Override
    public String getName()
    {
        return "measurement-age-updater";
    }

    @Override
    public List<Event> getEvents()
    {
        // The list of events this listener listens to
        return Arrays.<Event>asList(new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject patientRecordObj = doc.getXObject(new DocumentReference(
            doc.getDocumentReference().getRoot().getName(), Constants.CODE_SPACE, "PatientClass"));
        if (patientRecordObj == null) {
            return;
        }
        Date birthDate = patientRecordObj.getDateValue("date_of_birth");
        if (birthDate == null) {
            return;
        }
        List<BaseObject> objects = doc.getXObjects(new DocumentReference(
            doc.getDocumentReference().getRoot().getName(), Constants.CODE_SPACE, "MeasurementsClass"));
        if (objects == null || objects.isEmpty()) {
            return;
        }
        for (BaseObject measurement : objects) {
            if (measurement == null) {
                continue;
            } else if ("birth".equals(measurement.getStringValue("type"))) {
                measurement.setIntValue(AGE_PROPERTY_NAME, 0);
                measurement.removeField(DATE_PROPERTY_NAME);
                continue;
            }
            Date measurementDate = measurement.getDateValue(DATE_PROPERTY_NAME);
            if (measurementDate == null) {
                measurement.removeField(AGE_PROPERTY_NAME);
            } else {
                int age = Months.monthsBetween(new DateTime(birthDate), new DateTime(measurementDate)).getMonths();
                measurement.setIntValue(AGE_PROPERTY_NAME, age);
            }
        }
    }
}
