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
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Validation;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Collection of checks for checking if certain actions are allowed.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Singleton
public class ValidationImpl implements Validation
{
    @Inject
    private PatientRepository patientRepository;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private FamilyUtils familyUtils;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private UserManager userManager;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    @Named("edit")
    private AccessLevel editAccess;

    @Inject
    @Named("view")
    private AccessLevel viewAccess;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> provider;

    /**
     * Checks if the patient is already present within the family members list.
     */
    private boolean isInFamily(XWikiDocument family, String patientId) throws XWikiException
    {
        return this.familyUtils.getFamilyMembers(family).contains(patientId);
    }

    @Override
    public StatusResponse canAddToFamily(String familyAnchor, String patientId) throws XWikiException
    {
        XWikiDocument family = this.familyUtils.getFamilyDoc(this.familyUtils.getFromDataSpace(familyAnchor));

        return canAddToFamily(family, patientId);
    }

    @Override
    public StatusResponse canAddToFamily(XWikiDocument familyDoc, String patientId)
        throws XWikiException
    {
        StatusResponse response = new StatusResponse();

        DocumentReference patientRef = this.referenceResolver.resolve(patientId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument patientDoc = this.familyUtils.getDoc(patientRef);
        if (patientDoc == null) {
            response.statusCode = 404;
            response.errorType = "invalidId";
            response.message = String.format("Could not find patient %s.", patientId);
            return response;
        }

        EntityReference patientFamilyRef = this.familyUtils.getFamilyReference(patientDoc);
        if (patientFamilyRef != null) {
            boolean hasOtherFamily;
            hasOtherFamily = familyDoc == null || patientFamilyRef.compareTo(familyDoc.getDocumentReference()) != 0;
            if (hasOtherFamily) {
                response.statusCode = 501;
                response.errorType = "familyConflict";
                response.message = String.format("Patient %s already belongs to a different family, and therefore "
                    + "cannot be added to this one.", patientId);
                return response;
            }
        }

        boolean isInFamily = safeIsInFamilyCheck(familyDoc, patientId);

        PedigreeUtils.Pedigree pedigree = PedigreeUtils.getPedigree(patientDoc);
        if ((pedigree == null || pedigree.isEmpty()) || isInFamily) {
            if (!isInFamily && familyDoc != null) {
                return this.checkFamilyAccessWithResponse(familyDoc);
            }
            StatusResponse familyResponse = new StatusResponse();
            familyResponse.statusCode = 200;
            return familyResponse;
        } else {
            response.statusCode = 501;
            response.errorType = "existingPedigree";
            response.message =
                String.format("patient %s already has a different pedigree, and therefore cannot be included in "
                    + "this one.", patientId);
            return response;
        }
    }

    @Override
    public StatusResponse canAddEveryMember(XWikiDocument family, List<String> updatedMembers)
        throws XWikiException
    {
        StatusResponse defaultResponse = new StatusResponse();
        defaultResponse.statusCode = 200;

        for (String member : updatedMembers) {
            StatusResponse patientResponse = this.canAddToFamily(family, member);
            if (patientResponse.statusCode != 200) {
                return patientResponse;
            }
        }
        return defaultResponse;
    }

    private boolean safeIsInFamilyCheck(XWikiDocument familyDoc, String patientId) throws XWikiException
    {
        if (familyDoc != null) {
            return this.isInFamily(familyDoc, patientId);
        }
        return false;
    }

    @Override
    public StatusResponse checkFamilyAccessWithResponse(XWikiDocument familyDoc)
    {
        StatusResponse response = new StatusResponse();
        User currentUser = this.userManager.getCurrentUser();
        if (this.authorizationService.hasAccess(currentUser, Right.EDIT,
            new DocumentReference(familyDoc.getDocumentReference())))
        {
            response.statusCode = 200;
            return response;
        }
        response.statusCode = 401;
        response.errorType = "permissions";
        response.message = "Insufficient permissions to edit the family record.";
        return response;
    }

    /* Should not be used when saving families. Todo why? */
    @Override
    public boolean hasPatientEditAccess(String patientId)
    {
        User currentUser = this.userManager.getCurrentUser();
        return hasPatientAccess(this.patientRepository.getPatientById(patientId), this.editAccess, currentUser);
    }

    @Override
    public boolean hasPatientEditAccess(Patient patient)
    {
        User currentUser = this.userManager.getCurrentUser();
        return hasPatientAccess(patient, this.editAccess, currentUser);
    }

    @Override
    public boolean hasPatientEditAccess(Patient patient, User user)
    {
        return hasPatientAccess(patient, this.editAccess, user);
    }

    @Override
    public boolean hasPatientViewAccess(Patient patient)
    {
        User currentUser = this.userManager.getCurrentUser();
        return hasPatientAccess(patient, this.viewAccess, currentUser);
    }

    @Override
    public boolean hasPatientViewAccess(Patient patient, User user)
    {
        return hasPatientAccess(patient, this.viewAccess, user);
    }

    private boolean hasPatientAccess(Patient patient, AccessLevel accessLevel, User user)
    {
        PatientAccess patientAccess = this.permissionsManager.getPatientAccess(patient);
        AccessLevel patientAccessLevel = patientAccess.getAccessLevel(user.getProfileDocument());
        return patientAccessLevel.compareTo(accessLevel) >= 0;
    }

    @Override
    public boolean hasAccess(DocumentReference document, String permissions)
    {
        XWikiDocument xWikiDoc = null;
        try {
            xWikiDoc = this.familyUtils.getDoc(document);
        } catch (XWikiException e) {
            this.logger.error("Error retrieving family document for family [{}]: [{}]",
                document.getName(), e.getMessage());
        }

        XWikiContext context = this.provider.get();
        XWiki wiki = context.getWiki();

        try {
            return wiki.checkAccess(permissions, xWikiDoc, context);
        } catch (XWikiException e) {
            this.logger.error("Error checking permissions on [{}]: [{}]",
                xWikiDoc.getName(), e.getMessage());
        }
        return false;
    }
}
