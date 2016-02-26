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
package org.xwiki.locks.script;

import org.xwiki.component.annotation.Component;
import org.xwiki.locks.DocumentLock;
import org.xwiki.locks.LockManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Singleton
@Named("locks")
public class LockScriptService implements ScriptService
{
    @Inject
    private LockManager lockManager;

    /**
     * Returns the lock objects of the specified document if it is locked.
     *
     * @param document the target document
     * @return {@link DocumentLock} if lock exists, {@code null} if cannot find a lock
     */
    public DocumentLock getLock(DocumentReference document)
    {
        if (document == null) {
            return null;
        }
        return this.lockManager.getLock(document);
    }
}
