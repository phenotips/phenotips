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
package org.phenotips.groups.internal;

import org.phenotips.groups.Group;

import org.xwiki.model.reference.DocumentReference;

/**
 * Default implementation for {@link Group}.
 *
 * @version $Id$
 * @since 1.0M9
 */
public class DefaultGroup implements Group
{
    /** @see #getReference() */
    private final DocumentReference reference;

    /**
     * Simple constructor.
     *
     * @param reference the reference to the document where this group is defined
     */
    public DefaultGroup(DocumentReference reference)
    {
        this.reference = reference;
    }

    @Override
    public DocumentReference getReference()
    {
        return this.reference;
    }

    @Override
    public String toString()
    {
        return "Group " + getReference().getName();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof DefaultGroup)) {
            return false;
        }
        DefaultGroup otherGroup = (DefaultGroup) o;
        return this.reference.getName().equals(otherGroup.getReference().getName());
    }

    @Override
    public int hashCode()
    {
        return this.reference.getName().hashCode();
    }
}
