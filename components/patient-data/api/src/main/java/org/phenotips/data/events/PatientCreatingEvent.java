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

/**
 * Notifies that a new patient record is just being created. Canceling this event should prevent the patient creation,
 * although this behavior isn't implemented yet.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class PatientCreatingEvent extends AbstractCancelablePatientEvent
{
    /**
     * Simple constructor passing all the required information.
     *
     * @param patient the new patient being created
     * @param author the user performing this action
     */
    public PatientCreatingEvent(Patient patient, User author)
    {
        super("patientRecordCreating", patient, author);
    }

    /** Default constructor, to be used for declaring the events a listener wants to observe. */
    public PatientCreatingEvent()
    {
        this(null, null);
    }
}
