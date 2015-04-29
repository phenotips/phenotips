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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.data.events;

import org.phenotips.data.Patient;

import org.xwiki.users.User;

/**
 * Notifies that a patient record is being deleted. Canceling this event should prevent the deletion, although this
 * behavior isn't implemented yet.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class PatientDeletingEvent extends AbstractCancelablePatientEvent
{
    /**
     * Simple constructor passing all the required information.
     *
     * @param patient the patient being deleted
     * @param author the user performing this action
     */
    public PatientDeletingEvent(Patient patient, User author)
    {
        super("patientRecordDeleting", patient, author);
    }

    /** Default constructor, to be used for declaring the events a listener wants to observe. */
    public PatientDeletingEvent()
    {
        this(null, null);
    }
}
