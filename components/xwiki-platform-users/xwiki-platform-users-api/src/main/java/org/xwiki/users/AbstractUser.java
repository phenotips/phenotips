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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.xwiki.users;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Base class for implementing users.
 *
 * @version $Id$
 * @since 1.0M9
 */
public abstract class AbstractUser implements User
{
    /** A link to the user profile document. */
    protected DocumentReference profileReference;

    /** Used for serializing the profile reference into a string. */
    protected EntityReferenceSerializer<String> serializer;

    /**
     * {@inheritDoc}
     *
     * @see User#getId()
     */
    @Override
    public String getId()
    {
        return (this.profileReference == null) ? "" : this.serializer.serialize(this.profileReference);
    }

    /**
     * {@inheritDoc}
     *
     * @see User#getUsername()
     */
    @Override
    public String getUsername()
    {
        return (this.profileReference == null) ? "" : this.profileReference.getName();
    }

    /**
     * {@inheritDoc}
     *
     * @see User#getProfileDocument()
     */
    @Override
    public DocumentReference getProfileDocument()
    {
        return this.profileReference;
    }

    /**
     * {@inheritDoc}
     *
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return getName();
    }

    /**
     * {@inheritDoc}
     *
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo(User user)
    {
        return getName().compareTo(user.getName());
    }
}
