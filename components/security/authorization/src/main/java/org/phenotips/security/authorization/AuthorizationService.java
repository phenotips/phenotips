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
package org.phenotips.security.authorization;

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

/**
 * Service which checks if a specific operation on a patient record should be granted or not. This can be implemented by
 * different components, each one with a different priority. When a specific access is requested, each of the available
 * implementations will be queried in descending order of their priority, until one responds with a non-null decision.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Role
public interface AuthorizationService extends Comparable<AuthorizationService>
{
    /**
     * The priority of this implementation. Implementations with a higher priority will be queried before
     * implementations with a lower priority. The base implementation has a priority of 0, and always returns a constant
     * decision, configured globally. The implementation which takes into account the access rights defined using global
     * ACLs has priority 100, and the (optional) implementation which takes into account collaboration settings has
     * priority 200. Custom implementations that decide based on other criterias, such as deferring to a remote
     * authorization server, should pick a higher priority.
     *
     * @return a positive number
     */
    int getPriority();

    /**
     * Checks if the specified user has the requested access level on the target patient.
     *
     * @param access the requested access level
     * @param user the user whose rights should be checked
     * @param patient the target patient
     * @return {@code True} if access is granted, {@code False} if access is denied, {@code null} if this component
     *         cannot determine if access should be granted or denied
     */
    Boolean hasAccess(Right access, User user, Patient patient);
}
