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

import org.xwiki.stability.Unstable;

import java.util.Locale;

/**
 * The possible types of access a user can have to other patient profiles.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public enum AccessType
{
    /** The user's own patient. */
    OWNED(true, false),
    /** A patient from one of the user's consortia. */
    GROUP_OWNED(true, false),
    /** A publicly shared patient from another organization. */
    PUBLIC(true, false),
    /** A matchable or private patient that has been explicitly shared with the user. */
    SHARED(true, false),
    /** A matchable patient. */
    MATCH(false, true),
    /** A private patient, this shouldn't be listed at all. */
    PRIVATE(false, false);

    /** @see #isOpenAccess() */
    private final boolean openAccess;

    /** @see #isLimitedAccess() */
    private final boolean limitedAccess;

    /**
     * Simple constructor identifying if the user has full or limited access.
     * 
     * @param openAccess the value to set for {@link #openAccess}
     * @param limitedAccess the value to set for {@link #limitedAccess}
     */
    AccessType(boolean openAccess, boolean limitedAccess)
    {
        this.openAccess = openAccess;
        this.limitedAccess = limitedAccess;
    }

    /**
     * Indicates full access to the patient's data.
     * 
     * @return {@code true} if the patient has full access to the patient, {@code false} otherwise
     */
    public boolean isOpenAccess()
    {
        return this.openAccess;
    }

    /**
     * Indicates limited, obfuscated access to the patient's data.
     * 
     * @return {@code true} if the patient has only limited access to the patient, {@code false} otherwise
     */
    public boolean isLimitedAccess()
    {
        return this.limitedAccess;
    }

    /**
     * Indicates no access to the patient's data.
     * 
     * @return {@code true} if the patient has no access to the patient, {@code false} otherwise
     */
    public boolean isPrivateAccess()
    {
        return !(this.openAccess || this.limitedAccess);
    }

    @Override
    public String toString()
    {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
