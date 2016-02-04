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

import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;
import org.phenotips.templates.data.Template;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @version $Id$
 */
@Component(roles = { TemplatesRepository.class })
@Singleton
public class TemplatesRepository
{
    private static final String INPUT_PARAMETER = "input";

    private static final String MATCHED_TEMPLATES = "matchedTemplates";

    private static final String UNRESTRICTED_TEMPLATES_ACCESS = "unrestricted";
    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private UserManager userManager;

    @Inject
    private GroupManager groupManager;

    /**
     * Returns a collection of templates that are available for the user. The
     * list is compiled based on the system property of templates visibility. If
     * templates are unrestricted, all templates will be returned. If the
     * templates are available based on group visibility, then only templates
     * for which the current user has permission will be returned.
     *
     * @return a collection of templates
     */
    public List<Template> getAllTemplatesForUser() {
        List<Template> templatesList = null;
        if (this.isTemplatesAccessUnrestricted()) {
            templatesList = this.getAllTemplates();
        } else {
            templatesList = new ArrayList<Template>();
            User currentUser = this.userManager.getCurrentUser();
            for (Group group : this.groupManager.getGroupsForUser(currentUser)) {
                for (Template templates : group.getTemplates()) {
                    if (!templatesList.contains(templates)) {
                        templatesList.add(templates);
                    }
                }
            }
        }

        Collections.sort(templatesList, new Comparator<Template>() {
            @Override
            public int compare(Template o1, Template o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return templatesList;
    }

    /**
     * Returns a JSON object with a list of templates, all with ids that fit a
     * search criterion. If the search criterion is null, it is ignored.
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
                templatesArray.add(templateJson);
            }
        }

        JSONObject result = new JSONObject();
        result.put(MATCHED_TEMPLATES, templatesArray);
        return result.toString();
    }

    /**
     * @return true is access to templates is unrestricted (and not by group subscription).
     */
    public boolean isTemplatesAccessUnrestricted()
    {
        return UNRESTRICTED_TEMPLATES_ACCESS.equals(this.getTemplatesSubmissionPreference());
    }

    /**
     * @return value of templates submission preference property
     */
    public String getTemplatesSubmissionPreference()
    {
        XWikiContext xContext = this.contextProvider.get();
        XWiki xwiki = xContext.getWiki();
        XWikiDocument prefsDoc = null;
        String objectsSpace = "XWiki";
        try {
            DocumentReference prefsref = new DocumentReference(xContext.getWikiId(), objectsSpace, "XWikiPreferences");
            prefsDoc = xwiki.getDocument(prefsref, xContext);
        } catch (XWikiException e) {
            this.logger.error("Error reading templates submission preferences.", e.getMessage());
            return null;
        }
        DocumentReference confRef = new DocumentReference(xContext.getWikiId(), objectsSpace, "ConfigurationClass");
        BaseObject result = prefsDoc.getXObject(confRef, "property", "study-visibility-option");
        if (result != null) {
            return result.getStringValue("value");
        } else {
            return null;
        }
    }

    private List<Template> getAllTemplates() {
        return this.queryTemplates(null, -1);
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
