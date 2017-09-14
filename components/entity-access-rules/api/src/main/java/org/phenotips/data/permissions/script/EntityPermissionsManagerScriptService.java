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
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityResolver;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.UserManager;

import java.util.Collection;

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

    public Visibility resolveVisibility(String name)
    {
        return this.manager.resolveVisibility(name);
    }

    public Collection<AccessLevel> listAccessLevels()
    {
        return this.manager.listAccessLevels();
    }

    public AccessLevel resolveAccessLevel(String name)
    {
        return this.manager.resolveAccessLevel(name);
    }

    /**
     * Returns EntityAccess to the given entity for the given user.
     *
     * @param targetEntity the entity
     */
    public EntityAccess getEntityAccess(PrimaryEntity targetEntity)
    {
        return this.getEntityAccess(targetEntity.getId());
    }

    public EntityAccess getEntityAccess(String targetEntityId)
    {
        // scripts have only access to a SecurePatient implementation of a Patient,
        // which does not support all the functionality EntityAccess needs. So
        // need to get the full Patient object here instead of taking it as an argument
        //
        // Since this is a script service, need to check access rights the same way SecurePatientReporistory does.
        //
        // TODO: rights management should be refactored so that less is done from velocity
        //       and this method won't be needed any more

        PrimaryEntity entity = this.resolver.resolveEntity(targetEntityId);
        if (entity == null) {
            return null;
        }
        if (!this.access.hasAccess(this.userManager.getCurrentUser(), Right.VIEW, entity.getDocumentReference())) {
            return null;
        }
        return this.manager.getEntityAccess(entity);
    }

    public void fireRightsUpdateEvent(String targetEntityId)
    {
        this.manager.fireRightsUpdateEvent(targetEntityId);
    }
}
