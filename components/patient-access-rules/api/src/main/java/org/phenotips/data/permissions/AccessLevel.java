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
package org.phenotips.data.permissions;

import org.xwiki.component.annotation.Role;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * A specific access level that a user or a group can have on a patient record, determining which operations are allowed
 * on that record.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Role
public interface AccessLevel extends Comparable<AccessLevel>
{
    /**
     * The internal name of this access level.
     *
     * @return a short lowercase identifier
     */
    String getName();

    /**
     * The user-friendly name of this access level which can be displayed to the users.
     *
     * @return a short titlecase name
     */
    String getLabel();

    /**
     * A descriptive label of this access level which can be displayed to the users to describe this access level.
     *
     * @return a long sentence describing the access level
     */
    String getDescription();

    /**
     * Some levels can be explicitly granted to {@link Collaborator}s, while others are implicit. This method indicates
     * which access levels can be assigned to collaborators.
     *
     * @return {@code true} if this level can be assigned to collaborators, {@code false} otherwise
     */
    boolean isAssignable();

    /**
     * The equivalent XWiki right granted by this access level.
     *
     * @return an equivalent right
     */
    Right getGrantedRight();
}
