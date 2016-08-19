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
package org.phenotips.data.permissions.rest;

import org.phenotips.data.permissions.rest.model.VisibilityOptionsRepresentation;
import org.phenotips.rest.ParentResource;
import org.phenotips.rest.Relation;

import org.xwiki.rest.resources.RootResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Resource for working with patient independent features of {@link org.phenotips.data.permissions.Visibility}.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Path("/permissions/visibility")
@Relation("https://phenotips.org/rel/visibilityOptions")
@ParentResource(RootResource.class)
public interface VisibilityOptionsResource
{
    /**
     * The system has several predefined options of visibility levels.
     *
     * @return all the visibility levels present in the system
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    VisibilityOptionsRepresentation getVisibilityOptions();
}
