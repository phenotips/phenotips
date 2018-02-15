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

import javax.inject.Singleton;

/**
 * Implementation for the {@link DocumentLockManager} role which will accept a lock request even if the lock couldn't be
 * obtained when a timeout interval (10 seconds) has ellapsed.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class TimeoutDocumentLockManager implements DocumentLockManager
{
    private ConcurrentHashMap<DocumentReference, Lock> locks = new ConcurrentHashMap<>();

    @Override
    public void lock(DocumentReference document)
    {
        Lock lock;
        synchronized (this.locks) {
            if (!this.locks.containsKey(document)) {
                lock = new StampedLock().asWriteLock();
                this.locks.put(document, lock);
            } else {
                lock = this.locks.get(document);
            }
        }
        try {
            lock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // We don't expect any interruptions
        }
    }

    @Override
    public void unlock(DocumentReference document)
    {
        Lock lock = this.locks.get(document);
        if (lock == null) {
            return;
        }
        try {
            lock.unlock();
        } catch (IllegalMonitorStateException e) {
            // We don't expect any interruptions
        }
    }
}
