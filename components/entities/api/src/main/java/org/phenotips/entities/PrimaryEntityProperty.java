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
package org.phenotips.entities;

import org.xwiki.stability.Unstable;

/**
 * A property of a {@link PrimaryEntity}, which in turn is another type of entity. For example, a Project has an image,
 * a Patient has a template.
 *
 * @param <E> the type of the property
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New API introduced in 1.3")
public interface PrimaryEntityProperty<E extends PrimaryEntity> extends PrimaryEntity
{
    /**
     * Returns the property, if exists.
     *
     * @return a property, or null
     */
    E get();

    /**
     * Sets the property.
     *
     * @param property the property
     * @return {@code true} if the property was successfully set, or was already set, {@code false} if the
     *         operation failed
     */
    boolean set(E property);

    /**
     * Removes the property.
     *
     * @return {@code true} if the property was successfully removed, or if it wasn't set, {@code false} if the
     *         operation failed
     */
    boolean remove();

}
