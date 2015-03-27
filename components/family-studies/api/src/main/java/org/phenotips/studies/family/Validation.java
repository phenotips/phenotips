package org.phenotips.studies.family;

import org.phenotips.studies.family.internal.StatusResponse;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Role
public interface Validation
{
    StatusResponse canAddToFamily(String familyAnchor, String patientId) throws XWikiException;

    StatusResponse canAddToFamily(XWikiDocument familyDoc, String patientId)
        throws XWikiException;

    StatusResponse checkFamilyAccessWithResponse(XWikiDocument familyDoc);

    StatusResponse createInsufficientPermissionsResponse(String patientId);

    boolean hasPatientEditAccess(XWikiDocument patientDoc);
}
