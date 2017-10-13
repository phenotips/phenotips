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
package org.phenotips.data.permissions.internal.access;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.internal.AbstractAccessLevel;

import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */
@Component
@Named("none")
@Singleton
public class NoAccessLevel extends AbstractAccessLevel
{
    /** Simple constructor. */
    public NoAccessLevel()
    {
        super(0, false);
    }

    @Override
    public String getName()
    {
        return "none";
    }

    @Override
    public int compareTo(AccessLevel o)
    {
        // Overridden so that this access level is considered less permissive than null and unknown access levels
        if (o instanceof AbstractAccessLevel) {
            // For comparisons with other AbstractAccessLevel-based levels, the default implementation is correct
            return super.compareTo(o);
        }
        // For other cases, this is to be considered the least permissive level.
        return Integer.MIN_VALUE;
    }
}
