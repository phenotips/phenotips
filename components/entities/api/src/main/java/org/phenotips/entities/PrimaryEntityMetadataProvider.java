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
package org.phenotips.entities;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Map;

/**
 * Provides some simple metadata about a primary entity, for example which family a patient belongs to.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable("New API introduced in 1.4")
@Role
public interface PrimaryEntityMetadataProvider
{
    /**
     * Retrieves the metadata for a particular entity.
     *
     * @param entity the target entity
     * @return a map of simple metadata, may be empty
     */
    Map<String, Object> provideMetadata(PrimaryEntity entity);
}
