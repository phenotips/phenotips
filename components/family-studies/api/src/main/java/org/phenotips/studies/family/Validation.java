package org.phenotips.studies.family;

import org.phenotips.studies.family.internal.StatusResponse;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;

@Role
public interface Validation
{
    boolean isInFamily(String familyAnchor, String otherId) throws XWikiException;

    StatusResponse canAddToFamily(String familyAnchor, String patientId) throws XWikiException;

    boolean hasFamily(String id) throws XWikiException;
}
