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

import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.Validation;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.xml.XMLUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Handles export of information about families.
 *
 * @version $Id$
 */
@Component(roles = { XWikiFamilyExport.class })
@Singleton
public class XWikiFamilyExport
{
    private static final String INPUT_PARAMETER = "input";

    private static final String INPUT_FORMAT = "%s%%";

    /** Runs queries for finding families. */
    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private Validation validation;

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

    private void queryFamilies(String input, String requiredPermissions, int resultsLimit,
        List<FamilySearchResult> resultsList)
    {
        StringBuilder querySb = new StringBuilder();
        querySb.append(" from doc.object(PhenoTips.FamilyClass) as family ");
        querySb.append(" where lower(doc.name) like :").append(XWikiFamilyExport.INPUT_PARAMETER);
        querySb.append(" or lower(family.external_id) like :").append(XWikiFamilyExport.INPUT_PARAMETER);

        List<String> queryResults = runQuery(querySb.toString(), input, resultsLimit);

        // Process family query results
        for (String queryResult : queryResults) {
            Family family = this.familyRepository.getFamilyById(queryResult);
            if (family == null) {
                continue;
            }

            if (!this.validation.hasAccess(family.getDocumentReference(), requiredPermissions)) {
                continue;
            }

            resultsList.add(new FamilySearchResult(family, requiredPermissions));
        }
    }

    private void queryPatients(String input, String requiredPermissions, int resultsLimit,
        List<FamilySearchResult> resultsList)
    {
    }

    private List<String> runQuery(String queryString, String input, int resultsLimit)
    {
        String formattedInput = String.format(XWikiFamilyExport.INPUT_FORMAT, input);

        // Query patients
        Query query = null;
        List<String> queryResults = null;
        try {
            query = this.qm.createQuery(queryString, Query.XWQL);
            query.setLimit(resultsLimit);
            query.bindValue(XWikiFamilyExport.INPUT_PARAMETER, formattedInput);
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
                familyJson.put("id", searchResult.getId());
                familyJson.put("url", searchResult.getUrl());
                familyJson.put("identifier", searchResult.getExternalId());
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
}
