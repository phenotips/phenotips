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
package org.phenotips.data.permissions;

import org.xwiki.component.annotation.Role;
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
}
