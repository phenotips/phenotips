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
package org.phenotips.security.encryption;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Retrieves the system password using a configured mechanism.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New API introduced in 1.3")
@Role
public interface SystemPasswordConfiguration
{
    /**
     * A specific way of configuring the system password, for example from a configuration file on the filesystem, a
     * prompt during client requests, or fetching from a remote host. Only one module may be enabled at a time.
     *
     * @version $Id$
     * @since 1.3M2
     */
    @Unstable("New API introduced in 1.3")
    @Role
    interface ConfigurationModule
    {
        /**
         * Get the system password configured for this module, if any.
         *
         * @return a password, or {@code null} if this module isn't set up with a proper password
         */
        String getSystemPassword();
    }

    /**
     * Get the configured system password.
     *
     * @return a password
     * @throws IllegalStateException if the configuration is wrong
     */
    String getSystemPassword() throws IllegalStateException;
}
