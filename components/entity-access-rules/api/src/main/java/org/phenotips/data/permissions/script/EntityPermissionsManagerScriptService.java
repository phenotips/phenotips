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
package org.phenotips.data.permissions.script;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent.RightsUpdateEventType;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityResolver;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.UserManager;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 * @since 1.0M9
 */
@Component
@Named("permissions")
@Singleton
public class EntityPermissionsManagerScriptService implements ScriptService
{
    @Inject
    @Named("secure")
    private EntityPermissionsManager manager;

    @Inject
    private PrimaryEntityResolver resolver;

    /** Used for obtaining the current user. */
    @Inject
    private UserManager userManager;

    /** Used for checking access rights. */
    @Inject
    private AuthorizationService access;

    /**
     * Get the visibility options available, excluding {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of enabled visibilities, may be empty if none are enabled
     */
    public Collection<Visibility> listVisibilityOptions()
    {
        return this.manager.listVisibilityOptions();
    }

    /**
     * Get all visibility options available in the platform, including {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of visibilities, may be empty if none are available
     * @since 1.3M2
     */
    public Collection<Visibility> listAllVisibilityOptions()
    {
        return this.manager.listAllVisibilityOptions();
    }

    /**
     * Get the default visibility to set for new entity records.
     *
     * @return a visibility, or {@code null} if none is configured or the configured one isn't valid
     * @since 1.3M2
     */
    public Visibility getDefaultVisibility()
    {
        return this.manager.getDefaultVisibility();
    }

    /**
     * Resolve the {@link Visibility} from the provided {@code name}.
     *
     * @param name the name of the {@link Visibility} of interest
     * @return the {@link Visibility} associated with the provided {@code name}
     */
    public Visibility resolveVisibility(String name)
    {
        return this.manager.resolveVisibility(name);
    }

    /**
     * Lists all {@link AccessLevel#isAssignable() assignable} access levels.
     *
     * @return a collection of all {@link AccessLevel#isAssignable() assignable} {@link AccessLevel} objects
     */
    public Collection<AccessLevel> listAccessLevels()
    {
        return this.manager.listAccessLevels();
    }

    /**
     * Resolve the {@link AccessLevel} from the provided {@code name}.
     *
     * @param name the name of the {@link AccessLevel} of interest
     * @return the {@link AccessLevel} associated with the provided {@code name}
     */
    public AccessLevel resolveAccessLevel(String name)
    {
        return this.manager.resolveAccessLevel(name);
    }

    /**
     * Returns {@link EntityAccess} to the given {@code entity} for the current user.
     *
     * @param entity the {@link PrimaryEntity entity} of interest
     * @return the {@link EntityAccess} to the given {@code entity}
     */
    public EntityAccess getEntityAccess(PrimaryEntity entity)
    {
        if (entity == null) {
            return null;
        }
        if (!this.access.hasAccess(this.userManager.getCurrentUser(), Right.VIEW, entity.getDocumentReference())) {
            return null;
        }
        return this.manager.getEntityAccess(entity);
    }

    /**
     * Returns {@link EntityAccess} to the entity with the given {@code entityId}, for the current user.
     *
     * @param entityId the ID, as string, for {@link PrimaryEntity entity} of interest
     * @return the {@link EntityAccess} to the given {@code entity}
     */
    public EntityAccess getEntityAccess(String entityId)
    {
        PrimaryEntity entity = this.resolver.resolveEntity(entityId);
        return getEntityAccess(entity);
    }

    /**
     * Fires a rights update event for the entity with {@code targetEntityId}.
     *
     * @param targetEntityId the ID for the {@link PrimaryEntity} of interest
     */
    public void fireRightsUpdateEvent(String targetEntityId)
    {
        this.manager.fireRightsUpdateEvent(targetEntityId);
    }

    /**
     * Fires a rights update event for the entity with {@code targetEntityId} for particular update events.
     *
     * @param eventTypes the types of this event, a list of {@link RightsUpdateEventType}s
     * @param targetEntityId the ID for the {@link PrimaryEntity} of interest
     */
    public void fireRightsUpdateEvent(List<RightsUpdateEventType> eventTypes, String targetEntityId)
    {
        this.manager.fireRightsUpdateEvent(eventTypes, targetEntityId);
    }

    /**
     * Fires a study update event for the entity with {@code targetEntityId}.
     *
     * @param targetEntityId the ID for the {@link PrimaryEntity} of interest
     * @param newStudyId the ID for the new study the entity is getting assigned to
     */
    public void fireStudyUpdateEvent(String targetEntityId, String newStudyId)
    {
        this.manager.fireStudyUpdateEvent(targetEntityId, newStudyId);
    }
}
