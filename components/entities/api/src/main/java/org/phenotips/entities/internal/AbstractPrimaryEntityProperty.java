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
 * Base class for implementing PrimaryEntityProperty. The implementation is done as a Group of one object.
 * <p>
 * It is {@code abstract} because an actual implementation is supposed to be a component with appropriate annotations -
 * other than that it can be instantiated and should work as is.
 *
 * @param <E> the type of entity with the property
 * @param <P> the type of entity of the property
 * @version $Id$
 * @since 1.3M2
 */
public abstract class AbstractPrimaryEntityProperty<E extends PrimaryEntity, P extends PrimaryEntity>
    implements PrimaryEntityProperty<E, P>
{
    private AbstractInternalPrimaryEntityGroupManager<E, P> manager;

    protected AbstractPrimaryEntityProperty(EntityReference groupEntityReference, EntityReference memberEntityReference)
    {
        this.manager = new AbstractInternalPrimaryEntityGroupManager<E, P>(groupEntityReference, memberEntityReference);
    }

    @Override
    public P get(E entity)
    {
        Collection<P> members = this.manager.getMembers(entity);
        Iterator<P> iterator = members.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            return null;
        }
    }

    @Override
    public boolean set(E entity, P property)
    {
        boolean removed = this.remove(entity);
        if (property == null || !removed) {
            return removed;
        }
        return this.manager.addMember(entity, property);
    }

    @Override
    public boolean remove(E entity)
    {
        P property = this.get(entity);
        if (property == null) {
            return true;
        } else {
            return this.manager.removeMember(entity, property);
        }
    }

    @Override
    public Collection<E> getEntitiesForProperty(P property)
    {
        return this.manager.getGroupsForMember(property);
    }
}
