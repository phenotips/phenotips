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
package org.phenotips.studies.family.internal.export;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;

/**
 * A component which knows how to collect all the data a pedigree needs to know about a patient.
 *
 * @version $Id$
 */
@Component(roles = { PedigreePatientSummary.class })
@Singleton
public class PedigreePatientSummary
{
    private static final String LAST_NAME = "last_name";

    private static final String FIRST_NAME = "first_name";

    private static final String PERMISSIONS = "permissions";

    private static final String URL = "url";

    private static final String NAME = "name";

    private static final String IDENTIFIER = "identifier";

    private static final String ID = "id";

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    @Inject
    private Provider<XWikiContext> provider;

    /**
     * Collects all the information about a patient that pedigree needs and returns it in the JSON
     * format that pedigree knows how to handle. The patient may or may not be in a family.
     *
     * @param patient the summary will be generated for this patient
     * @return patient summary in the JSON format that pedigree knows how to handle
     */
    public JSONObject getPatientSummaryForPedigree(Patient patient)
    {
        JSONObject patientJSON = new JSONObject();

        // handle patient names
        PatientData<String> patientNames = patient.getData("patientName");
        String firstName = StringUtils.defaultString(patientNames.get(FIRST_NAME));
        String lastName = StringUtils.defaultString(patientNames.get(LAST_NAME));
        String patientNameForJSON = String.format("%s %s", firstName, lastName).trim();

        // add data to json
        patientJSON.put(ID, patient.getId());
        patientJSON.put(IDENTIFIER, patient.getExternalId());
        patientJSON.put(NAME, patientNameForJSON);

        // Patient URL
        XWikiContext context = this.provider.get();
        String url = context.getWiki().getURL(patient.getDocumentReference(), "view", context);
        patientJSON.put(URL, url);

        // add permissions information
        User currentUser = this.userManager.getCurrentUser();
        JSONObject permissionJSON = new JSONObject();
        permissionJSON.put("hasEdit",
            this.authorizationService.hasAccess(currentUser, Right.EDIT, patient.getDocumentReference()));
        permissionJSON.put("hasView",
            this.authorizationService.hasAccess(currentUser, Right.VIEW, patient.getDocumentReference()));
        permissionJSON.put("hasDelete",
            this.authorizationService.hasAccess(currentUser, Right.DELETE, patient.getDocumentReference()));
        patientJSON.put(PERMISSIONS, permissionJSON);

        return patientJSON;
    }
}
