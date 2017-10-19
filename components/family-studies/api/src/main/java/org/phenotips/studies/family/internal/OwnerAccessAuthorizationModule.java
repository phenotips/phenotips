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
package org.phenotips.studies.family.internal;

import org.phenotips.data.permissions.Owner;
import org.phenotips.security.authorization.AuthorizationModule;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation that allows all document access to the owner of a family record.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("family-owner-access")
@Singleton
public class OwnerAccessAuthorizationModule implements AuthorizationModule
{
    /** Checks to see if a document is a patient. */
    @Inject
    private FamilyRepository familyRepository;

    /** Reads the owner property. */
    @Inject
    private DocumentAccessBridge dab;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> strResolver;

    @Inject
    private AuthorizationService auth;

    @Override
    public int getPriority()
    {
        return 400;
    }

    @Override
    public Boolean hasAccess(User user, Right access, EntityReference entity)
    {
        if (!ObjectUtils.allNotNull(access, entity) || access.getTargetedEntityType() == null
            || !access.getTargetedEntityType().contains(EntityType.DOCUMENT)) {
            return null;
        }

        Family family = this.familyRepository.get(entity.toString());
        if (family == null) {
            return null;
        }

        String ownerStr = (String) this.dab.getProperty(family.getDocumentReference(),
            this.resolver.resolve(Owner.CLASS_REFERENCE), Owner.PROPERTY_NAME);
        DocumentReference owner = this.strResolver.resolve(ownerStr);

        if (StringUtils.isEmpty(ownerStr) && (user == null || user.getProfileDocument() == null)
            || user != null && owner.equals(user.getProfileDocument())) {
            return true;
        }
        // Grant access to administrators
        if (this.auth.hasAccess(user, Right.ADMIN, entity)) {
            return true;
        }

        return null;
    }
}
