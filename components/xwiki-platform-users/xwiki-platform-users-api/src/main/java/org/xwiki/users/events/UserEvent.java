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

import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;

/**
 * Represents all events affecting users: creation, modification, deletion.
 *
 * @version $Id$
 * @since 1.1M1
 */
@Unstable
public interface UserEvent extends Event
{
    /**
     * Identifies the type of action performed on the user.
     *
     * @return a short string, e.g. {@code userCreated}
     */
    String getEventType();

    /**
     * The affected user.
     *
     * @return the new version of the user for events notifying of a change, or the last version if this is a deletion
     *         event
     */
    User getUser();

    /**
     * The user performing this action. Normally user operations are performed either by guests (registering a new
     * account, requesting a password reset), or the same user (editing account details), but they can also be performed
     * by an administrative account (deactivating an account), so for accountability the user performing an action
     * should also be recorded.
     *
     * @return a user reference, may be a real user, the special "system" user, or an anonymous user
     */
    User getAuthor();
}
