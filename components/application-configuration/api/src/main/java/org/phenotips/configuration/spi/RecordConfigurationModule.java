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
package org.phenotips.configuration.spi;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Modular record configuration service which provides or alters the record configuration according to a specific
 * purpose. This can be implemented by different components, each one with a different priority. When a specific record
 * configuration is requested, each of the available implementations will be queried by the
 * {@link RecordConfigurationManager} in ascending order of their priority, starting with an empty configuration that
 * can be altered by each module, and the final record configuration obtained after this process is considered the
 * active configuration to be displayed.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable("New API introduced in 1.4")
@Role
public interface RecordConfigurationModule
{
    /**
     * Configures how the record appears.
     *
     * @param config the previous configuration
     * @return an updated configuration
     */
    RecordConfiguration process(RecordConfiguration config);

    /**
     * The priority of this implementation. Implementations with a lower priority will be invoked before implementations
     * with a higher priority. The base implementation has a priority of 0, and returns the global configuration. It is
     * recommended that the returned values be in the [0..100] range.
     *
     * @return a positive number
     */
    int getPriority();

    /**
     * Indicates whether this configuration modules supports a specific type of record.
     *
     * @param recordType the string representing the type of record
     * @return true iff this module supports the specified record type, false otherwise
     */
    boolean supportsRecordType(String recordType);
}
