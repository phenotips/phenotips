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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityResolver;
import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Implementation that allows access for a Collaborator based on the Access Level.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("collaborator-access")
@Singleton
public class CollaboratorAccessAuthorizationModule implements AuthorizationModule
{
    /**
     * Checks to see if document is an entity (DocumentReference).
     */
    @Inject
    private PrimaryEntityResolver resolver;

    @Inject
    private EntityAccessManager accessHelper;

    @Override
    public int getPriority()
    {
        return 300;
    }

    @Override
    public Boolean hasAccess(User user, Right access, EntityReference entity)
    {
        if (!ObjectUtils.allNotNull(user, access, entity) || access.getTargetedEntityType() == null
            || !access.getTargetedEntityType().contains(EntityType.DOCUMENT)) {
            return null;
        }

        // This converts the document to an entity.
        PrimaryEntity primaryEntity = this.resolver.resolveEntity(entity.toString());
        if (primaryEntity == null) {
            return null;
        }
        // This retrieves the access level for the entity.
        AccessLevel grantedAccess = this.accessHelper.getAccessLevel(primaryEntity, user.getProfileDocument());
        Right grantedRight = grantedAccess.getGrantedRight();

        if (grantedRight.equals(access)
            || (grantedRight.getImpliedRights() != null && grantedRight.getImpliedRights().contains(access))) {
            return true;
        }

        return null;
    }
}
