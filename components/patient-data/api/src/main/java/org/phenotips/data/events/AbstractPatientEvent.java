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
package org.phenotips.data.events;

import org.phenotips.data.Patient;

import org.xwiki.users.User;

import org.apache.commons.lang3.StringUtils;

/**
 * Base class for implementing {@link PatientEvent}.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public abstract class AbstractPatientEvent implements PatientEvent
{
    /** The type of this event. */
    protected final String eventType;

    /** The affected patient. */
    protected final Patient patient;

    /** The user performing this action. */
    protected final User author;

    /**
     * Constructor initializing the required fields.
     *
     * @param eventType the type of this event
     * @param patient the affected patient
     * @param author the user performing this action
     */
    protected AbstractPatientEvent(String eventType, Patient patient, User author)
    {
        this.eventType = eventType;
        this.patient = patient;
        this.author = author;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent instanceof PatientEvent) {
            PatientEvent otherPatientEvent = (PatientEvent) otherEvent;
            if (!StringUtils.equals(otherPatientEvent.getEventType(), this.eventType)) {
                return false;
            }
            return this.patient == null || this.patient.getDocument() == null
                || (otherPatientEvent.getPatient() != null && this.patient.getDocument().equals(
                    otherPatientEvent.getPatient().getDocument()));
        }
        return false;
    }

    @Override
    public String getEventType()
    {
        return this.eventType;
    }

    @Override
    public Patient getPatient()
    {
        return this.patient;
    }

    @Override
    public User getAuthor()
    {
        return this.author;
    }
}
