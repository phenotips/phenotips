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

import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;

/**
 * Represents all events affecting whole patient records: creation, modification, deletion.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Unstable
public interface PatientEvent extends Event
{
    /**
     * Identifies the type of action performed on the patient record.
     *
     * @return a short string, e.g. {@code patientRecordCreated}
     */
    String getEventType();

    /**
     * The affected patient record.
     *
     * @return the new version of the patient record for events notifying of a change, or the last version if this is a
     *         deletion event
     */
    Patient getPatient();

    /**
     * The user performing this action.
     *
     * @return a user reference, may be a real user, the special "system" user, or an anonymous user
     */
    User getAuthor();
}
