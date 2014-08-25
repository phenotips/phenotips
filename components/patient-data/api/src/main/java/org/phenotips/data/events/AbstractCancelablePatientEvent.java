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

import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.users.User;

/**
 * Base class for implementing cancelable pre-notification {@link PatientEvent}s.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public abstract class AbstractCancelablePatientEvent extends AbstractPatientEvent implements CancelableEvent
{
    /** @see #isCanceled() */
    protected boolean canceled;

    /** @see #getReason() */
    protected String cancelReason;

    /**
     * Constructor initializing the required fields.
     *
     * @param eventType the type of this event
     * @param patient the affected patient
     * @param author the user performing this action
     */
    protected AbstractCancelablePatientEvent(String eventType, Patient patient, User author)
    {
        super(eventType, patient, author);
    }

    @Override
    public boolean isCanceled()
    {
        return this.canceled;
    }

    @Override
    public void cancel()
    {
        cancel(null);
    }

    @Override
    public void cancel(String reason)
    {
        this.canceled = true;
        this.cancelReason = reason;
    }

    @Override
    public String getReason()
    {
        return this.cancelReason;
    }
}
