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
package org.phenotips.data.permissions.internal.access;

import org.phenotips.data.permissions.internal.AbstractAccessLevel;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.ManageRight;
import org.xwiki.security.authorization.Right;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */
@Component
@Named("owner")
@Singleton
public class OwnerAccessLevel extends AbstractAccessLevel
{
    public OwnerAccessLevel()
    {
        super(100, false);
    }

    @Override
    public String getName()
    {
        return "owner";
    }

    @Override
    public Right getGrantedRight()
    {
        return ManageRight.MANAGE;
    }
}
