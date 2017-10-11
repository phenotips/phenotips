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

import org.phenotips.Constants;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Role
public interface Visibility extends Comparable<Visibility>
{
    EntityReference CLASS_REFERENCE = new EntityReference("VisibilityClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    String getName();

    String getLabel();

    String getDescription();

    /**
     * The level of access this visibility grants to non-collaborators; lower values mean more restrictions, higher
     * values mean more permissions. Should be a number between 0 (no access) and 100 (full access). If a visibility
     * {@code V80} has a higher permissiveness than another visibility {@code V60}, then it must grant all the access
     * that {@code V60} grants, plus some more, thus the visibility options are strictly comparable.
     *
     * @return a number between {@code 0} and {@code 100}
     * @since 1.1M1
     */
    int getPermissiveness();

    AccessLevel getDefaultAccessLevel();

    /**
     * @return {@code true} if this visibility is disabled in the configuration, and shouldn't be available as an option
     *         when choosing the visibility of a record
     * @since 1.3M2
     */
    boolean isDisabled();
}
