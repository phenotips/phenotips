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
package edu.toronto.cs.cidb.listeners;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.joda.time.Months;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;

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
    /** The space where the classes are. */
    private static final String CODE_SPACE = "ClinicalInformationCode";

    @Override
    public String getName()
    {
        return "measurement-age-updater";
    }

    @Override
    public List<Event> getEvents()
    {
        // The list of events this listener listens to
        return Arrays.<Event> asList(new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject patientRecordObj = doc.getXObject(new DocumentReference(
            doc.getDocumentReference().getRoot().getName(), CODE_SPACE, "PatientClass"));
        if (patientRecordObj == null) {
            return;
        }
        Date birthDate = patientRecordObj.getDateValue("date_of_birth");
        if (birthDate == null) {
            return;
        }
        String targetPropertyName = "age";
        for (BaseObject measurement : doc.getXObjects(new DocumentReference(
            doc.getDocumentReference().getRoot().getName(), CODE_SPACE, "MeasurementsClass"))) {
            Date measurementDate = measurement.getDateValue("date");
            if (measurementDate == null) {
                PropertyInterface prop = null;
                try {
                    prop = measurement.get(targetPropertyName);
                } catch (XWikiException ex) {
                    // Shouldn't happen, ignore it
                }
                if (prop != null) {
                    measurement.addPropertyForRemoval(prop);
                }
            } else {
                int age = Months.monthsBetween(new DateTime(birthDate), new DateTime(measurementDate)).getMonths();
                measurement.setIntValue(targetPropertyName, age);
            }
        }
    }
}
