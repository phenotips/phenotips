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
package org.phenotips.entities.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityProperty;

import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Iterator;

/**
 * Base class for implementing PrimaryEntityProperty. The implementation is done as a container of one object.
 *
 * @param <G> the type of entity with the property
 * @param <E> the type of entity of the property
 * @version $Id$
 * @since 1.3M2
 */
public abstract class AbstractPrimaryEntityProperty<G extends PrimaryEntity, E extends PrimaryEntity>
    extends AbstractContainerPrimaryEntityGroup<G, E>
    implements PrimaryEntityProperty<G, E>
{
    protected AbstractPrimaryEntityProperty(
            EntityReference groupEntityReference, EntityReference memberEntityReference)
    {
        super(groupEntityReference, memberEntityReference);
    }

    @Override
    public E get(G group)
    {
        Collection<E> members = this.getMembers(group);
        Iterator<E> iterator = members.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            return null;
        }
    }

    @Override
    public boolean set(G group, E property)
    {
        boolean removed = this.remove(group);
        if (property == null || !removed) {
            return removed;
        }
        return addMember(group, property);
    }

    @Override
    public boolean remove(G group)
    {
        E property = this.get(group);
        if (property == null) {
            return true;
        } else {
            return this.removeMember(group, property);
        }
    }

    @Override
    public Collection<G> getGroupsForProperty(E property)
    {
        return super.getGroupsForMember(property);
    }
}
