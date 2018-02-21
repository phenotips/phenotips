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
package org.phenotips.entities.configuration;

import org.xwiki.stability.Unstable;

import java.util.Locale;

/**
 * Options that affect the behavior of a {@link RecordSection}.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public enum RecordSectionOption
{
    /** Whether this section should be expanded and fully visible in edit mode by default. */
    EXPANDED_BY_DEFAULT;

    @Override
    public String toString()
    {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
