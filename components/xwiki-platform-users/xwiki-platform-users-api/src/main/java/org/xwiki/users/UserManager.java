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
package org.xwiki.users;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * User management APIs.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Role
public interface UserManager
{
    /**
     * The user corresponding to the identifier.
     *
     * @param identifier the user identifier to resolve: the username passed in the login form, a serialized identifier
     *            stored in the document's metadata, or an identifier passed by an external authentication service
     * @return the corresponding user, if found, or {@code null} otherwise
     */
    User getUser(String identifier);

    /**
     * The user corresponding to the identifier. If no existing user is found and the {@code force} parameter is
     * {@code true}, return a new user in the default user management system.
     *
     * @param identifier the user identifier to resolve: the username passed in the login form, a serialized identifier
     *            stored in the document's metadata, or an identifier passed by an external authentication service
     * @param force whether to force returning a new profile in case the user is not found
     * @return the corresponding user, if found; a new user profile if {@code force} is {@code true}; {@code null}
     *         otherwise
     */
    User getUser(String identifier, boolean force);

    /**
     * Get the currently logged in user.
     *
     * @return the currently logged in user, or {@code null} if no user is logged in
     */
    User getCurrentUser();
}
