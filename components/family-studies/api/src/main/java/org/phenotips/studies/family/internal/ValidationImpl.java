package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.internal.PhenoTipsPatient;
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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Collection of checks for checking if certain actions are allowed. Needs to be split up really, but later.
 */
@Component
@Singleton
public class ValidationImpl implements Validation
{
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
    @Named("edit") private AccessLevel editAccess;

    /**
     * Checks if the patient is already present within the family members list.
     */
    private boolean isInFamily(XWikiDocument family, String patientId) throws XWikiException
    {
        return familyUtils.getFamilyMembers(family).contains(patientId);
    }

    /**
     * Checks if the current {@link com.xpn.xwiki.XWikiContext}/user has sufficient access to this patent id and the
     * family to which the patient is being added to.
     */
    public StatusResponse canAddToFamily(String familyAnchor, String patientId) throws XWikiException
    {
        XWikiDocument family = familyUtils.getFamilyDoc(familyUtils.getFromDataSpace(familyAnchor));

        return canAddToFamily(family, patientId);
    }

    public StatusResponse canAddToFamily(XWikiDocument familyDoc, String patientId)
        throws XWikiException
    {
        StatusResponse response = new StatusResponse();

        DocumentReference patientRef = referenceResolver.resolve(patientId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument patientDoc = familyUtils.getDoc(patientRef);
        if (patientDoc == null) {
            response.statusCode = 404;
            response.errorType = "invalidId";
            response.message = String.format("Could not find patient %s.", patientId);
            return response;
        }

        EntityReference patientFamilyRef = familyUtils.getFamilyReference(patientDoc);
        if (patientFamilyRef != null) {
            boolean hasOtherFamily;
            hasOtherFamily = familyDoc == null || patientFamilyRef.compareTo(familyDoc.getDocumentReference()) != 0;
            if (hasOtherFamily) {
                response.statusCode = 501;
                response.errorType = "familyConflict";
                response.message = String.format("Patient %s belongs to a different family.", patientId);
                return response;
            }
        }

        boolean isInFamily = false;
        if (familyDoc != null) {
            isInFamily = this.isInFamily(familyDoc, patientId);
        }
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
                String.format("Patient %s has an existing pedigree.", patientId);
            return response;
        }
    }

    /** Should not be used when saving families. */
    public boolean hasPatientEditAccess(XWikiDocument patientDoc) {
        User currentUser = userManager.getCurrentUser();
        PatientAccess patientAccess = permissionsManager.getPatientAccess(new PhenoTipsPatient(patientDoc));
        AccessLevel patientAccessLevel = patientAccess.getAccessLevel(currentUser.getProfileDocument());
        return patientAccessLevel.compareTo(editAccess) >= 0;
    }

    public StatusResponse createInsufficientPermissionsResponse(String patientId) {
        StatusResponse response = new StatusResponse();
        response.statusCode = 401;
        response.errorType = "permissions";
        response.message = String.format("Insufficient permissions to edit the patient record (%s).", patientId);
        return response;
    }

    public StatusResponse checkFamilyAccessWithResponse(XWikiDocument familyDoc) {
        StatusResponse response = new StatusResponse();
        User currentUser = userManager.getCurrentUser();
        if (authorizationService.hasAccess(currentUser, Right.EDIT, new DocumentReference(familyDoc.getDocumentReference()))) {
            response.statusCode = 200;
            return response;
        }
        response.statusCode = 401;
        response.errorType = "permissions";
        response.message = "Insufficient permissions to edit the family record.";
        return response;
    }
}
