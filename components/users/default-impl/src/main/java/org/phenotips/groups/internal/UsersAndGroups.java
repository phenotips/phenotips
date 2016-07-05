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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.omg.CORBA.UNKNOWN;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component(roles = { UsersAndGroups.class })
@Singleton
public class UsersAndGroups
{
    /** Result returned in case getType() result is a user. */
    public static final String USER = "user";

    /** Result returned in case getType() result is a group. */
    public static final String GROUP = "group";

    /** Result returned in case getType() result is unknown. */
    public static final String UNKNOWN = "unknown";

    private static final EntityReference USER_CLASS = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final EntityReference GROUP_CLASS = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final String INPUT_PARAMETER = "input";

    private static final String INPUT_FORMAT = "%%%s%%";

    private static final String ID_KEY = "id";

    private static final String VALUE_KEY = "value";

    private static final String ICON_KEY = "icon";

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

    @Inject
    private DocumentAccessBridge bridge;

    static {
        StringBuilder usersQuerySb = new StringBuilder();
        usersQuerySb.append("from doc.object(XWiki.XWikiUsers) as user ");
        usersQuerySb.append(" where lower(doc.name) like :").append(UsersAndGroups.INPUT_PARAMETER);
        usersQuerySb.append(" or concat(concat(lower(user.first_name), ' '), lower(user.last_name)) like ");
        usersQuerySb.append(":").append(UsersAndGroups.INPUT_PARAMETER);
        usersQuerySb.append(" order by user.first_name, user.last_name");
        UsersAndGroups.usersQueryString = usersQuerySb.toString();

        StringBuilder groupsQuerySb = new StringBuilder();
        groupsQuerySb.append("from doc.object(PhenoTips.PhenoTipsGroupClass) as groups ");
        groupsQuerySb.append(" where lower(doc.name)  like :").append(UsersAndGroups.INPUT_PARAMETER);
        groupsQuerySb.append(" and doc.fullName <> 'PhenoTips.PhenoTipsGroupTemplate' ");
        groupsQuerySb.append(" order by doc.name");
        UsersAndGroups.groupsQueryString = groupsQuerySb.toString();
    }

    /**
     * Checks whether an {@link userOrGroup} is an entity that represents a user or a group.
     *
     * @param userOrGroup entity to check
     * @return {@link USER}, @{link GROUP}, or {@link UNKNOWN}
     */
    public String getType(EntityReference userOrGroup)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument((DocumentReference) userOrGroup);
            if (doc.getXObject(USER_CLASS) != null) {
                return USER;
            } else if (doc.getXObject(GROUP_CLASS) != null) {
                return GROUP;
            }
        } catch (Exception ex) {
            this.logger.error("Error in getType({})", userOrGroup.getName(), ex.getMessage());
        }
        return UNKNOWN;
    }

    /**
     * Searches for users and/or groups matching the input parameter. Result is returned as JSON
     *
     * @param input string to look for
     * @param searchUsers if true, includes users in result
     * @param searchGroups if true, includes groups in result
     * @return a json object containing all results found
     */
    public JSONObject search(String input, boolean searchUsers, boolean searchGroups)
    {
        String formattedInput = input.toLowerCase();
        formattedInput = String.format(UsersAndGroups.INPUT_FORMAT, input);

        JSONArray resultArray = new JSONArray();
        try {
            if (searchUsers) {
                runUsersQuery(resultArray, formattedInput);
            }

            if (searchGroups) {
                runGroupsQuery(resultArray, formattedInput);
            }
        } catch (Exception ex) {
            this.logger.error("Error in search ({})", input, ex.getMessage());
        }

        JSONObject result = new JSONObject();
        result.put("matched", resultArray);
        return result;
    }

    private void runUsersQuery(JSONArray resultArray, String formattedInput) throws Exception
    {
        List<String> queryResult = runQuery(UsersAndGroups.usersQueryString, formattedInput, 10);
        for (String userName : queryResult)
        {
            User user = this.userManager.getUser(userName);
            List<AttachmentReference> attachmentRefs =
                this.bridge.getAttachmentReferences((DocumentReference) user.getProfileDocument());
            String avatarURL = "";
            if (attachmentRefs.size() > 0) {
                avatarURL = this.bridge.getAttachmentURL(attachmentRefs.get(0), true);
            } else {
                Provider<XWikiContext> xcontextProvider =
                    ComponentManagerRegistry.getContextComponentManager().getInstance(XWikiContext.TYPE_PROVIDER);
                XWikiContext xcontext = xcontextProvider.get();
                XWiki xwiki = xcontext.getWiki();
                avatarURL = xwiki.getSkinFile("icons/xwiki/noavatar.png", xcontext);
            }
            String name = user.getName();
            if (StringUtils.isBlank(name)) {
                name = user.getUsername();
            }
            JSONObject o = createObject(user.getProfileDocument().toString(), name, avatarURL, USER);
            resultArray.put(o);
        }
    }

    private void runGroupsQuery(JSONArray resultArray, String formattedInput) throws Exception
    {
        List<String> queryResult = runQuery(UsersAndGroups.groupsQueryString, formattedInput, 10);
        for (String groupName : queryResult)
        {
            Group group = this.groupManager.getGroup(groupName);
            List<AttachmentReference> attachmentRefs = this.bridge.getAttachmentReferences(group.getReference());
            String avatarURL = "";
            if (attachmentRefs.size() > 0) {
                avatarURL = this.bridge.getAttachmentURL(attachmentRefs.get(0), true);
            } else {
                Provider<XWikiContext> xcontextProvider =
                    ComponentManagerRegistry.getContextComponentManager().getInstance(XWikiContext.TYPE_PROVIDER);
                XWikiContext xcontext = xcontextProvider.get();
                XWiki xwiki = xcontext.getWiki();
                avatarURL = xwiki.getSkinFile("icons/xwiki/noavatargroup.png", xcontext);
            }
            JSONObject o = createObject(groupName, group.getReference().getName(), avatarURL, GROUP);
            resultArray.put(o);
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
            query.bindValue(UsersAndGroups.INPUT_PARAMETER, formattedInput);
            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing query: [{}] ", queryString, e.getMessage());
            return Collections.emptyList();
        }
        return queryResults;
    }

    private JSONObject createObject(String id, String value, String avatar, String type)
    {
        JSONObject o = new JSONObject();

        StringBuilder idWithType = new StringBuilder();
        idWithType.append(id).append(";").append(type);
        o.put(UsersAndGroups.ID_KEY, idWithType.toString());
        o.put(UsersAndGroups.VALUE_KEY, value);
        o.put(UsersAndGroups.ICON_KEY, avatar);

        return o;
    }
}
