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

import org.xwiki.stability.Unstable;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A resource for integrating a gene panel display with a LiveTable.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable("New API introduced in 1.4")
@Path("/suggested-gene-panels/livetable")
@Relation("https://phenotips.org/rel/genePanels")
@ParentResource(GenePanelsResource.class)
public interface GenePanelsLiveTableResource
{
    /**
     * Retrieves a JSON representation of genes associated with provided terms and counts for each gene. The following
     * request parameters are used:
     *
     * <dl>
     * <dt>present-term</dt>
     * <dd>a list of term IDs that are observed to be present (e.g. HP:0001154)</dd>
     * <dt>absent-term</dt>
     * <dd>a list of term IDs that are observed to be absent</dd>
     * <dt>rejected-gene</dt>
     * <dd>a list of gene IDs that were rejected and should be excluded from gene suggestion results</dd>
     * <dt>with-match-count</dt>
     * <dd>set to true iff the number of genes available for term should be counted</dd>
     * <dt>offset</dt>
     * <dd>the offset for the results, numbering starts from 1</dd>
     * <dt>limit</dt>
     * <dd>the number of results to display, must be an integer</dd>
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
