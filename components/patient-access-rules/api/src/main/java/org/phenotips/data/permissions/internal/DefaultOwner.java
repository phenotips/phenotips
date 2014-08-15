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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.Owner;

import org.xwiki.model.reference.EntityReference;

/**
 * Default implementation of the {@link Owner} interface, making use of a {@link PatientAccessHelper} component for any
 * actual owner type detection.
 *
 * @version $Id$
 */
public class DefaultOwner implements Owner
{
    private final EntityReference user;

    private final PatientAccessHelper helper;

    public DefaultOwner(EntityReference user, PatientAccessHelper helper)
    {
        this.user = user;
        this.helper = helper;
    }

    @Override
    public String getType()
    {
        return this.helper.getType(this.user);
    }

    @Override
    public boolean isUser()
    {
        return "user".equals(getType());
    }

    @Override
    public boolean isGroup()
    {
        return "group".equals(getType());
    }

    @Override
    public EntityReference getUser()
    {
        return this.user;
    }

    @Override
    public String getUsername()
    {
        if (this.user == null) {
            return null;
        }
        return this.user.getName();
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof Owner)) {
            return false;
        }
        Owner otherCollaborator = (Owner) other;
        return this.user.equals(otherCollaborator.getUser());
    }

    @Override
    public int hashCode()
    {
        return this.user != null ? this.user.hashCode() : 0;
    }

    @Override
    public String toString()
    {
        return "[" + (this.user != null ? getUser() : "nobody") + "]";
    }
}
