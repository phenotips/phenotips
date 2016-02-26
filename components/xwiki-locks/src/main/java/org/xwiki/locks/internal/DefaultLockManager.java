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
package org.xwiki.locks.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.locks.DocumentLock;
import org.xwiki.locks.LockManager;
import org.xwiki.locks.LockModule;
import org.xwiki.model.reference.DocumentReference;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Singleton
public class DefaultLockManager implements LockManager
{
    @Inject
    private Logger logger;

    /** Provides the list of all the available lock modules, which perform the actual lock checking. */
    @Inject
    private Provider<List<LockModule>> managers;

    @Override
    public DocumentLock getLock(DocumentReference document)
    {
        for (LockModule service : this.managers.get()) {
            try {
                DocumentLock lock = service.getLock(document);
                if (lock != null) {
                    return lock;
                }
            } catch (Exception ex) {
                // Don't fail because of bad authorization modules
                this.logger.warn("Failed to invoke locking manager [{}]: {}",
                    service.getClass().getCanonicalName(), ex.getMessage());
            }
        }
        return null;
    }
}
