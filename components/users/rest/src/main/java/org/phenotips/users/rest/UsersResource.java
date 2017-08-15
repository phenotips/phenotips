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
package org.phenotips.users.rest;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Root resource for working with users and groups of users.
 *
 * @version $Id$
 * @since 1.4
 */
@Path("/users")
public interface UsersResource
{
    /**
     * Searches for users and/or groups matching the input parameter. Result is returned as JSON
     *
     * @param input string to look for
     * @param searchUsers if true, includes users in result
     * @param searchGroups if true, includes groups in result
     * @return a json object containing all results found
     */
    @GET
    Response searchUsersAndGroups(
        @QueryParam("input") @DefaultValue("0") String input,
        @QueryParam("searchUsers") @DefaultValue("true") boolean searchUsers,
        @QueryParam("searchGroups") @DefaultValue("false") boolean searchGroups);
}
