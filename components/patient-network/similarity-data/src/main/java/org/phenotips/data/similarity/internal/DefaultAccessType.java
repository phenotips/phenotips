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
package org.phenotips.data.similarity.internal;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.similarity.AccessType;

/**
 * Straightforward implementation of {@link AccessType}.
 * 
 * @version $Id$
 * @since 1.0M9
 */
public class DefaultAccessType implements AccessType
{
    /** The "view" access level, the lower threshold for open access. */
    private AccessLevel view;

    /** The "match" access level, indicates partial access to data. */
    private AccessLevel match;

    /** @see #getAccessLevel() */
    private final AccessLevel access;

    /**
     * Simple constructor identifying the access level to wrap.
     * 
     * @param access the value to set for {@link #access}
     * @param view the value to set for {@link #view}, passed from outside since this is not a component
     * @param match the value to set for {@link #match}, passed from outside since this is not a component
     */
    public DefaultAccessType(AccessLevel access, AccessLevel view, AccessLevel match)
    {
        this.access = access;
        this.view = view;
        this.match = match;
    }

    @Override
    public AccessLevel getAccessLevel()
    {
        return this.access;
    }

    @Override
    public boolean isOpenAccess()
    {
        return this.access.compareTo(this.view) >= 0;
    }

    @Override
    public boolean isLimitedAccess()
    {
        return this.access.compareTo(this.match) == 0;
    }

    @Override
    public boolean isPrivateAccess()
    {
        return this.access.compareTo(this.match) < 0;
    }

    @Override
    public String toString()
    {
        return this.access.getName();
    }
}
