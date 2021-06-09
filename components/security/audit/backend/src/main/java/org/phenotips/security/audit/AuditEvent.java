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
package org.phenotips.security.audit;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.users.User;

import java.util.Calendar;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.json.JSONObject;

/**
 * An audit event represents an action that a user performed.
 *
 * @version $Id$
 * @since 1.4
 */
@Entity
public class AuditEvent
{
    /** Unique identifier, needed for persistence. */
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "uuid", length = 16)
    @Type(type = "uuid-binary")
    private UUID uuid;

    @Type(type = "org.phenotips.security.audit.internal.UserType")
    @Column(name = "userRef")
    private User user;

    private String ip;

    private String action;

    @Type(type = "text")
    private String extra;

    @Type(type = "org.phenotips.security.audit.internal.DocumentReferenceType")
    private DocumentReference entity;

    private Calendar time;

    /** Default constructor used by Hibernate. */
    public AuditEvent()
    {
        // Nothing to do, Hibernate will populate all the fields from the database
    }

    /**
     * Constructor passing all the event data.
     *
     * @param user the user performing the action
     * @param ip the ip where the request came from; may be a proxy IP, not the real user's IP
     * @param action the executed action, for example {@code view}, {@code edit} or {@code export}
     * @param extra custom extra information about the action, for example the export format, the parameters of a
     *            livetable request, etc
     * @param entity the affected entity
     * @param time the time of the action
     */
    public AuditEvent(final User user, final String ip,
        final String action, final String extra,
        final DocumentReference entity,
        final Calendar time)
    {
        this.user = user;
        this.ip = ip;
        this.action = action;
        this.extra = extra;
        this.entity = entity;
        this.time = time;
    }

    /**
     * The user that performed this action.
     *
     * @return a user, may be {@code null} if the action was performed by an unauthenticated user
     */
    @Nullable
    public User getUser()
    {
        return this.user;
    }

    /**
     * The ip where the request came from. This may be a proxy IP, not the real user's IP.
     *
     * @return an IP address, may be {@code null} if the IP cannot be determined
     */
    @Nullable
    public String getIp()
    {
        return this.ip;
    }

    /**
     * The executed action.
     *
     * @return a simple action name, for example {@code view}, {@code edit} or {@code export}.
     */
    @Nonnull
    public String getAction()
    {
        return this.action;
    }

    /**
     * Custom extra information about the action, for example the export format, the parameters of a livetable request,
     * etc.
     *
     * @return any extra information about this action, may be {@code null}
     */
    @Nullable
    public String getExtraInformation()
    {
        return this.extra;
    }

    /**
     * The entity affected by the action. In case the action doesn't affect an entity, for example a user logging in, no
     * entity is considered affected.
     *
     * @return a document reference, may be {@code null} for global actions not affecting an entity
     */
    @Nullable
    public DocumentReference getEntity()
    {
        return this.entity;
    }

    /**
     * The time of the action.
     *
     * @return a time reference
     */
    @Nonnull
    public Calendar getTime()
    {
        return this.time;
    }

    @Override
    public String toString()
    {
        return (this.user == null ? null : this.user.getId()) + " (" + this.ip + "): "
            + this.action + " on " + this.entity + " at "
            + (this.time == null ? null : this.time.getTime());
    }

    @Override
    public boolean equals(Object other)
    {
        return EqualsBuilder.reflectionEquals(this, other, false);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }

    /**
     * Returns the JSON representation of the audit event.
     *
     * @return a JSON object.
     */
    public JSONObject toJSON()
    {
        JSONObject event = new JSONObject();
        event.put("user", this.user == null ? null : this.user.getId());
        event.put("ip", this.ip);
        event.put("action", this.action);
        event.put("extra", this.extra);
        event.put("entity", this.entity == null ? null : this.entity.toString());
        event.put("time", this.time == null ? null : this.time.toInstant().toString());
        return event;
    }
}
