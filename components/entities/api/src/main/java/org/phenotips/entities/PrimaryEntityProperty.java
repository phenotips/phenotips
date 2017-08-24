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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;

/**
 * Represents a "property" of a {@link PrimaryEntity}, which is in turn a {@link PrimaryEntity} itself.
 * For example, a Project has a (unique) image, a Patient has a (unique) template.
 * <p>
 * This is equivalent to a group containing just one member, and the interface is provided for convenience.
 * Note that many different entities may have the same property (e.g. many patients have the same template).
 *
 * @param <E> the type of the entity with the property
 * @param <P> the type of the property
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New API introduced in 1.3")
@Role
public interface PrimaryEntityProperty<E extends PrimaryEntity, P extends PrimaryEntity>
{
    /**
     * Returns the property, if exists.
     *
     * @param entity to get the property from
     * @return a property, or null
     */
    P get(E entity);

    /**
     * Sets the property.
     *
     * @param entity to set the property to
     * @param property the property
     * @return {@code true} if the property was successfully set, or was already set, {@code false} if the
     *         operation failed
     */
    boolean set(E entity, P property);

    /**
     * Removes the property.
     *
     * @param entity to remove the property from
     * @return {@code true} if the property was successfully removed, or if it wasn't set, {@code false} if the
     *         operation failed
     */
    boolean remove(E entity);

    /**
     * Returns all the groups that have the given {@link property}.
     *
     * @param property to look for
     * @return a collection of {@link G}-s
     */
    Collection<E> getEntitiesForProperty(P property);
}
