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
package org.phenotips.templates.internal;

import org.phenotips.templates.data.Template;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @version $Id$
 */
@Component(roles = { TemplatesRepository.class })
@Singleton
public class TemplatesRepository
{
    private static final String INPUT_PARAMETER = "input";

    private static final String MATCHED_TEMPLATES = "matchedTemplates";

    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    /**
     * @return all templates
     */
    public List<Template> getAllTemplates()
    {
        List<Template> templates = this.queryTemplates(null, -1);
        Collections.sort(templates, new Comparator<Template>()
        {
            @Override
            public int compare(Template o1, Template o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return templates;
    }

    /**
     * Returns a JSON object with a list of templates, all with ids that fit a search criterion. If the search criterion
     * is null, it is ignored.
     *
     * @param input the beginning of the template id
     * @param resultsLimit maximal length of list
     * @return JSON object with a list of templates
     */
    public String searchTemplates(String input, int resultsLimit)
    {
        List<Template> templates = this.queryTemplates(input, resultsLimit);

        JSONArray templatesArray = new JSONArray();
        if (templates != null) {
            for (Template template : templates) {
                JSONObject templateJson = new JSONObject();
                templateJson.put("id", template.getId());
                templateJson.put("textSummary", template.getName());
                templatesArray.put(templateJson);
            }
        }

        JSONObject result = new JSONObject();
        result.put(MATCHED_TEMPLATES, templatesArray);
        return result.toString();
    }

    private List<Template> queryTemplates(String input, int resultsLimit)
    {
        StringBuilder querySb = new StringBuilder();
        querySb.append(" from  doc.object(PhenoTips.StudyClass) as s ");
        querySb.append(" where doc.fullName <> 'PhenoTips.StudyTemplate'");
        if (input != null) {
            querySb.append(" and lower(doc.name) like :").append(TemplatesRepository.INPUT_PARAMETER);
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
                query.bindValue(TemplatesRepository.INPUT_PARAMETER, formattedInput);
            }
            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing templates query: [{}] ", e.getMessage());
        }
        Collections.sort(queryResults, String.CASE_INSENSITIVE_ORDER);

        List<Template> templates = new ArrayList<Template>();
        if (queryResults != null) {
            for (String queryResult : queryResults) {
                Template t = new DefaultTemplate(queryResult);
                templates.add(t);
            }
        }
        return templates;
    }
}
