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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
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
public class PhenotipsFamilyRepository extends FamilyEntityManager implements FamilyRepository
{
    private static final String FAMILY_REFERENCE_FIELD = "reference";

    private static final String OWNER = "owner";

    private static final String IDENTIFIER = "identifier";

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private PatientsInFamilyManager pifManager;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private PedigreeProcessor pedigreeConverter;

    /** Used for obtaining the current user. */
    @Inject
    private UserManager userManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entitySerializer;

    @Override
    public Family createFamily(User creator)
    {
        return create(creator != null ? creator.getProfileDocument() : null);
    }

    @Override
    public synchronized Family create(final DocumentReference creator)
    {
        try {
            final XWikiContext context = this.xcontextProvider.get();
            final Family family = super.create(creator);
            final XWikiDocument doc = family.getXDocument();

            // Adding owner reference to family
            doc.newXObject(Owner.CLASS_REFERENCE, context).set(OWNER,
                creator == null ? StringUtils.EMPTY : this.entitySerializer.serialize(creator), context);
            // Adding identifier to family
            doc.getXObject(Family.CLASS_REFERENCE).setLongValue(IDENTIFIER,
                Integer.parseInt(family.getId().replaceAll("\\D++", StringUtils.EMPTY)));
            context.getWiki().saveDocument(doc, context);
            return family;
        } catch (Exception ex) {
            this.logger.warn("Failed to create family: {}", ex.getMessage(), ex);
            return null;
        }
    }

    // TODO add inherited delete()
    @Override
    public synchronized boolean deleteFamily(Family family, User updatingUser, boolean deleteAllMembers)
    {
        return delete(family, deleteAllMembers);
    }

    @Override
    public synchronized boolean delete(final Family family)
    {
        return delete(family, false);
    }

    @Override
    public synchronized boolean delete(final Family family, boolean deleteAllMembers)
    {
        // TODO: Should there be a SecureFamilyRepository to perform these checks (similar to SecurePatientRepository)?
        final User currentUser = this.userManager.getCurrentUser();
        if (!canDeleteFamily(family, currentUser, deleteAllMembers, false)) {
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
        } else if (!this.pifManager.forceRemoveAllMembers(family, currentUser)) {
            return false;
        }

        return super.delete(family);
    }


    @Override
    public Family getFamilyById(String id)
    {
        return get(id);
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
                    if (!this.authorizationService.hasAccess(
                        updatingUser, Right.DELETE, patient.getDocumentReference())) {
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
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, patient.getDocumentReference())) {
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
            family.getXDocument().setAuthorReference(context.getUserReference());
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

    /*
     * Returns the largest family identifier id
     */
    @Override
    protected long getLastUsedId()
    {
        this.logger.debug("getLastUsedId()");

        long crtMaxID = 0;
        try {
            Query q = this.qm.createQuery("select family.identifier "
                + "from     Document doc, "
                + "         doc.object(PhenoTips.FamilyClass) as family "
                + "where    family.identifier is not null "
                + "order by family.identifier desc", Query.XWQL).setLimit(1);
            List<Long> crtMaxIDList = q.execute();
            if (crtMaxIDList.size() > 0 && crtMaxIDList.get(0) != null) {
                crtMaxID = crtMaxIDList.get(0);
            }
            crtMaxID = Math.max(crtMaxID, 0);
        } catch (QueryException ex) {
            this.logger.warn("Failed to get the last used identifier: {}", ex.getMessage());
        }
        return crtMaxID;
    }

    @Override
    public EntityReference getDataSpace()
    {
        return Family.DATA_SPACE;
    }
}
