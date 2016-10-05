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
 * A property of a {@link PrimaryEntity}, which in turn is another type of entity. For example, a Project has an image,
 * a Patient has a template.
 *
 * @param <G> the type of the entity with the property
 * @param <E> the type of the property
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New API introduced in 1.3")
@Role
public interface PrimaryEntityProperty<G extends PrimaryEntity, E extends PrimaryEntity>
    extends PrimaryEntityGroupManager<G, E>
{
    /**
     * Returns the property, if exists.
     *
     * @param group to get the property from
     * @return a property, or null
     */
    E get(G group);

    /**
     * Sets the property.
     *
     * @param group to set the property to
     * @param property the property
     * @return {@code true} if the property was successfully set, or was already set, {@code false} if the
     *         operation failed
     */
    boolean set(G group, E property);

    /**
     * Removes the property.
     *
     * @param group to remove the property from
     * @return {@code true} if the property was successfully removed, or if it wasn't set, {@code false} if the
     *         operation failed
     */
    boolean remove(G group);

    /**
     * Returns all the groups that have a given {@link property}.
     *
     * @param property to look for
     * @return a collection of {@link G}-s
     */
    Collection<G> getGroupsForProperty(E property);
}
