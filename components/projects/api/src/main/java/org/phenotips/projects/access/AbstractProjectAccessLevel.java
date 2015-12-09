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
package org.phenotips.projects.access;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.internal.AbstractAccessLevel;

/**
 * @version $Id$
 */
public abstract class AbstractProjectAccessLevel extends AbstractAccessLevel implements ProjectAccessLevel
{

    protected AbstractProjectAccessLevel(int permissiveness, boolean assignable)
    {
        super(permissiveness, assignable);
    }

    @Override
    public int compareTo(AccessLevel o)
    {
        // Comparing AbstractProjectAccessLevel with same. AbstractProjectAccessLevel cannot be compared to
        // AbstractAccessLevel.
        if (o != null && o instanceof AbstractProjectAccessLevel) {
            return this.compareTo(o);
        }
        return Integer.MIN_VALUE;
    }
}
