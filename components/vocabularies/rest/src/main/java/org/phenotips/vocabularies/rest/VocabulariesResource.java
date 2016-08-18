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
package org.phenotips.vocabularies.rest;

import org.phenotips.rest.ParentResource;
import org.phenotips.rest.Relation;
import org.phenotips.vocabularies.rest.model.Vocabularies;

import org.xwiki.rest.resources.RootResource;
import org.xwiki.stability.Unstable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Root resource for working with vocabularies, listing all the available vocabularies.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Unstable("New API introduced in 1.3")
@Path("/vocabularies")
@ParentResource(RootResource.class)
@Relation("https://phenotips.org/rel/vocabularies")
public interface VocabulariesResource
{
    /**
     * Entry resource for the Vocabularies RESTful API, provides a list of available vocabulary resources.
     *
     * @return a {@link Vocabularies} resource representing all the vocabularies that are currently available
     */
    @GET
    Vocabularies getAllVocabularies();
}
