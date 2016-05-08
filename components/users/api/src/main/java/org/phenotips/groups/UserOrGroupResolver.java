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
package org.phenotips.groups;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * A non-fully qualified user id (eg. 'userid') is ambiguous - it can refer to either a group or a user. This class
 * takes care of determining which reference should be used.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Unstable
@Role
public interface UserOrGroupResolver
{
    /**
     * Resolves an id to either a group or a user reference.
     *
     * @param id of either a user or a group
     * @return either a valid user/group reference, or {@link null}
     */
    EntityReference resolve(String id);
}
