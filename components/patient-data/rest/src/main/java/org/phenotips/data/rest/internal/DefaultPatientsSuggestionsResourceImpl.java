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
package org.phenotips.data.rest.internal;

import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.PatientsSuggestionsResource;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation for {@link PatientsSuggestionsResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.4M3
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPatientsSuggestionsResourceImpl")
@Singleton
public class DefaultPatientsSuggestionsResourceImpl extends XWikiResource implements PatientsSuggestionsResource
{
    private static final String FIRST_NAME = "first_name";

    private static final String INPUT_PARAMETER = "input";

    private static final String INPUT_FORMAT = "%%%s%%";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** The query manager for patient retrieval. */
    @Inject
    private QueryManager qm;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    @Inject
    private RecordConfigurationManager configuration;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private Provider<XWikiContext> provider;

    @Override
    public String suggest(String input, @DefaultValue("10") int maxResults,
        @DefaultValue("view") String requiredPermission, @DefaultValue("false") boolean markFamilyAssociation,
        @DefaultValue("id") String orderField, @DefaultValue("asc") String order)
    {
        if (StringUtils.isEmpty(input)) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        JSONArray results = new JSONArray();
        JSONObject jsonResult = new JSONObject();

        List<String> queryResults = queryPatients(input.toLowerCase(), orderField, order);

        for (String queryResult : queryResults) {
            Patient patient = this.patientRepository.get(queryResult);
            if (patient == null) {
                continue;
            }

            Right right = Right.toRight(requiredPermission);
            if (!this.authorizationService.hasAccess(this.userManager.getCurrentUser(), right,
                patient.getDocumentReference())) {
                continue;
            }

            results.put(getPatientJSON(patient, markFamilyAssociation));

            if (results.length() >= maxResults) {
                break;
            }
        }

        jsonResult.put("matchedPatients", results);

        return jsonResult.toString();
    }

    private List<String> queryPatients(String input, String orderField, String order)
    {
        List<String> queryResults = new LinkedList<>();

        String safeOrderField = "doc.name";
        if ("eid".equals(orderField)) {
            safeOrderField = "patient.external_id";
        }
        String safeOrder = " asc";
        if ("desc".equals(order)) {
            safeOrder = " desc";
        }

        StringBuilder querySb = new StringBuilder();
        querySb.append("select doc.name from  Document doc, doc.object(PhenoTips.PatientClass) as patient");
        querySb.append(" where doc.name <> :t and lower(doc.name) like :").append(INPUT_PARAMETER);
        querySb.append(" or lower(patient.external_id) like :").append(INPUT_PARAMETER);

        boolean usePatientName = this.configuration.getConfiguration("patient").getEnabledFieldNames()
            .contains(FIRST_NAME);
        if (usePatientName) {
            querySb.append(" or lower(patient.first_name) like :").append(INPUT_PARAMETER);
            querySb.append(" or lower(patient.last_name) like :").append(INPUT_PARAMETER);
        }

        querySb.append(" order by " + safeOrderField + safeOrder);
        try {
            Query query = this.qm.createQuery(querySb.toString(), Query.XWQL);
            query.bindValue("t", "PatientTemplate");
            String formattedInput = String.format(INPUT_FORMAT, input);
            query.bindValue(INPUT_PARAMETER, formattedInput);

            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing patiets query: [{}] ", e.getMessage());
            return Collections.emptyList();
        }
        return queryResults;
    }

    private JSONObject getPatientJSON(Patient patient, boolean markFamilyAssociation)
    {
        JSONObject patientJSON = new JSONObject();
        patientJSON.put("id", patient.getId());
        patientJSON.put("identifier", patient.getExternalId());

        // Add patient URL
        XWikiContext context = this.provider.get();
        String url = context.getWiki().getURL(patient.getDocumentReference(), "view", context);
        patientJSON.put("url", url);

        // Add description
        StringBuilder descriptionSb = new StringBuilder(patient.getId());
        String patientName = "";
        PatientData<String> patientNames = patient.getData("patientName");
        String firstName = StringUtils.defaultString(patientNames.get("first_name"));
        String lastName = StringUtils.defaultString(patientNames.get("last_name"));
        patientName = (firstName + " " + lastName).trim();
        if (StringUtils.isNotEmpty(patientName)) {
            descriptionSb.append(", name: ").append(patientName);
        }
        String patientExternalId = patient.getExternalId();
        if (StringUtils.isNotEmpty(patientExternalId)) {
            descriptionSb.append(", identifier: ").append(patientExternalId);
        }

        // Add family info
        Family family = this.familyRepository.getFamilyForPatient(patient);
        if (family != null) {
            patientJSON.put("familyId", family.getId());
            if (markFamilyAssociation) {
                descriptionSb.append(", in family: ").append(family.getId());
            }
        }

        patientJSON.put("textSummary", descriptionSb.toString());

        return patientJSON;
    }
}
