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

import java.util.List;

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
    /**
     * An enum of all the possible entity permissions event types.
     *
     * @since 1.4
     */
    public enum RightsUpdateEventType
    {
        /** Entity visibility updated event type. */
        ENTITY_VISIBILITY_UPDATED,
        /** Entity owner updated event type. */
        ENTITY_OWNER_UPDATED,
        /** Entity collaborators updated event type. */
        ENTITY_COLLABORATORS_UPDATED;
    }

    /** The affected entity id. */
    protected final String entityId;

    /** The types of this event. */
    protected final List<RightsUpdateEventType> eventTypes;

    /**
     * Constructor initializing the required fields.
     *
     * @param eventTypes the types of this event
     * @param entityId the {@link PrimaryEntity#getId() identifier} of the affected entity
     */
    public EntityRightsUpdatedEvent(List<RightsUpdateEventType> eventTypes, String entityId)
    {
        this.entityId = entityId;
        this.eventTypes = eventTypes;
    }

    /** Default constructor, to be used for declaring the events a listener wants to observe. */
    public EntityRightsUpdatedEvent()
    {
        this(null, null);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent instanceof EntityRightsUpdatedEvent) {
            EntityRightsUpdatedEvent otherRightsUpdateEvent = (EntityRightsUpdatedEvent) otherEvent;
            return this.entityId == null
                || (StringUtils.equals(otherRightsUpdateEvent.getEntityId(), this.entityId) && otherRightsUpdateEvent
                    .getEventTypes().equals(this.eventTypes));
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

    /**
     * Identifies the types of rights update actions performed on the entity record.
     *
     * @return a list of {@link RightsUpdateEventType}s
     */
    public List<RightsUpdateEventType> getEventTypes()
    {
        return this.eventTypes;
    }
}
