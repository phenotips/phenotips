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

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.context.Execution;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Update the Onset aggregated property whenever the congenital, year or month properties are changed.
 * 
 * @version $Id$
 */
public class PatientSaveListener implements EventListener
{
    /** Provides access to the current execution context. */
    @Inject
    private Execution execution;

    @Override
    public String getName()
    {
        // The unique name of this event listener
        return "PatientRecordSaved";
    }

    @Override
    public List<Event> getEvents()
    {
        // The list of events this listener listens to
        return Arrays.<Event> asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Current context
        XWikiContext crtContext = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        XWikiDocument doc = (XWikiDocument) source;
        BaseObject patientRecordObj = doc.getObject("ClinicalInformationCode.FPatientClass");
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
            doc.setMetaDataDirty(false);
            doc.setContentDirty(false);
            try {
                crtContext.getWiki().saveDocument(doc, doc.getComment(), doc.isMinorEdit(), crtContext);
            } catch (XWikiException ex) {
                // TODO Auto-generated catch block
            }
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
