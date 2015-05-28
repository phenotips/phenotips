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
package org.phenotips.configuration;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Provides access to {@code RecordConfiguration patient record configurations}.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Role
public interface RecordConfigurationManager
{
    /**
     * Retrieves the {@code RecordConfiguration patient record configuration} active for the current user.
     *
     * @return a valid configuration, either the global one or one configured, for example in one of the user's groups
     */
    RecordConfiguration getActiveConfiguration();
}
