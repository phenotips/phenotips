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
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.Owner;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.internal.AbstractPrimaryEntityManager;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.exceptions.PTException;
import org.phenotips.studies.family.exceptions.PTInvalidFamilyIdException;
import org.phenotips.studies.family.exceptions.PTInvalidPatientIdException;
import org.phenotips.studies.family.exceptions.PTNotEnoughPermissionsOnFamilyException;
import org.phenotips.studies.family.exceptions.PTNotEnoughPermissionsOnPatientException;
import org.phenotips.studies.family.exceptions.PTPatientAlreadyInAnotherFamilyException;
import org.phenotips.studies.family.groupManagers.PatientsInFamilyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides utility methods for working with family documents and patients.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = {FamilyRepository.class, PrimaryEntityManager.class})
@Named("Family")
@Singleton
public class PhenotipsFamilyRepository extends AbstractPrimaryEntityManager<Family> implements FamilyRepository
{
    private static final String PREFIX = "FAM";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private PhenotipsFamilyPermissions familyPermissions;

    @Inject
    private PatientsInFamilyManager pifManager;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> entityReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    // TODO add inherited delete()
    @Override
    public synchronized boolean deleteFamily(Family family, User updatingUser, boolean deleteAllMembers)
    {
        if (!canDeleteFamily(family, updatingUser, deleteAllMembers, false)) {
            return false;
        }
        if (deleteAllMembers) {
            for (Patient patient : this.pifManager.getMembers(family)) {
                if (!this.patientRepository.delete(patient)) {
                    this.logger.error("Failed to delete patient [{}] - deletion of family [{}] aborted",
                        patient.getId(), family.getId());
                    return false;
                }
            }
        } else if (!this.pifManager.forceRemoveAllMembers(family, updatingUser)) {
            return false;
        }

        if (!super.delete(family)) {
            this.logger.error("Failed to delete family document [{}].", family.getId());
            return false;
        }

        return true;
    }


    @Override
    public Family getFamilyById(String id)
    {
        return super.get(id);
    }

    @Override
    public Family getFamilyForPatient(Patient patient)
    {
        Collection<Family> families = this.pifManager.getGroupsForMember(patient);
        if (families.size() != 1) {
            return null;
        } else {
            return families.iterator().next();
        }
    }


    @Override
    public synchronized void updateFamilyPermissions(Family family)
    {
        XWikiContext context = this.provider.get();
        this.updateFamilyPermissions(family, context, true);
    }

    private void updateFamilyPermissions(Family family, XWikiContext context, boolean saveXwikiDocument)
    {
        this.familyPermissions.updatePermissions(family, context);
        if (saveXwikiDocument) {
            this.saveFamilyDocument(family, "updated permissions", context);
        }
    }

    @Override
    public boolean canAddToFamily(Family family, Patient patient, User updatingUser, boolean throwException)
        throws PTException
    {
        try {
            if (family == null) {
                if (throwException) {
                    throw new PTInvalidFamilyIdException(null);
                }
                return false;
            }
            if (patient == null) {
                if (throwException) {
                    throw new PTInvalidPatientIdException(null);
                }
                return false;
            }
            this.checkIfPatientCanBeAddedToFamily(family, patient, updatingUser);
            return true;
        } catch (PTException ex) {
            if (throwException) {
                throw ex;
            }
            return false;
        }
    }

    @Override
    public boolean canDeleteFamily(Family family, User updatingUser,
        boolean deleteAllMembers, boolean throwException) throws PTException
    {
        try {
            if (family == null) {
                if (throwException) {
                    throw new PTInvalidFamilyIdException(null);
                }
                return false;
            }
            if (!this.authorizationService.hasAccess(updatingUser, Right.DELETE, family.getDocumentReference())) {
                throw new PTNotEnoughPermissionsOnFamilyException(Right.DELETE, family.getId());
            }
            if (deleteAllMembers) {
                // check permissions on all patients
                for (Patient patient : this.pifManager.getMembers(family)) {
                    if (!this.authorizationService.hasAccess(updatingUser, Right.DELETE, patient.getDocument())) {
                        throw new PTNotEnoughPermissionsOnPatientException(Right.DELETE, patient.getId());
                    }
                }
            }
            return true;
        } catch (PTException ex) {
            if (throwException) {
                throw ex;
            }
            return false;
        }
    }

    private void checkIfPatientCanBeAddedToFamily(Family family, Patient patient, User updatingUser) throws PTException
    {
        // check rights
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, family.getDocumentReference())) {
            throw new PTNotEnoughPermissionsOnFamilyException(Right.EDIT, family.getId());
        }
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, patient.getDocument())) {
            throw new PTNotEnoughPermissionsOnPatientException(Right.EDIT, patient.getId());
        }
        // check for logical problems: patient in another family
        Family familyForLinkedPatient = this.getFamilyForPatient(patient);
        if (familyForLinkedPatient != null && !familyForLinkedPatient.getId().equals(family.getId())) {
            throw new PTPatientAlreadyInAnotherFamilyException(patient.getId(), familyForLinkedPatient.getId());
        }
    }

    private synchronized boolean saveFamilyDocument(Family family, String documentHistoryComment, XWikiContext context)
    {
        try {
            context.getWiki().saveDocument(family.getXDocument(), documentHistoryComment, context);
        } catch (XWikiException e) {
            this.logger.error("Error saving family [{}] document for commit {}: [{}]",
                family.getId(), documentHistoryComment, e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected String getIdPrefix()
    {
        return PREFIX;
    }

    @Override
    public synchronized Family create(DocumentReference creator)
    {
        Family newFamily = null;

        try {
            XWikiContext context = this.xcontextProvider.get();
            newFamily = super.create(creator);
            int familyID = Integer.parseInt(newFamily.getDocumentReference().getName().replaceAll("\\D++", ""));

            // TODO newFamily.getXDocument();
            XWikiDocument familyXDocument = (XWikiDocument) this.bridge.getDocument(newFamily.getDocumentReference());

            BaseObject ownerObject = familyXDocument.newXObject(Owner.CLASS_REFERENCE, context);

            String ownerString = creator == null ? "" : this.entityReferenceSerializer.serialize(creator);
            ownerObject.set("owner", ownerString, context);

            BaseObject familyObject = familyXDocument.getXObject(Family.CLASS_REFERENCE);
            familyObject.set("identifier", familyID, context);

            context.getWiki().saveDocument(familyXDocument, context);
        } catch (Exception e) {
            this.logger.error("Could not create a new family document: {}", e.getMessage());
        }

        return newFamily;
    }

    @Override
    public EntityReference getDataSpace()
    {
        return Family.DATA_SPACE;
    }
}
