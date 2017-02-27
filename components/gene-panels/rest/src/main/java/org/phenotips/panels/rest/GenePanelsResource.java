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
package org.phenotips.panels.rest;

import org.phenotips.rest.ParentResource;
import org.phenotips.rest.Relation;

import org.xwiki.rest.resources.RootResource;
import org.xwiki.stability.Unstable;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Root resource for working with gene panels.
 *
 * @version $Id$
 * @since 1.3
 */
@Unstable("New API introduced in 1.3")
@Path("/suggested-gene-panels")
@Relation("https://phenotips.org/rel/genePanels")
@ParentResource(RootResource.class)
public interface GenePanelsResource
{
    /**
     * Retrieves a JSON representation of genes associated with provided terms and counts for each gene. The following
     * request parameters are used:
     * <dl>
     * <dt>presentTerms</dt>
     * <dd>a list of term IDs that are observed to be present (e.g. HP:0001154)</dd>
     * <dt>absentTerms</dt>
     * <dd>a list of term IDs that are observed to be absent</dd>
     * <dt>startPage</dt>
     * <dd>the start page from which to display the results, numbering starts from 1</dd>
     * <dt>numResults</dt>
     * <dd>get the number of results to display, must be an integer</dd>
     * <dt>reqNo</dt>
     * <dd>the request number, must be an integer</dd>
     * </dl>
     *
     * @return associated genes and counts data if successful, an error code otherwise
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    Response getGeneCountsFromPhenotypes();
}
