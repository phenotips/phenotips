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

import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientRepository;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;
import org.xwiki.xml.XMLUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Handles export of information about families.
 *
 * @version $Id$
 */
@Component(roles = { PhenotipsFamilyExport.class })
@Singleton
public class PhenotipsFamilyExport
{
    private static final String LAST_NAME = "last_name";

    private static final String FIRST_NAME = "first_name";

    private static final String PERMISSIONS = "permissions";

    private static final String URL = "url";

    private static final String REPORTS = "reports";

    private static final String NAME = "name";

    private static final String IDENTIFIER = "identifier";

    private static final String ID = "id";

    private static final String INPUT_PARAMETER = "input";

    private static final String INPUT_FORMAT = "%%%s%%";

    private static final String FAMILY_ID = ID;

    private static final String FAMILY_EXTERNAL_ID = "externalId";

    private static final String FAMILY_WARNING = "warning";

    private static final String FAMILY_MEMBERS = "familyMembers";

    /** Runs queries for finding families. */
    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    @Inject
    private RecordConfigurationManager configuration;

    @Inject
    private Provider<XWikiContext> provider;

    /**
     * Returns a list of families by the input search criteria. The user has to have requiredPermissions on each family.
     * The list is returned as JSON if returnAsJSON is true or as HTML otherwise.
     *
     * @param input criterion to select families by
     * @param resultsLimit maximal number of results for each query
     * @param requiredPermissions permissions a user has to have over each family in the result
     * @param returnAsJSON if true, the result is returned as JSON, otherwise as HTML
     * @return list of families
     */
    public String searchFamilies(String input, int resultsLimit, String requiredPermissions, boolean returnAsJSON)
    {
        List<FamilySearchResult> resultsList = new LinkedList<FamilySearchResult>();
        queryFamilies(input, requiredPermissions, resultsLimit, resultsList);
        queryPatients(input, requiredPermissions, resultsLimit, resultsList);
        return formatResults(resultsList, returnAsJSON);
    }

    /**
     * returns information about the family in JSON format.
     *
     * @param family family
     * @return JSON object with family information
     */
    public JSONObject toJSON(Family family)
    {
        JSONObject familyJSON = new JSONObject();
        familyJSON.put(FAMILY_ID, family.getId());
        familyJSON.put(FAMILY_EXTERNAL_ID, family.getExternalId());
        familyJSON.put(FAMILY_WARNING, family.getWarningMessage());

        JSONArray patientsJSONArray = new JSONArray();
        for (Patient patient : family.getMembers()) {
            JSONObject patientJSON = getPatientInformationAsJSON(patient);
            patientsJSONArray.add(patientJSON);
        }
        familyJSON.put(FAMILY_MEMBERS, patientsJSONArray);

        return familyJSON;
    }

    private JSONObject getPatientInformationAsJSON(Patient patient)
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
        patientJSON.put(REPORTS, getMedicalReports(patient));

        // Patient URL
        XWikiContext context = this.provider.get();
        String url = context.getWiki().getURL(patient.getDocument(), "view", context);
        patientJSON.put(URL, url);

        // add permissions information
        User currentUser = this.userManager.getCurrentUser();
        JSONObject permissionJSON = new JSONObject();
        permissionJSON.put("hasEdit",
            this.authorizationService.hasAccess(currentUser, Right.EDIT, patient.getDocument()));
        permissionJSON.put("hasView",
            this.authorizationService.hasAccess(currentUser, Right.VIEW, patient.getDocument()));
        patientJSON.put(PERMISSIONS, permissionJSON);

