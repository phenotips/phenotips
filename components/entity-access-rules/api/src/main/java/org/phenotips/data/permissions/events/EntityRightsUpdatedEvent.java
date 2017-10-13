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
package org.phenotips.data.permissions.events;

import org.phenotips.entities.PrimaryEntity;

import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;

import org.apache.commons.lang3.StringUtils;

/**
 * An event that is fired every time entity permissions are updated.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public class EntityRightsUpdatedEvent implements Event
{
    /** The affected entity id. */
    protected final String entityId;

    /**
     * Constructor initializing the required fields.
     *
     * @param entityId the {@link PrimaryEntity#getId() identifier} of the affected entity
     */
    public EntityRightsUpdatedEvent(String entityId)
    {
        this.entityId = entityId;
    }

    /** Default constructor, to be used for declaring the events a listener wants to observe. */
    public EntityRightsUpdatedEvent()
    {
        this(null);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent instanceof EntityRightsUpdatedEvent) {
            EntityRightsUpdatedEvent otherRightsUpdateEvent = (EntityRightsUpdatedEvent) otherEvent;
            return this.entityId == null || StringUtils.equals(otherRightsUpdateEvent.getEntityId(), this.entityId);
        }
        return false;
    }

    /**
     * Returns the {@link PrimaryEntity#getId() identifier} of the entity being updated.
     *
     * @return the {@link PrimaryEntity#getId() identifier} of the affected entity, or {@code null} if this isn't an
     *         actual event on an entity
     */
    public String getEntityId()
    {
        return this.entityId;
    }
}
