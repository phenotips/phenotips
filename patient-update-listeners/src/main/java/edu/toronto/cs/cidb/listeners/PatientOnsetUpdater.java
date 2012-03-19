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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Update the Onset aggregated property whenever the congenital, year or month properties are specified in the request.
 * 
 * @version $Id$
 */
@Component
@Named("patient-onset-updater")
@Singleton
public class PatientOnsetUpdater implements EventListener
{
    /** Needed for getting access to the request. */
    @Inject
    private Container container;

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
            doc.getDocumentReference().getRoot().getName(), "ClinicalInformationCode", "PatientClass"));
        if (patientRecordObj == null) {
            return;
        }
        String targetPropertyName = "onset";
        int congenital = getParameter("onset_congenital", patientRecordObj.getNumber());
        int years = getParameter("onset_years", patientRecordObj.getNumber());
        int months = getParameter("onset_months", patientRecordObj.getNumber());
        int onset = patientRecordObj.getIntValue(targetPropertyName);
        int newOnset = onset;
        if (congenital == 1) {
            newOnset = -1;
        } else if (years >= 0 && months >= 0) {
            newOnset = Math.max(-1, years * 12 + months);
        }
        if (newOnset != onset) {
            patientRecordObj.setIntValue(targetPropertyName, newOnset);
        }
    }

    /**
     * Read a property from the request.
     * 
     * @param propertyName the name of the property as it would appear in the class, for example {@code
     *        age_of_onset_years}
     * @param objectNumber the object's number
     * @return the value sent in the request, or {@code 0} if the property is missing
     */
    private int getParameter(String propertyName, int objectNumber)
    {
        String parameterName = "ClinicalInformationCode.PatientClass_" + objectNumber + "_" + propertyName;
        String value = (String) this.container.getRequest().getProperty(parameterName);
        if (value == null) {
            return -1;
        }
        return Integer.valueOf(value);
    }
}
