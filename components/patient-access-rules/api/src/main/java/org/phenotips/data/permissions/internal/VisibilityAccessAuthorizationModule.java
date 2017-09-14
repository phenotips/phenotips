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

import org.phenotips.data.permissions.Visibility;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityResolver;
import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Implementation that allows access based on Visibility rights.
 *
 * @version $Id$
 * @since 1.4
 */
public class VisibilityAccessAuthorizationModule implements AuthorizationModule
{
    /**
     * Checks to see if document is an entity (DocumentReference).
     */
    @Inject
    private PrimaryEntityResolver resolver;

    @Inject
    private EntityAccessHelper helper;

    @Override
    public int getPriority()
    {
        return 200;
    }

    @Override
    public Boolean hasAccess(User user, Right access, EntityReference entity)
    {
        if (!ObjectUtils.allNotNull(access, entity) || access.getTargetedEntityType() == null
            || !access.getTargetedEntityType().contains(EntityType.DOCUMENT)) {
            return null;
        }

        // This converts the document to an entity.
        PrimaryEntity primaryEntity = this.resolver.resolveEntity(entity.toString());
        if (primaryEntity == null) {
            return null;
        }

        Visibility visibility = this.helper.getVisibility(primaryEntity);
        if (visibility == null) {
            return null;
        }

        // Checks if the visibility of Patient Record and the access rights
        Right grantedRight = visibility.getDefaultAccessLevel().getGrantedRight();
        if (user != null && user.getProfileDocument() != null && (grantedRight.equals(access)
            || (grantedRight.getImpliedRights() != null && grantedRight.getImpliedRights().contains(access)))) {
            return true;
        }
        return null;
    }
}
