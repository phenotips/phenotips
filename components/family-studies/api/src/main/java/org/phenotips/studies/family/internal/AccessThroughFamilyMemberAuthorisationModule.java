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

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.internal.PatientAccessHelper;
import org.phenotips.security.authorization.AuthorizationModule;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Implementation that allows view and edit access to families to the users that have access to at least one patient
 * that is a member of that family (ignoring access granted through the patient visibility).
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("family-member-access")
@Singleton
public class AccessThroughFamilyMemberAuthorisationModule implements AuthorizationModule
{
    /** Checks to see if a document is a family. */
    @Inject
    private FamilyRepository familyRepository;

    /** Checks to see if the user has access. */
    @Inject
    private PatientAccessHelper manager;

    @Override
    public int getPriority()
    {
        return 200;
    }

    @Override
    public Boolean hasAccess(User user, Right access, EntityReference entity)
    {
        if (!ObjectUtils.allNotNull(user, access, entity) || !(access == Right.VIEW || access == Right.EDIT)) {
            return null;
        }

        Family family = this.familyRepository.get(entity.toString());
        if (family == null) {
            return null;
        }

        for (Patient member : family.getMembers()) {
            AccessLevel grantedAccess = this.manager.getAccessLevel(member, user.getProfileDocument());
            Right grantedRight = grantedAccess.getGrantedRight();

            if (grantedRight != null && (grantedRight.equals(access)
                || (grantedRight.getImpliedRights() != null && grantedRight.getImpliedRights().contains(access)))) {
                return true;
            }
        }

        return null;
    }
}
