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
package org.phenotips.data.permissions;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * A manager for entity permissions preferences. Provides convenience methods for fetching default owner, view,
 * collaborators and study preferences for particular entity.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
@Role
public interface EntityPermissionsPreferencesManager
{
    /**
     * Get the {@code defaultOwner} preference for the {@code entity} of interest or for the currently logged-in user,
     * if {@code entity} is {@code null}.
     *
     * @param entity the {@code entity} of interest whose default settings will be fetched (a user or workgroup)
     * @return an entity of the default owner, if exists, null otherwise
     */
    DocumentReference getDefaultOwner(@Nullable DocumentReference entity);

    /**
     * Get the {@code defaultCollaborator} preferences for the {@code entity} of interest or for the currently
     * logged-in user, if {@code entity} is {@code null}.
     *
     * @param entity the {@code entity} whose default settings will be fetched (a user, workgroup, or study)
     * @return a map of default {@link Collaborator}s mapped to the corresponding entities
     */
    Map<EntityReference, Collaborator> getDefaultCollaborators(@Nullable DocumentReference entity);

    /**
     * Get the {@code defaultVisibility} preference for the {@code entity} of interest or for the currently
     * logged-in user, if {@code entity} is {@code null}.
     *
     * @param entity the {@code entity} whose default settings will be fetched (a user, workgroup, or study)
     * @return the default {@link Visibility}, if exists, null otherwise
     */
    Visibility getDefaultVisibility(DocumentReference entity);

    /**
     * Get the {@code defaultStudy} preference for the {@code entity} of interest or for the currently logged-in user,
     * if {@code entity} is {@code null}.
     *
     * @param entity the {@code entity} of interest whose default settings will be fetched (a user or workgroup)
     * @return an entity of the default study, if exists, null otherwise
     */
    DocumentReference getDefaultStudy(DocumentReference entity);
}
