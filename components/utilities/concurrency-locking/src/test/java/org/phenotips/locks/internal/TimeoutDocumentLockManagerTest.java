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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the {@link TimeoutDocumentLockManager} component.
 *
 * @version $Id$
 */
public class TimeoutDocumentLockManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<DocumentLockManager> mocker =
        new MockitoComponentMockingRule<>(TimeoutDocumentLockManager.class);

    private DocumentLockManager lockManager;

    private DocumentReference docRef = new DocumentReference("xwiki", "data", "P0000001");

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.lockManager = this.mocker.getComponentUnderTest();
    }

    @Test
    public void lockingTwiceDelaysForTenSeconds() throws ComponentLookupException
    {
        long start = System.currentTimeMillis();
        this.lockManager.lock(this.docRef);
        this.lockManager.lock(this.docRef);
        this.lockManager.lock(this.docRef);
        long time = System.currentTimeMillis() - start;
        Assert.assertTrue(time >= 19 * 1000);
    }

    @Test
    public void lockingDifferentDocumentsDoesNotDelay() throws ComponentLookupException
    {
        long start = System.currentTimeMillis();
        this.lockManager.lock(this.docRef);
        this.lockManager.lock(new DocumentReference("xwiki", "data", "P0000002"));
        this.lockManager.lock(new DocumentReference("xwiki", "data", "P0000003"));
        long time = System.currentTimeMillis() - start;
        Assert.assertTrue(time < 5 * 1000);
    }

    @Test
    public void lockingAndUnlockingWorks() throws ComponentLookupException
    {
        long start = System.currentTimeMillis();
        this.lockManager.lock(this.docRef);
        this.lockManager.unlock(this.docRef);
        this.lockManager.lock(this.docRef);
        this.lockManager.unlock(this.docRef);
        this.lockManager.lock(this.docRef);
        this.lockManager.unlock(this.docRef);
        long time = System.currentTimeMillis() - start;
        Assert.assertTrue(time < 5 * 1000);
    }

    @Test
    public void unlockingTwiceDoesntThrowException() throws ComponentLookupException
    {
        long start = System.currentTimeMillis();
        this.lockManager.lock(this.docRef);
        this.lockManager.unlock(this.docRef);
        this.lockManager.unlock(this.docRef);
        long time = System.currentTimeMillis() - start;
        Assert.assertTrue(time < 5 * 1000);
    }

    @Test
    public void interruptingDoesntThrowException() throws ComponentLookupException
    {
        long start = System.currentTimeMillis();
        final Thread current = Thread.currentThread();
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Tests, we don't care
                }
                current.interrupt();
            }
        }).start();
        this.lockManager.lock(this.docRef);
        this.lockManager.lock(this.docRef);
        long time = System.currentTimeMillis() - start;
        Assert.assertTrue(time < 5 * 1000);
    }

    @Test
    public void locksAreAquiredWhenTheyAreReleased() throws ComponentLookupException
    {
        long start = System.currentTimeMillis();
        Thread other = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    TimeoutDocumentLockManagerTest.this.lockManager.lock(TimeoutDocumentLockManagerTest.this.docRef);
                    Thread.sleep(1000);
                    TimeoutDocumentLockManagerTest.this.lockManager.unlock(TimeoutDocumentLockManagerTest.this.docRef);
                } catch (InterruptedException e) {
                    // Tests, we don't care
                }
            }
        });
        other.start();
        try {
            Thread.sleep(1000);
            this.lockManager.lock(this.docRef);
            Thread.sleep(1000);
            this.lockManager.unlock(this.docRef);
            other.join(0);
        } catch (InterruptedException e) {
            // Tests, we don't care
        }
        long time = System.currentTimeMillis() - start;
        Assert.assertTrue(time < 5 * 1000);
    }

    @Test
    public void unlockingBeforeLockingDoesntThrowException() throws ComponentLookupException
    {
        this.lockManager.unlock(this.docRef);
    }
}
