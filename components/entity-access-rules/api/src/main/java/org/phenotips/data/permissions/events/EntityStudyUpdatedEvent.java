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
 * An event that is fired every time entity is assigned to a new study.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public class EntityStudyUpdatedEvent implements Event
{
    /** The affected entity id. */
    protected final String entityId;

    /** The affected study id. */
    protected final String studyId;

    /**
     * Constructor initializing the required fields.
     *
     * @param entityId the {@link PrimaryEntity#getId() identifier} of the affected entity
     * @param studyId the ID for the new study the entity is getting assigned to
     */
    public EntityStudyUpdatedEvent(String entityId, String studyId)
    {
        this.entityId = entityId;
        this.studyId = studyId;
    }

    /** Default constructor, to be used for declaring the events a listener wants to observe. */
    public EntityStudyUpdatedEvent()
    {
        this(null, null);
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent instanceof EntityStudyUpdatedEvent) {
            EntityStudyUpdatedEvent otherRightsUpdateEvent = (EntityStudyUpdatedEvent) otherEvent;
            return this.entityId == null
                || (StringUtils.equals(otherRightsUpdateEvent.getEntityId(), this.entityId) && StringUtils.equals(
                    otherRightsUpdateEvent.getStudyId(), this.studyId));
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
     * Returns the study Id for the entity being updated.
     *
     * @return the study identifier for the affected entity, or {@code null} if this isn't an actual event on an entity
     */
    public String getStudyId()
    {
        return this.studyId;
    }
}
