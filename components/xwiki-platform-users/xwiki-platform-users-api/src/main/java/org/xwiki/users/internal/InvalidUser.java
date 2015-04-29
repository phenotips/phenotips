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
package org.xwiki.users.internal;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.users.AbstractUser;

import java.net.URI;

/**
 * {@link org.xwiki.users.User} implementation marking invalid users. This could mean users no longer existing in the
 * wiki, or users whose profiles are not accessible.
 *
 * @version $Id$
 * @since 1.0M9
 */
public class InvalidUser extends AbstractUser
{
    /**
     * Constructor.
     *
     * @param profileReference supposed user profile, no longer valid for some reason
     * @param serializer the entity reference serializer to use
     */
    public InvalidUser(DocumentReference profileReference, EntityReferenceSerializer<String> serializer)
    {
        this.profileReference = profileReference;
        this.serializer = serializer;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.users.User#exists()
     */
    @Override
    public boolean exists()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.users.User#getName()
     */
    @Override
    public String getName()
    {
        return getUsername();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.users.User#getProfileDocument()
     */
    @Override
    public DocumentReference getProfileDocument()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.users.User#getProfileURI()
     */
    @Override
    public URI getProfileURI()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.users.User#getAttribute(String)
     */
    @Override
    public Object getAttribute(String attributeName)
    {
        return null;
    }
}
