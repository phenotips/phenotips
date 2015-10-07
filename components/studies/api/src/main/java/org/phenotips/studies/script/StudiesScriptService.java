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
package org.phenotips.studies.script;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.script.service.ScriptService;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @version $Id$
 */

@Component
@Named("studies")
@Singleton
public class StudiesScriptService implements ScriptService
{
    private static final String INPUT_PARAMETER = "input";

    private static final String MATCHED_STUDIES = "matchedStudies";

    /** Runs queries for finding families. */
    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    /**
     * Returns a JSON object with a list of studies, all with ids that fit a search criterion.
     *
     * @param input the beginning of the study id
     * @param resultsLimit maximal length of list
     * @return JSON object with a list of studies
     */
    public String searchStudies(String input, int resultsLimit)
    {
        StringBuilder querySb = new StringBuilder();
        querySb.append(" from  doc.object(PhenoTips.StudyClass) as s ");
        querySb.append(" where lower(doc.name) like :").append(StudiesScriptService.INPUT_PARAMETER);
        querySb.append(" and   doc.fullName <> 'PhenoTips.StudyTemplate'");

        String formattedInput = String.format("%s%%", input);

        Query query = null;
        List<String> queryResults = null;
        try {
            query = this.qm.createQuery(querySb.toString(), Query.XWQL);
            query.setLimit(resultsLimit);
            query.bindValue(StudiesScriptService.INPUT_PARAMETER, formattedInput);
            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing studies query: [{}] ", e.getMessage());
        }
        Collections.sort(queryResults, String.CASE_INSENSITIVE_ORDER);

        JSONArray studiesArray = new JSONArray();
        if (queryResults != null) {
            for (String queryResult : queryResults) {
                JSONObject studyJson = new JSONObject();
                studyJson.put("id", queryResult);
                studyJson.put("textSummary", queryResult.split("\\.")[1]);
                studiesArray.add(studyJson);
            }
        }

        JSONObject result = new JSONObject();
        result.put(MATCHED_STUDIES, studiesArray);
        return result.toString();
    }
}
