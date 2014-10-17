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
package org.xwiki.users.events;

import org.xwiki.users.User;

import org.apache.commons.lang3.StringUtils;

/**
 * Base class for implementing {@link UserEvent}.
 *
 * @version $Id$
 * @since 1.1M1
 */
public abstract class AbstractUserEvent implements UserEvent
{
    /** @see #getEventType() */
    protected final String eventType;

    /** @see #getUser() */
    protected final User user;

    /** @see #getAuthor() */
    protected final User author;

    /**
     * Constructor initializing the required fields.
     *
     * @param eventType the type of this event
     * @param user the affected user
     * @param author the user performing this action
     */
    protected AbstractUserEvent(String eventType, User user, User author)
    {
        this.eventType = eventType;
        this.user = user;
        this.author = author;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent instanceof UserEvent) {
            UserEvent otherPatientEvent = (UserEvent) otherEvent;
            if (!StringUtils.equals(otherPatientEvent.getEventType(), this.eventType)) {
                return false;
            }
            return this.user == null || this.user.getProfileDocument() == null
                || (otherPatientEvent.getUser() != null
                && this.user.getProfileDocument().equals(otherPatientEvent.getUser().getProfileDocument()));
        }
        return false;
    }

    @Override
    public String getEventType()
    {
        return this.eventType;
    }

    @Override
    public User getUser()
    {
        return this.user;
    }

    @Override
    public User getAuthor()
    {
        return this.author;
    }
}
