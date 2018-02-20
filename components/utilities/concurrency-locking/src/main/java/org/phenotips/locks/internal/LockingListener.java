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
 * An event listener that only allows one action request to proceed at a time for the same document. When an action
 * starts executing, a lock is aquired for the affected document, and when the action terminates, the lock is released.
 * If a lock is already held by an action execution, the subsequent actions will block while waiting for the lock to be
 * released. The purpose of this mechanism is to prevent concurrent document updates, which may cause inconsistent data,
 * hibernate stale state exceptions, unique key conflicts, or other storage errors. This isn't the best way to prevent
 * such errors, but properly fixing the concurrency problems of XWiki requires much deeper and broader fixes throughout
 * the old core and any custom code updating documents.
 * <p>
 * Implementation note: the {@code get} and {@code view} methods should theoretically not be locked, since they don't
 * normally modify data, but at the moment there are still legacy scripts that are accessed in view mode but do modify
 * their or other documents' data, such as {@code OpenPatientRecord}, so these actions must also be locked.
 * </p>
 *
 * @version $Id$
 * @since 1.3.7
 * @since 1.4
 */
@Component
@Named("concurrency-locking")
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
        if (event == null || source == null || !(event instanceof ActionExecutionEvent)) {
            return;
        }
        String name = ((ActionExecutionEvent) event).getActionName();
        if (!SUPPORTED_EVENTS.contains(name)) {
            return;
        }
        if (event instanceof ActionExecutingEvent) {
            this.lockManager.lock(((XWikiDocument) source).getDocumentReference());
        } else if (event instanceof ActionExecutedEvent) {
            this.lockManager.unlock(((XWikiDocument) source).getDocumentReference());
        }
    }
}
