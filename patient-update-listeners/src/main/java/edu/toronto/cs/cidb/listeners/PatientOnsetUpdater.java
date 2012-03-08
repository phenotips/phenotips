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
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Update the Onset aggregated property whenever the congenital, year or month properties are changed.
 * 
 * @version $Id$
 */
@Component
@Named("patient-onset-updater")
@Singleton
public class PatientOnsetUpdater implements EventListener
{
    @Override
    public String getName()
    {
        return "patient-onset-updater";
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
            doc.getDocumentReference().getRoot().getName(), "ClinicalInformationCode", "FPatientClass"));
        if (patientRecordObj == null) {
            return;
        }
        String targetPropertyName = "onset";
        int congenital = patientRecordObj.getIntValue("congenital");
        int years = patientRecordObj.getIntValue("age_of_onset_years");
        int months = patientRecordObj.getIntValue("age_of_onset_months");
        String onset = patientRecordObj.getStringValue(targetPropertyName);
        String newOnset = "Not specified";
        if (congenital == 1) {
            newOnset = "Congenital";
        } else if (years != 0 || months != 0) {
            newOnset = displayNumber(years, "year", false);
            newOnset += displayNumber(months, "month", true);
            newOnset = newOnset.trim();
        }
        if (!newOnset.equals(onset)) {
            patientRecordObj.setStringValue(targetPropertyName, newOnset);
        }
    }

    /**
     * Display a number nicely.
     * 
     * @param value the number to display
     * @param itemName the unit, "year" or "month"
     * @param displayIfZero should the value be displayed or not when the value is 0
     * @return the formatted string, for example "2 years", "1 month", or the empty string if the value is 0 and it
     *         shouldn't be displayed
     */
    private String displayNumber(int value, String itemName, boolean displayIfZero)
    {
        String result = "";
        if (value != 0 || displayIfZero) {
            result = " " + value + " " + itemName;
            if (value != 1) {
                result += 's';
            }
        }
        return result;
    }
}
