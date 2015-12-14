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
package org.phenotips.studies.internal;

import org.phenotips.studies.data.Study;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @version $Id$
 */
@Component(roles = { StudiesRepository.class })
@Singleton
public class StudiesRepository
{
    private static final String INPUT_PARAMETER = "input";

    private static final String MATCHED_STUDIES = "matchedStudies";

    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    /**
     * Returns a JSON object with a list of studies, all with ids that fit a search criterion. If the search criterion
     * is null, it is ignored.
     *
     * @param input the beginning of the study id
     * @param resultsLimit maximal length of list
     * @return JSON object with a list of studies
     */
    public String searchStudies(String input, int resultsLimit)
    {
        List<Study> studies = this.queryStudies(input, resultsLimit);

        JSONArray studiesArray = new JSONArray();
        if (studies != null) {
            for (Study study : studies) {
                JSONObject studyJson = new JSONObject();
                studyJson.put("id", study.getId());
                studyJson.put("textSummary", study.getName());
                studiesArray.add(studyJson);
            }
        }

        JSONObject result = new JSONObject();
        result.put(MATCHED_STUDIES, studiesArray);
        return result.toString();
    }

    private List<Study> queryStudies(String input, int resultsLimit)
    {
        StringBuilder querySb = new StringBuilder();
        querySb.append(" from  doc.object(PhenoTips.StudyClass) as s ");
        querySb.append(" where doc.fullName <> 'PhenoTips.StudyTemplate'");
        if (input != null) {
            querySb.append(" and lower(doc.name) like :").append(StudiesRepository.INPUT_PARAMETER);
        }

        Query query = null;
        List<String> queryResults = null;
        try {
            query = this.qm.createQuery(querySb.toString(), Query.XWQL);
            if (resultsLimit > 0) {
                query.setLimit(resultsLimit);
            }
            if (input != null) {
                String formattedInput = String.format("%s%%", input);
                query.bindValue(StudiesRepository.INPUT_PARAMETER, formattedInput);
            }
            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing studies query: [{}] ", e.getMessage());
        }
        Collections.sort(queryResults, String.CASE_INSENSITIVE_ORDER);

        List<Study> studies = new ArrayList<Study>();
        if (queryResults != null) {
            for (String queryResult : queryResults) {
                Study s = new DefaultStudy(queryResult);
                studies.add(s);
            }
        }
        return Collections.unmodifiableList(studies);
    }

}
