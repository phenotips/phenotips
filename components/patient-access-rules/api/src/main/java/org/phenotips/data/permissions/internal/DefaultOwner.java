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
