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

import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;
import org.phenotips.studies.data.Study;

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
@Component(roles = { StudiesRepository.class })
@Singleton
public class StudiesRepository
{
    private static final String INPUT_PARAMETER = "input";

    private static final String MATCHED_STUDIES = "matchedStudies";

    private static final String UNRESTRICTED_STUDIES_ACCESS = "unrestricted";
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
     * Returns a JSON object with a /** Returns a collection of studies that are
     * available for the user. The list is compiled based on the system property
     * of studies visibility. If studies are unrestricted, all studies will be
     * returned. If the studies are available based on group visibility, then
     * only studies for which the current user has permission will be returned.
     *
     * @return a collection of studies
     */
    public List<Study> getAllStudiesForUser() {
        List<Study> studiesList = null;
        if (this.isStudyAccessUnrestricted()) {
            studiesList = this.queryStudies(null, -1);
        } else {
            studiesList = new ArrayList<Study>();
            User currentUser = this.userManager.getCurrentUser();
            for (Group group : this.groupManager.getGroupsForUser(currentUser)) {
                for (Study study : group.getStudies()) {
                    if (!studiesList.contains(study)) {
                        studiesList.add(study);
                    }
                }
            }
        }

        Collections.sort(studiesList, new Comparator<Study>() {
            @Override
            public int compare(Study o1, Study o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return studiesList;
    }

    /**
     * Returns a JSON object with a list of studies, all with ids that fit a
     * search criterion. If the search criterion is null, it is ignored.
     *
     * @param input
     *            the beginning of the study id
     * @param resultsLimit
     *            maximal length of list
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

    /**
     * @return true is access to study is unrestricted (and not by group subscription).
     */
    public boolean isStudyAccessUnrestricted()
    {
        return UNRESTRICTED_STUDIES_ACCESS.equals(this.getStudiesSubmissionPreference());
    }

    /**
     * @return value of study submission preference property
     */
    public String getStudiesSubmissionPreference()
    {
        XWikiContext xContext = this.contextProvider.get();
        XWiki xwiki = xContext.getWiki();
        XWikiDocument prefsDoc = null;
        String objectsSpace = "XWiki";
        try {
            DocumentReference prefsref = new DocumentReference(xContext.getWikiId(), objectsSpace, "XWikiPreferences");
            prefsDoc = xwiki.getDocument(prefsref, xContext);
        } catch (XWikiException e) {
            this.logger.error("Error reading studies submission preferences.", e.getMessage());
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
        return studies;
    }
}
