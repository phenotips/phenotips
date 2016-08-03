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
package org.phenotips.security.authorization;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;

/**
 * Modular authorization service which checks if a specific operation on an entity should be granted or not. This can be
 * implemented by different components, each one with a different priority. When a specific access is requested, each of
 * the available implementations will be queried by the {@link AuthorizationService} in descending order of their
 * priority, until one responds with a non-null decision.
 *
 * @version $Id$
 * @since 1.0M13
 */
@Unstable
@Role
public interface AuthorizationModule
{
    /**
     * The priority of this implementation. Implementations with a higher priority will be queried before
     * implementations with a lower priority. The base implementation has a priority of 0, and always returns a constant
     * decision, configured globally. The implementation which takes into account the access rights defined using ACLs
     * has priority 100. Custom implementations that decide based on other criteria, such as deferring to a remote
     * authorization server, should pick a higher priority.
     *
     * @return a positive number
     */
    int getPriority();

    /**
     * Checks if the specified user has the requested access level on the target entity.
     *
     * @param user the user whose rights should be checked
     * @param access the requested access level
     * @param entity the target entity (document, space, wiki...)
     * @return {@code True} if access is granted, {@code False} if access is denied, {@code null} if this module cannot
     *         determine if access should be granted or denied
     */
    Boolean hasAccess(User user, Right access, EntityReference entity);
}
