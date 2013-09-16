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
package org.phenotips.data.similarity.permissions.internal;

import org.phenotips.data.permissions.internal.AbstractAccessLevel;

import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Allows reading only partial information from the patient. Only works when pairing two patients, a reference (visible
 * to the current user) and a matched patient (explicitly marked as "matchable"), and only features similar between the
 * two patients will be accessible. On the permissiveness level, this sits between
 * {@link org.phenotips.data.permissions.internal.access.NoAccessLevel no access} and
 * {@link org.phenotips.data.permissions.internal.access.ViewAccessLevel view access}.
 * 
 * @version $Id$
 */
@Component
@Named("match")
@Singleton
public class MatchAccessLevel extends AbstractAccessLevel
{
    /** Default constructor. */
    public MatchAccessLevel()
    {
        super(5, false);
    }

    @Override
    public String getName()
    {
        return "match";
    }
}
