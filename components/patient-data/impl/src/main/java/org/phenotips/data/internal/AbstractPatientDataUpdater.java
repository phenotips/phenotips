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

import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Some method implementations that can be used by objects that update patient data.
 *
 * @version $Id$
 */
public abstract class AbstractPatientDataUpdater extends AbstractEventListener
{
    /** Needed for getting access to the request. */
    @Inject
    private Container container;

    /**
     * @param name the listener's name.
     * @param events the list of events this listener is configured to receive.
     */
    public AbstractPatientDataUpdater(String name, List<? extends Event> events)
    {
        super(name, events);
    }

    /**
     * @param name the listener's name.
     * @param events the list of events this listener is configured to receive.
     */
    public AbstractPatientDataUpdater(String name, Event... events)
    {
        super(name, events);
    }

    /**
     * Read a property from the request.
     *
     * @param propertyName the name of the property as it would appear in the class, for example
     *            {@code age_of_onset_years}
     * @param objectNumber the object's number
     * @return the value sent in the request, or false if not set
     */
    protected boolean getParameter(String propertyName, int objectNumber)
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

    /**
     * Returns a patient record base object.
     *
     * @param source the event source i.e. the object for which the event was triggered.
     * @return a patient record object
     */
    protected BaseObject getPatientRecord(Object source)
    {
        if (source instanceof XWikiDocument) {
            XWikiDocument doc = (XWikiDocument) source;
            BaseObject patientRecordObj = doc.getXObject(Patient.CLASS_REFERENCE);
            return patientRecordObj;
        } else {
            return null;
        }
    }

}
