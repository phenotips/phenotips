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
package org.xwiki.security.authorization;

import org.xwiki.model.EntityType;

import java.util.EnumSet;
import java.util.Set;

public class ManageRight extends Right
{
    public static final Right MANAGE = new ManageRight();

    private static final long serialVersionUID = 2197709133328931585L;

    private ManageRight()
    {
        super(new RightDescription()
        {
            @Override
            public boolean isReadOnly()
            {
                return false;
            }

            @Override
            public RuleState getTieResolutionPolicy()
            {
                return RuleState.ALLOW;
            }

            @Override
            public Set<EntityType> getTargetedEntityType()
            {
                return EnumSet.of(EntityType.DOCUMENT);
            }

            @Override
            public String getName()
            {
                return "manage";
            }

            @Override
            public boolean getInheritanceOverridePolicy()
            {
                return true;
            }

            @Override
            public Set<Right> getImpliedRights()
            {
                return new RightSet(Right.EDIT, Right.DELETE);
            }

            @Override
            public RuleState getDefaultState()
            {
                return RuleState.DENY;
            }
        });
    }
}
