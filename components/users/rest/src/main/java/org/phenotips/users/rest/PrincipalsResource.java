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

import org.phenotips.rest.ParentResource;
import org.phenotips.rest.Relation;

import org.xwiki.rest.resources.RootResource;
import org.xwiki.stability.Unstable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Root resource for working with users and groups of users.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable("New API introduced in 1.4")
@Path("/principals")
@ParentResource(RootResource.class)
@Relation("https://phenotips.org/rel/principals")
public interface PrincipalsResource
{
    /**
     * Entry resource for the principals RESTful API, provides a list of available principals (users of groups).
     *
     * @return a json object containing all results found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response getAllUsersAndGroups();
}
