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
package org.phenotips.data.similarity;

import org.phenotips.data.permissions.AccessLevel;

import org.xwiki.stability.Unstable;

/**
 * Helper class providing quick information about an {@link AccessLevel access level}.
 * 
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
public interface AccessType
{
    /**
     * The real level of access the user has on the patient's data.
     * 
     * @return the computed access level, can not be {@code null}
     */
    AccessLevel getAccessLevel();

    /**
     * Indicates full access to the patient's data.
     * 
     * @return {@code true} if the patient has full access to the patient data, {@code false} otherwise
     */
    boolean isOpenAccess();

    /**
     * Indicates limited, obfuscated access to the patient's data.
     * 
     * @return {@code true} if the patient has only limited access to the patient data, {@code false} otherwise
     */
    boolean isLimitedAccess();

    /**
     * Indicates no access to the patient's data.
     * 
     * @return {@code true} if the patient has no access to the patient data, {@code false} otherwise
     */
    boolean isPrivateAccess();
}
