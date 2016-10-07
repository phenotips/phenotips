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
package org.phenotips.data.permissions.events;

import org.apache.commons.lang3.StringUtils;

import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;

/**
 * An event that is fired every time patient prmissions are updated.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Unstable
public class PatientRightsUpdatedEvent implements Event
{
    /** The affected patient id. */
    protected final String patientId;

    /**
     * Constructor initializing the required fields.
     *
     * @param eventType the type of this event
     * @param family the affected family
     * @param author the user performing this action
     */
    public PatientRightsUpdatedEvent(String patientId)
    {
        this.patientId = patientId;
    }

    /** Default constructor, to be used for declaring the events a listener wants to observe. */
    public PatientRightsUpdatedEvent()
    {
        this(null);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent instanceof PatientRightsUpdatedEvent) {
            PatientRightsUpdatedEvent otherRightsUpdateEvent = (PatientRightsUpdatedEvent) otherEvent;
            if (patientId != null && !StringUtils.equals(otherRightsUpdateEvent.getPatientId(), this.patientId)) {
                return false;
            }
            return true;
        }
        return false;
    }

    public String getPatientId()
    {
        return this.patientId;
    }
}
