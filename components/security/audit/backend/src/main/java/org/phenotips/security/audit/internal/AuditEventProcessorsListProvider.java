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
package org.phenotips.security.audit.internal;

import org.phenotips.security.audit.spi.AuditEventProcessor;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Provides a list of event processors valid in the current wiki. For security purposes, we don't allow more specific
 * event processors, since a user may register a custom event processor for his own profile, which automatically
 * replaces his name with someone else's name.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class AuditEventProcessorsListProvider implements Provider<List<AuditEventProcessor>>
{
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    @Override
    public List<AuditEventProcessor> get()
    {
        try {
            return this.componentManager.<AuditEventProcessor>getInstanceList(AuditEventProcessor.class);
        } catch (ComponentLookupException ex) {
            throw new RuntimeException("Failed to look up audit event processors", ex);
        }
    }
}
