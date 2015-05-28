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
package org.phenotips.storage.migrators;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Manages the migration of a specific type of data: reads the available data from all the implemented store types and
 * pushes it to the currently enabled store.
 * <p>
 * WARNING: Due to a limitation in the current implementation of the component manager, the stor
 * </p>
 *
 * @param <T> the type of data managed by this migrator, one of the classes from the data model
 * @version $Id$
 * @since 1.0RC1
 */
@Unstable("The implemented role will change (see the WARNING in the interface javadoc), "
    + "and a method to list data will be added.")
@Role
public interface DataTypeMigrator<T>
{
    /**
     * The type of data managed by this migrator.
     *
     * @return a short name for the data type, such as {@code "attachments"} or {@code "deleted attachments"}
     * @see Type#getDataType()
     */
    String getDataType();

    /**
     * Perform the data migration from all available, but not used, store types, into the store currently used for this
     * data type.
     *
     * @return {@code true} if all the data was successfully migrated, {@code false} in case of failure
     */
    boolean migrate();
}
