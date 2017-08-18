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
import org.phenotips.vocabularies.rest.model.Categories;

import org.xwiki.stability.Unstable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Resource for working with vocabulary categories, listing all the available categories.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable("New API introduced in 1.4")
@Path("/vocabularies/categories")
@ParentResource(VocabulariesResource.class)
@Relation("https://phenotips.org/rel/categories")
public interface CategoriesResource
{
    /**
     * Provides a list of all available vocabulary categories.
     *
     * @return a {@link Categories} resource representing all the vocabulary categories that are currently available
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Categories getAllCategories();
}
