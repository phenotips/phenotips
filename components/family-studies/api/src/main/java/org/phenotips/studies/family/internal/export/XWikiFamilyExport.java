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

import org.xwiki.component.annotation.Component;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Singleton;

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
    }

    private void queryPatients(String input, String requiredPermissions, int resultsLimit,
        List<FamilySearchResult> resultsList)
    {
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
