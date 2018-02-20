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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * Implementation for the {@link DocumentLockManager} role which will accept a lock request even if the lock couldn't be
 * obtained when a timeout interval (10 seconds) has ellapsed.
 *
 * @version $Id$
 * @since 1.3.7
 * @since 1.4
 */
@Component
@Singleton
public class TimeoutDocumentLockManager implements DocumentLockManager
{
    @Inject
    private Logger logger;

    private final ConcurrentHashMap<DocumentReference, Lock> locks = new ConcurrentHashMap<>();

    @Override
    public void lock(@Nonnull final DocumentReference document)
    {
        try {
            final Lock lock = this.locks.computeIfAbsent(document, k -> new StampedLock().asWriteLock());
            final boolean cleanLock = lock.tryLock(10, TimeUnit.SECONDS);
            if (!cleanLock) {
                this.logger.debug("Timed out while waiting for lock on [{}], proceeding anyway", document);
            }
        } catch (InterruptedException ex) {
            // We don't expect any interruptions
            this.logger.error("Unexpected interruption why waiting for lock: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void unlock(@Nonnull final DocumentReference document)
    {
        Lock lock = this.locks.get(document);
        if (lock == null) {
            return;
        }
        try {
            lock.unlock();
        } catch (IllegalMonitorStateException ex) {
            // Unlocking may fail if a lock timeout resulted in an unclean lock, and then two requests try to unlock
            this.logger.debug("Lock was unexpectedly unlocked already: {}", ex.getMessage(), ex);
        }
    }
}
