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
package org.phenotips.security.audit.spi;

import org.phenotips.security.audit.AuditEvent;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import javax.annotation.Nonnull;

/**
 * A module that can further process an audit event, since some actions may be more complex than a simple action name,
 * such as "get", can explain. For example, this may record the format of an export action, or change the actual target
 * of an event when a document is used to retrieve or modify the contents of another document.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable("New SPI added in 1.4")
@Role
public interface AuditEventProcessor
{
    /**
     * Process an audit event. If the event needs to be modified, a new event should be returned, based on the input
     * event, with new values as needed. Otherwise, the input event must be returned unchanged.
     *
     * @param event the event to process
     * @return the same or another event
     */
    @Nonnull
    AuditEvent process(@Nonnull AuditEvent event);
}
