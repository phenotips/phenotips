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

import org.phenotips.Constants;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;
import org.xwiki.xml.XMLUtils;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component(roles = { UsersAndGroups.class })
@Singleton
public class UsersAndGroups implements Initializable
{
    private static final EntityReference USER_CLASS = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        Constants.XWIKI_SPACE_REFERENCE);

    private static final EntityReference GROUP_CLASS = new EntityReference("PhenoTipsGroupClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    private static final String INPUT_PARAMETER = "input";

    private static final String INPUT_FORMAT = "%%%s%%";

    private static final String ID_KEY = "id";

    private static final String VALUE_KEY = "value";

    private static final String ICON_KEY = "icon";

    private static final String INFO_KEY = "info";

    private static final String DESC_KEY = "description";

    private static String usersQueryString;

    private static String groupsQueryString;

    private static String allUsersQueryString;

    private static String allGroupsQueryString;

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

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private EntityReferenceResolver<String> resolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> documentResolver;

    @Override
    public void initialize() throws InitializationException
    {

        StringBuilder usersQuerySb = new StringBuilder();
        usersQuerySb.append("from doc.object(XWiki.XWikiUsers) as user");
        usersQuerySb.append(" where lower(doc.name) like :").append(UsersAndGroups.INPUT_PARAMETER);
        usersQuerySb.append(" or concat(concat(lower(user.first_name), ' '), lower(user.last_name)) like ");
        usersQuerySb.append(":").append(UsersAndGroups.INPUT_PARAMETER);
        usersQuerySb.append(" order by user.first_name, user.last_name");
        UsersAndGroups.usersQueryString = usersQuerySb.toString();

        StringBuilder groupsQuerySb = new StringBuilder();
        groupsQuerySb.append("from doc.object(PhenoTips.PhenoTipsGroupClass) as groups");
        groupsQuerySb.append(" where concat(concat(lower(doc.name), ' '), lower(doc.title)) like ");
        groupsQuerySb.append(":").append(UsersAndGroups.INPUT_PARAMETER);
        groupsQuerySb.append(" and doc.fullName <> 'PhenoTips.PhenoTipsGroupTemplate'");
        groupsQuerySb.append(" order by doc.name");
        UsersAndGroups.groupsQueryString = groupsQuerySb.toString();

        StringBuilder allUsersQuerySb = new StringBuilder();
        allUsersQuerySb.append("from doc.object(XWiki.XWikiUsers) as user order by user.first_name, user.last_name");
        UsersAndGroups.allUsersQueryString = allUsersQuerySb.toString();

        StringBuilder allGroupsQuerySb = new StringBuilder();
        allGroupsQuerySb.append("from doc.object(PhenoTips.PhenoTipsGroupClass) as groups");
        allGroupsQuerySb.append(" where doc.fullName <> 'PhenoTips.PhenoTipsGroupTemplate'");
        allGroupsQuerySb.append(" order by doc.name");
        UsersAndGroups.allGroupsQueryString = allGroupsQuerySb.toString();
    }

    /**
     * Checks whether an {@link userOrGroup} is an entity that represents a user.
     *
     * @param userOrGroup entity to check
     * @return true if an entity represents a {@link USER}, false otherwise
     */
    public boolean isUser(EntityReference userOrGroup)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument((DocumentReference) userOrGroup);
            return doc.getXObject(USER_CLASS) != null;
        } catch (Exception ex) {
            this.logger.error("Error in isUser({})", userOrGroup.getName(), ex.getMessage());
        }
        return false;
    }

    /**
     * Checks whether an {@link userOrGroup} is an entity that represents a group.
     *
     * @param userOrGroup entity to check
     * @return true if an entity represents a {@link GROUP}, false otherwise
     */
    public boolean isGroup(EntityReference userOrGroup)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument((DocumentReference) userOrGroup);
            return doc.getXObject(GROUP_CLASS) != null;
        } catch (Exception ex) {
            this.logger.error("Error in isGroup({})", userOrGroup.getName(), ex.getMessage());
        }
        return false;
    }

    /**
     * Searches for users and/or groups matching the input parameter. Result is returned as JSON
     *
     * @param input string to look for
     * @param maxResults the maximum number of results to be returned
     * @param searchUsers if true, includes users in result
     * @param searchGroups if true, includes groups in result
     * @param returnAsJSON if true, the result is returned as JSON, otherwise as XML
     * @return a JSON or XML object containing all results found
     */
    public String search(String input, int maxResults, boolean searchUsers, boolean searchGroups,
        boolean returnAsJSON)
    {
        String formattedInput = String.format(UsersAndGroups.INPUT_FORMAT, input.toLowerCase());

        JSONArray resultArray = new JSONArray();
        try {
            if (searchUsers) {
                runUsersQuery(resultArray, UsersAndGroups.usersQueryString, formattedInput, maxResults);
            }

            if (searchGroups && resultArray.length() < maxResults) {
                runGroupsQuery(resultArray, UsersAndGroups.groupsQueryString, formattedInput,
                    maxResults - resultArray.length());
            }
        } catch (Exception ex) {
            this.logger.error("Error in search ({})", input, ex.getMessage());
        }

        if (returnAsJSON) {
            JSONObject result = new JSONObject();
            result.put("matched", resultArray);
            return result.toString();
        } else {
            StringBuilder xmlResult = new StringBuilder();
            xmlResult.append("<results>");
            for (Object object : resultArray) {
                JSONObject entityJson = (JSONObject) object;
                String escapedId = XMLUtils.escapeAttributeValue(entityJson.optString(UsersAndGroups.ID_KEY));
                String escapedInfo = XMLUtils.escapeAttributeValue(entityJson.optString(UsersAndGroups.INFO_KEY));
                String escapedValue = XMLUtils.escapeAttributeValue(entityJson.optString(UsersAndGroups.VALUE_KEY));
                String escapedDescription =
                    XMLUtils.escapeElementContent(entityJson.optString(UsersAndGroups.DESC_KEY));

                xmlResult.append("<rs id=\"").append(escapedId).append("\" ");
                xmlResult.append("icon=\"").append(entityJson.optString(UsersAndGroups.ICON_KEY)).append("\" ");
                xmlResult.append("value=\"").append(escapedValue).append("\" ");
                xmlResult.append("info=\"").append(escapedInfo).append("\">");

                xmlResult.append(escapedDescription);

                xmlResult.append("</rs>");
            }
            xmlResult.append("</results>");
            return xmlResult.toString();
        }
    }

    /**
     * Searches for all users and groups. Result is returned as JSON
     *
     * @return a json object containing all results found
     */
    public JSONObject getAll()
    {
        JSONArray resultArray = new JSONArray();
        try {
            runUsersQuery(resultArray, UsersAndGroups.allUsersQueryString, "", 0);
            runGroupsQuery(resultArray, UsersAndGroups.allGroupsQueryString, "", 0);
        } catch (Exception ex) {
            this.logger.error("Error in search for all users and groups ({})", ex.getMessage());
        }

        JSONObject result = new JSONObject();
        result.put("results", resultArray);
        return result;
    }

    private void runUsersQuery(JSONArray resultArray, String queryString, String formattedInput, int maxResults)
        throws Exception
    {
        List<String> queryResult = runQuery(queryString, formattedInput, maxResults);
        for (String userName : queryResult)
        {
            User user = this.userManager.getUser(userName);
            List<AttachmentReference> attachmentRefs =
                this.bridge.getAttachmentReferences((DocumentReference) user.getProfileDocument());
            String avatarURL = "";
            if (attachmentRefs.size() > 0) {
                avatarURL = this.bridge.getAttachmentURL(attachmentRefs.get(0), true);
            } else {
                XWikiContext xcontext = this.xcontextProvider.get();
                XWiki xwiki = xcontext.getWiki();
                avatarURL = xwiki.getSkinFile("icons/xwiki/noavatar.png", xcontext);
            }
            String name = user.getName();
            if (StringUtils.isBlank(name)) {
                name = user.getUsername();
            }
            JSONObject o = createObject(user.getUsername(), name, avatarURL, name, userName);
            resultArray.put(o);
            if (resultArray.length() == maxResults) {
                break;
            }
        }
    }

    private void runGroupsQuery(JSONArray resultArray, String queryString, String formattedInput, int maxResults)
        throws Exception
    {
        List<String> queryResult = runQuery(queryString, formattedInput, maxResults);
        for (String groupName : queryResult) {
            Group group = this.groupManager.getGroup(groupName);
            String avatarURL = getGroupAvatar(group);
            DocumentReference ref = group.getReference();
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument(ref);
            JSONObject o = createObject(ref.getName(), groupName, avatarURL, doc.getTitle(), groupName);
            resultArray.put(o);
        }
    }

    private String getGroupAvatar(Group group)
    {
        String imageName = (String) this.bridge.getProperty(group.getReference(),
            this.documentResolver.resolve(GROUP_CLASS), "image");
        String avatarURL = "";

        if (StringUtils.isNotBlank(imageName)) {
            AttachmentReference attachmentRef =
                new AttachmentReference(this.resolver.resolve(imageName, EntityType.ATTACHMENT, group.getReference()));
            if (attachmentExists(attachmentRef)) {
                avatarURL = this.bridge.getAttachmentURL(attachmentRef, true);
            }
        }

        if (StringUtils.isBlank(avatarURL)) {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            avatarURL = xwiki.getSkinFile("icons/xwiki/noavatargroup.png", xcontext);
        }

        return avatarURL;
    }

    private boolean attachmentExists(AttachmentReference attachment)
    {
        try {
            return this.bridge.getAttachmentReferences(attachment.getDocumentReference()).stream()
                .anyMatch(i -> StringUtils.equals(i.getName(), attachment.getName()));
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> runQuery(String queryString, String formattedInput, int resultsLimit)
    {
        Query query = null;
        List<String> queryResults = null;
        try {
            query = this.qm.createQuery(queryString, Query.XWQL);
            if (resultsLimit > 0) {
                query.setLimit(resultsLimit);
                query.setOffset(0);
            }
            if (StringUtils.isNotBlank(formattedInput)) {
                query.bindValue(UsersAndGroups.INPUT_PARAMETER, formattedInput);
            }
            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing query: [{}] ", queryString, e.getMessage());
            return Collections.emptyList();
        }
        return queryResults;
    }

    private JSONObject createObject(String id, String value, String avatar, String info, String desc)
    {
        JSONObject o = new JSONObject();
        o.put(UsersAndGroups.ID_KEY, id);
        o.put(UsersAndGroups.VALUE_KEY, value);
        o.put(UsersAndGroups.ICON_KEY, avatar);
        o.put(UsersAndGroups.INFO_KEY, info);
        o.put(UsersAndGroups.DESC_KEY, desc);
        return o;
    }
}
