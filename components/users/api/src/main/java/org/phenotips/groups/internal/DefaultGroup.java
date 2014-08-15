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
}
