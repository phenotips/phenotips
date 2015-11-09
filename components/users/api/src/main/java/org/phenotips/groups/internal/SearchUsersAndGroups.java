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
package org.phenotips.groups.internal;

import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @version $Id$
 */
@Component(roles = { SearchUsersAndGroups.class })
@Singleton
public class SearchUsersAndGroups
{
    private static final String INPUT_PARAMETER = "input";

    private static final String INPUT_FORMAT = "%%%s%%";

    private static final String ID_KEY = "id";

    private static final String VALUE_KEY = "value";

    private static String usersQueryString;

    private static String groupsQueryString;

    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    @Inject
    private UserManager userManager;

    @Inject
    private GroupManager groupManager;

    static {
        StringBuilder usersQuerySb = new StringBuilder();
        usersQuerySb.append("from doc.object(XWiki.XWikiUsers) as user ");
        usersQuerySb.append(" where lower(doc.name) like :").append(SearchUsersAndGroups.INPUT_PARAMETER);
        usersQuerySb.append(" or concat(concat(lower(user.first_name), ' '), lower(user.last_name)) like ");
        usersQuerySb.append(":").append(SearchUsersAndGroups.INPUT_PARAMETER);
        usersQuerySb.append(" order by user.first_name, user.last_name");
        SearchUsersAndGroups.usersQueryString = usersQuerySb.toString();

        StringBuilder groupsQuerySb = new StringBuilder();
        groupsQuerySb.append("from doc.object(PhenoTips.PhenoTipsGroupClass) as groups ");
        groupsQuerySb.append(" where lower(doc.name)  like :").append(SearchUsersAndGroups.INPUT_PARAMETER);
        groupsQuerySb.append(" and doc.fullName <> 'PhenoTips.PhenoTipsGroupTemplate' ");
        groupsQuerySb.append(" order by doc.name");
        SearchUsersAndGroups.groupsQueryString = groupsQuerySb.toString();
    }

    /**
     * Searches for users and/or groups matching the input parameter. Result is returned as JSON
     *
     * @param input string to look for
     * @param searchUsers if true, includes users in result
     * @param searchGroups if true, includes groups in result
     * @return a json object containing all results found
     */
    public JSON search(String input, boolean searchUsers, boolean searchGroups)
    {
        String formattedInput = input.toLowerCase();
        formattedInput = String.format(SearchUsersAndGroups.INPUT_FORMAT, input);

        JSONArray resultArray = new JSONArray();
        if (searchUsers) {
            runUsersQuery(resultArray, formattedInput);
        }

        if (searchGroups) {
            runGroupsQuery(resultArray, formattedInput);
        }

        JSONObject result = new JSONObject();
        result.put("matched", resultArray);
        return result;
    }

    private void runUsersQuery(JSONArray resultArray, String formattedInput)
    {
        List<String> queryResult = runQuery(SearchUsersAndGroups.usersQueryString, formattedInput, 10);
        for (String userName : queryResult)
        {
            User user = this.userManager.getUser(userName);
            JSONObject o = createObject(userName, user.getUsername(), "user");
            resultArray.add(o);
        }
    }

    private void runGroupsQuery(JSONArray resultArray, String formattedInput)
    {
        List<String> queryResult = runQuery(SearchUsersAndGroups.groupsQueryString, formattedInput, 10);
        for (String groupName : queryResult)
        {
            Group group = this.groupManager.getGroup(groupName);
            JSONObject o = createObject(groupName, group.getReference().getName(), "group");
            resultArray.add(o);
        }
    }

    private List<String> runQuery(String queryString, String formattedInput, int resultsLimit)
    {
        Query query = null;
        List<String> queryResults = null;
        try {
            query = this.qm.createQuery(queryString, Query.XWQL);
            query.setLimit(resultsLimit);
            query.setOffset(0);
            query.bindValue(SearchUsersAndGroups.INPUT_PARAMETER, formattedInput);
            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing query: [{}] ", queryString, e.getMessage());
            return Collections.emptyList();
        }
        return queryResults;
    }

    private JSONObject createObject(String id, String value, String type)
    {
        JSONObject o = new JSONObject();

        StringBuilder idWithType = new StringBuilder();
        idWithType.append(id).append(";").append(type);
        o.put(SearchUsersAndGroups.ID_KEY, idWithType.toString());

        o.put(SearchUsersAndGroups.VALUE_KEY, value);

        return o;
    }
}
