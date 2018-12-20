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

import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;

import org.apache.commons.lang3.StringUtils;

/**
 * An event that is fired every time entity is linked to another entity.
 *
 * @version $Id$
 * @since 1.5M1
 */
@Unstable
public class EntitiesLinkedEvent implements Event
{
    /** The affected entity id. */
    protected final String subjectEntityId;

    /** The affected entity id. */
    protected final String subjectEntitySpace;

    /** The linked to entity id. */
    protected final String linkedToEntityId;

    /** The linked to entity space. */
    protected final String linkedToEntitySpace;

    /**
     * Constructor initializing the required fields.
     *
     * @param entityId the identifier of the affected entity
     * @param entitySpace the space of the affected entity
     * @param linkedToEntityId the identifier for the entity is getting linked to
     * @param linkedToEntitySpace the space for the entity is getting linked to
     */
    public EntitiesLinkedEvent(String entityId, String entitySpace, String linkedToEntityId, String linkedToEntitySpace)
    {
        this.subjectEntityId = entityId;
        this.subjectEntitySpace = entitySpace;
        this.linkedToEntityId = linkedToEntityId;
        this.linkedToEntitySpace = linkedToEntitySpace;
    }

    /** Default constructor, to be used for declaring the events a listener wants to observe. */
    public EntitiesLinkedEvent()
    {
        this(null, null, null, null);
    }

    @Override
    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    public boolean matches(Object otherEvent)
    {
        if (otherEvent instanceof EntitiesLinkedEvent) {
            EntitiesLinkedEvent otherLinkedEvent = (EntitiesLinkedEvent) otherEvent;
            return this.subjectEntityId == null
                || (StringUtils.equals(otherLinkedEvent.getSubjectEntityId(), this.subjectEntityId)
                    && StringUtils.equals(otherLinkedEvent.getSubjectEntitySpace(), this.subjectEntitySpace)
                    && StringUtils.equals(otherLinkedEvent.getLinkedToEntityId(), this.linkedToEntityId)
                    && StringUtils.equals(otherLinkedEvent.getLinkedToEntitySpace(), this.linkedToEntitySpace));
        }
        return false;
    }

    /**
     * Returns the identifier of the entity being linked.
     *
     * @return the identifier of the affected entity ID, or {@code null} if this isn't an actual event on an entity
     */
    public String getSubjectEntityId()
    {
        return this.subjectEntityId;
    }

    /**
     * Returns the space of the entity being linked.
     *
     * @return the space of the affected entity, or {@code null} if this isn't an actual event on an entity
     */
    public String getSubjectEntitySpace()
    {
        return this.subjectEntitySpace;
    }

    /**
     * Returns the ID for the linked to entity.
     *
     * @return the identifier for the linked to entity, or {@code null} if this isn't an actual event on an entity
     */
    public String getLinkedToEntityId()
    {
        return this.linkedToEntityId;
    }

    /**
     * Returns the space for the linked to entity.
     *
     * @return the space for the linked to entity, or {@code null} if this isn't an actual event on an entity
     */
    public String getLinkedToEntitySpace()
    {
        return this.linkedToEntitySpace;
    }
}
