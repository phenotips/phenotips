/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.users.internal;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.users.AbstractGroup;

import java.net.URI;

/**
 * {@link org.xwiki.users.Group} implementation marking invalid groups. This could mean groups no longer existing in the
 * wiki, or groups whose profiles are not accessible.
 * 
 * @version $Id$
 * @since 1.0M10
 */
public class AllLocalUsersGroup extends AbstractGroup
{
    /**
     * Constructor.
     * 
     * @param profileReference supposed group profile, no longer valid for some reason
     * @param serializer the entity reference serializer to use
     */
    public AllLocalUsersGroup(DocumentReference profileReference, EntityReferenceSerializer<String> serializer)
    {
        this.profileReference = profileReference;
        this.serializer = serializer;
    }

    @Override
    public boolean exists()
    {
        return false;
    }

    @Override
    public DocumentReference getReference()
    {
        return null;
    }

    @Override
    public URI getProfileURI()
    {
        return null;
    }

    @Override
    public Object getAttribute(String attributeName)
    {
        return null;
    }
}
