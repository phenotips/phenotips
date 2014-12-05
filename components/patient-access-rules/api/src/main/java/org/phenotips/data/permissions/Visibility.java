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
}
