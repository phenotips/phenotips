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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Iterator;

/**
 * Retrieves existing data from an old storage engine.
 *
 * @param <T> the type of data managed by this reader, one of the classes from the data model
 * @version $Id$
 * @since 1.0RC1
 */
@Unstable
@Role
public interface DataReader<T>
{
    /** @return an identifier for this reader, specifying the type of data managed and the supported storage */
    Type getType();

    /** @return {@code true} if there is data in this storage engine */
    boolean hasData();

    /**
     * Lists all the data available in this store.
     *
     * @return references identifying the data
     */
    Iterator<EntityReference> listData();

    /**
     * Retrieves the data available in this store.
     *
     * @return the available data; the returned iterator does not support {@link Iterator#remove() removing data} (use
     *         {@link #discardEntity(Object)} for that), and in case an entity failed to be retrieved from the store,
     *         {@code null} might be returned by {@link Iterator#next()} in place of the actual entity
     */
    Iterator<T> getData();

    /**
     * Permanently deletes an entity from this store.
     *
     * @param entity the entity to delete
     * @return {@code true} if the entity was successfully deleted, {@code false} in case of failure
     */
    boolean discardEntity(T entity);

    /**
     * Permanently deletes all the entities (of the managed type) from this store.
     *
     * @return {@code true} if the data was successfully deleted, {@code false} in case of failure
     */
    boolean discardAllData();
}
