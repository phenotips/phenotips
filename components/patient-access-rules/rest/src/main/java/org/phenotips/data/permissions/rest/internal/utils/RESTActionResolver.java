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
package org.phenotips.data.permissions.rest.internal.utils;

import org.phenotips.data.permissions.AccessLevel;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Set;

/**
 * Class for determining the valid HTTP methods that can be performed on a REST endpoint given the current user.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Unstable
@Role
public interface RESTActionResolver
{
    /**
     * Determines the set of valid HTTP methods that can be performed on a given REST endpoint.
     *
     * @param restInterface the interface defining the RESTful endpoint
     * @param accessLevel the current accessLevel, may be {@code null} in which case all methods are assumed to be
     *            allowed
     * @return a set of Http methods that can be performed on the provided endpoint
     */
    Set<String> resolveActions(Class<?> restInterface, AccessLevel accessLevel);
}
