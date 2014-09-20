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
package org.phenotips.storage.migrators;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Manages the migration of all known types of data, invoking each available {@link DataTypeMigrator}.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Unstable
@Role
public interface DataMigrationManager
{
    /**
     * Perform the data migration from all available, but not used, store types, into the currently used store for each
     * data type.
     *
     * @return {@code true} if all the data was successfully migrated, {@code false} in case of failure
     * @see DataTypeMigrator#migrate()
     */
    boolean migrate();
}
