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
package org.phenotips.users.rest.internal;

import org.phenotips.groups.internal.UsersAndGroups;
import org.phenotips.users.rest.UsersResource;
import org.phenotips.users.rest.UsersSuggestResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Default implementation for {@link UsersResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("org.phenotips.users.rest.internal.DefaultUsersSuggestResourceImpl")
@Singleton
public class DefaultUsersSuggestResourceImpl extends XWikiResource implements UsersSuggestResource
{
    /** The logging object. */
    @Inject
    private Logger logger;

    @Inject
    private UsersAndGroups usersAndGroups;

    /**
     * Searches for users and/or groups matching the input parameter. Result is returned as JSON
     *
     * @param input string to look for
     * @param maxResults The maximum number of results to be returned
     * @param searchUsers if true, includes users in result
     * @param searchGroups if true, includes groups in result
     * @return a json object containing all results found
     */
    @Override
    public Response searchUsersAndGroups(String input, @DefaultValue("10") int maxResults, boolean searchUsers,
        boolean searchGroups)
    {
        // Check if patient ID is provided.
        if (StringUtils.isBlank(input)) {
            this.logger.error("No search input is provided.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        JSONObject json = this.usersAndGroups.search(input, maxResults, searchUsers, searchGroups);
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
