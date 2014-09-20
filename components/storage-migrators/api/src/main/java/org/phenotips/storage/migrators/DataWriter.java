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
 * Stores recovered data into the new storage engine.
 *
 * @param <T> the type of data managed by this writer, one of the classes from the data model
 * @version $Id$
 * @since 1.0RC1
 */
@Unstable
@Role
public interface DataWriter<T>
{
    /** @return an identifier for this writer, specifying the type of data managed and the supported storage */
    Type getType();

    /**
     * Store an entity in the new storage.
     *
     * @param entity the entity to store
     * @return {@code true} if the entity was successfully stored, {@code false} in case of failure
     */
    boolean storeEntity(T entity);
}
