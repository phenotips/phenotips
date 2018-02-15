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
package org.phenotips.locks.internal;

import org.phenotips.locks.DocumentLockManager;

import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.bridge.event.ActionExecutionEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The straight-forward implementation of the {@link DocumentLockManager} role.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("document-locking-listener")
@Singleton
public class LockingListener extends AbstractEventListener
{
    private static final List<String> SUPPORTED_EVENTS = Collections.unmodifiableList(
        Arrays.asList("get", "view", "save", "saveandcontinue", "preview", "objectadd", "objectremove", "rollback"));

    @Inject
    private DocumentLockManager lockManager;

    /** Basic constructor. */
    public LockingListener()
    {
        super("concurrency-locking", new ActionExecutingEvent(), new ActionExecutedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String name = ((ActionExecutionEvent) event).getActionName();
        if (!SUPPORTED_EVENTS.contains(name)) {
            return;
        }
        if (event instanceof ActionExecutingEvent) {
            this.lockManager.lock(((XWikiDocument) source).getDocumentReference());
        } else {
            this.lockManager.unlock(((XWikiDocument) source).getDocumentReference());
        }
    }
}