        return patientJSON;
    }

    private void queryFamilies(String input, String requiredPermissions, int resultsLimit,
        List<FamilySearchResult> resultsList)
    {
        StringBuilder querySb = new StringBuilder();
        querySb.append("select doc.name ");
        querySb.append(" from  Document doc, ");
        querySb.append("       doc.object(PhenoTips.FamilyClass) as family ");
        querySb.append(" where lower(doc.name) like :").append(PhenotipsFamilyExport.INPUT_PARAMETER);
        querySb.append(" or lower(family.external_id) like :").append(PhenotipsFamilyExport.INPUT_PARAMETER);

        List<String> queryResults = runQuery(querySb.toString(), input, resultsLimit);

        // Process family query results
        for (String queryResult : queryResults) {
            Family family = this.familyRepository.getFamilyById(queryResult);
            if (family == null) {
                continue;
            }

            Right right = Right.toRight(requiredPermissions);
            if (!this.authorizationService.hasAccess(
                this.userManager.getCurrentUser(), right, family.getDocumentReference()))
            {
                continue;
            }

            resultsList.add(new FamilySearchResult(family, requiredPermissions));
        }
    }

    private void queryPatients(String input, String requiredPermissions, int resultsLimit,
        List<FamilySearchResult> resultsList)
    {
        StringBuilder querySb = new StringBuilder();
        querySb.append("from doc.object(PhenoTips.PatientClass) as patient, ");
        querySb.append(" doc.object(PhenoTips.FamilyReferenceClass) as familyref ");
        querySb.append("  where lower(doc.name) like :").append(PhenotipsFamilyExport.INPUT_PARAMETER);
        querySb.append(" or lower(patient.external_id) like :").append(PhenotipsFamilyExport.INPUT_PARAMETER);

        boolean usePatientName = this.configuration.getActiveConfiguration().getEnabledFieldNames()
            .contains(FIRST_NAME);
        if (usePatientName)
        {
            querySb.append(" or lower(patient.first_name) like :").append(PhenotipsFamilyExport.INPUT_PARAMETER);
            querySb.append(" or lower(patient.last_name) like :").append(PhenotipsFamilyExport.INPUT_PARAMETER);
        }

        List<String> queryResults = runQuery(querySb.toString(), input, resultsLimit);

        // Process family query results
        for (String queryResult : queryResults) {
            Patient patient = this.patientRepository.getPatientById(queryResult);
            if (patient == null) {
                continue;
            }

            Right right = Right.toRight(requiredPermissions);
            if (!this.authorizationService.hasAccess(this.userManager.getCurrentUser(), right, patient.getDocument())) {
                continue;
            }

            Family family = this.familyRepository.getFamilyForPatient(patient);
            if (family == null) {
                continue;
            }

            resultsList.add(new FamilySearchResult(patient, usePatientName, family, requiredPermissions));
        }
    }

    private List<String> runQuery(String queryString, String input, int resultsLimit)
    {
        String formattedInput = String.format(PhenotipsFamilyExport.INPUT_FORMAT, input);

        // Query patients
        Query query = null;
        List<String> queryResults = null;
        try {
            query = this.qm.createQuery(queryString, Query.XWQL);
            query.setLimit(resultsLimit);
            query.bindValue(PhenotipsFamilyExport.INPUT_PARAMETER, formattedInput);
            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing patiets query: [{}] ", e.getMessage());
            return Collections.emptyList();
        }
        return queryResults;
    }

    private String formatResults(List<FamilySearchResult> resultsList, boolean returnAsJSON)
    {
        JSONArray familyArray = null;
        JSONObject jsonResult = null;
        StringBuilder htmlResult = null;

        if (returnAsJSON) {
            familyArray = new JSONArray();
            jsonResult = new JSONObject();
        } else {
            htmlResult = new StringBuilder();
            htmlResult.append("<results>");
        }

        for (FamilySearchResult searchResult : resultsList) {
            if (returnAsJSON) {
                JSONObject familyJson = new JSONObject();
                familyJson.put(ID, searchResult.getId());
                familyJson.put(URL, searchResult.getUrl());
                familyJson.put(IDENTIFIER, searchResult.getExternalId());
                familyJson.put("textSummary", searchResult.getDescription());
                familyArray.add(familyJson);
            } else {
                String escapedReference = XMLUtils.escapeXMLComment(searchResult.getReference());
                String escapedDescription = XMLUtils.escapeXMLComment(searchResult.getDescription());

                htmlResult.append("<rs id=\"").append(searchResult.getUrl()).append("\" ");
                htmlResult.append("info=\"").append(escapedReference).append("\">");

                htmlResult.append(escapedDescription);

                htmlResult.append("</rs>");
            }
        }

        if (returnAsJSON) {
            jsonResult.put("matchedFamilies", familyArray);
            return jsonResult.toString();
        } else {
            htmlResult.append("</results>");
            return htmlResult.toString();
        }
    }

    /**
     * Returns all medical reports associated with a patient.
     *
     * @param patient to get medical reports for
     * @return Map with medical reports
     */
    public Map<String, String> getMedicalReports(Patient patient)
    {
        PatientData<String> links = patient.getData("medicalreports");
        Map<String, String> mapOfLinks = new HashMap<>();

        if (this.authorizationService.hasAccess(this.userManager.getCurrentUser(), Right.VIEW, patient.getDocument())) {
            if (links != null) {
                Iterator<Map.Entry<String, String>> iterator = links.dictionaryIterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    mapOfLinks.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return mapOfLinks;
    }
}
