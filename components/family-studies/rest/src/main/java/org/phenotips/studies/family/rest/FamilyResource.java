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
package org.phenotips.studies.family.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Resource for working with family records, identified by their internal PhenoTips identifier.
 *
 * @version $Id$
 * @since 1.4
 */
@Path("/families/{id}")
public interface FamilyResource
{
    /**
     * Retrieve a family record, identified by its internal PhenoTips identifier, in its JSON representation. If the
     * indicated familyt record doesn't exist, or if the user sending the request doesn't have the right to view the
     * target family record, an error is returned.
     *
     * @param id the family's internal identifier, see {@link org.phenotips.studies.family.Family#getId()}
     * @return the JSON representation of the requested family, or a status message in case of error
     */
    @GET
    Response getFamily(@PathParam("id") String id);

    /**
     * Delete a family record, identified by its internal PhenoTips identifier. If the indicated family record doesn't
     * exist, or if the user sending the request doesn't have the right to delete the target family record, no change is
     * performed and an error is returned.
     *
     * @param id the family's internal identifier, see {@link org.phenotips.studies.family.Family#getId()}
     * @param deleteMembers when {@code true}, all family members will be deleted as well.
     * @return a status message
     */
    @DELETE
    Response deleteFamily(@PathParam("id") String id,
        @QueryParam("delete_all_members") @DefaultValue("false") Boolean deleteMembers);
}
