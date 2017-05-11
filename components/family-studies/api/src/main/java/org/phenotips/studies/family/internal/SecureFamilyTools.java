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
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyTools;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.exceptions.PTException;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.Right;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * A secure wrapper around FamilyTools which exposes a version of a Family object with
 * no access to underlying XWikiDocument (implemented by {@link SecureFamily}).
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = FamilyTools.class)
@Named("secure")
@Singleton
public class SecureFamilyTools implements FamilyTools
{
    @Inject
    private FamilyTools familyTools;

    @Override
    public Family createFamily()
    {
        return this.createSecureFamily(this.familyTools.createFamily());
    }

    @Override
    public Family getFamilyById(String familyId)
    {
        return this.createSecureFamily(this.familyTools.getFamilyById(familyId));
    }

    @Override
    public Pedigree getPedigreeForFamily(String familyId)
    {
        return this.familyTools.getPedigreeForFamily(familyId);
    }

    @Override
    public Family getFamilyForPatient(String patientId)
    {
        return this.createSecureFamily(this.familyTools.getFamilyForPatient(patientId));
    }

    @Override
    public Pedigree getPedigreeForPatient(String patientId)
    {
        return this.familyTools.getPedigreeForPatient(patientId);
    }

    @Override
    public boolean removeMember(String patientId)
    {
        return this.familyTools.removeMember(patientId);
    }

    @Override
    public boolean deleteFamily(String familyId, boolean deleteAllMembers)
    {
        return this.familyTools.deleteFamily(familyId, deleteAllMembers);
    }

    @Override
    public boolean forceRemoveAllMembers(Family family)
    {
        return this.familyTools.forceRemoveAllMembers(family);
    }

    @Override
    public boolean currentUserCanDeleteFamily(String familyId, boolean deleteAllMembers)
    {
        return this.familyTools.currentUserCanDeleteFamily(familyId, deleteAllMembers);
    }

    @Override
    public boolean familyExists(String familyId)
    {
        return this.familyTools.familyExists(familyId);
    }

    @Override
    public boolean currentUserHasAccessRight(String familyId, Right right)
    {
        return this.familyTools.currentUserHasAccessRight(familyId, right);
    }

    @Override
    public void setPedigree(Family family, Pedigree pedigree) throws PTException
    {
        this.familyTools.setPedigree(family, pedigree);
    }

    @Override
    public boolean canAddToFamily(Family family, Patient patient, boolean throwException) throws PTException
    {
        return this.familyTools.canAddToFamily(family, patient, throwException);
    }

    /**
     * A wrapper that returns null if inputr is null or a SecureFamily wrapper otherwise.
     * May also be useful for testing (see comments for createSecurePatient() in {@link SecurePatientRepository})
     * @param family the family to wrap with a secure wrapper with no access to XWikiDocument
     * @return null if input is null, secure wrapper aroiund the family otherwise
     */
    protected SecureFamily createSecureFamily(Family family)
    {
        if (family == null) {
            return null;
        }
        return new SecureFamily(family);
    }
}
