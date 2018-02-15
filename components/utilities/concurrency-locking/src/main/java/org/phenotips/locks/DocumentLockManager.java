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
package org.phenotips.locks;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Allows acquiring and releasing locks on specific documents. Acquiring locks is a blocking operation, the method will
 * hang until the document is released.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Unstable("New API introduced in 1.4")
@Role
public interface DocumentLockManager
{
    /**
     * Lock a document. This method will block until the lock is successfully obtained.
     *
     * @param document the document to lock
     */
    void lock(DocumentReference document);

    /**
     * Unlock a document.
     *
     * @param document the document to unlock
     */
    void unlock(DocumentReference document);
}
