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

import org.phenotips.security.audit.AuditEvent;
import org.phenotips.security.audit.AuditStore;
import org.phenotips.security.audit.spi.AuditEventProcessor;

import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.users.UserManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Event listener that logs an audit event for each executed action.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("auditor")
@Singleton
public class AuditEventListener extends AbstractEventListener
{
    @Inject
    private UserManager users;

    @Inject
    private AuditStore store;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Provider<List<AuditEventProcessor>> processors;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public AuditEventListener()
    {
        super("auditor", new ActionExecutedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        final ActionExecutedEvent aee = (ActionExecutedEvent) event;
        final XWikiDocument doc = (XWikiDocument) source;
        final XWikiRequest request = this.xcontextProvider.get().getRequest();
        final String ip = request != null ? request.getRemoteAddr() : null;

        AuditEvent auditEvent = new AuditEvent(this.users.getCurrentUser(), ip, aee.getActionName(), null,
            doc.getDocumentReference(), Calendar.getInstance(Locale.ROOT));

        for (AuditEventProcessor processor : this.processors.get()) {
            auditEvent = processor.process(auditEvent);
        }

        this.store.store(auditEvent);
    }
}
