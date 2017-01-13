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
import org.phenotips.vocabularies.rest.model.Category;

import org.xwiki.stability.Unstable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * A resource for working with an individual category that contains one or more
 * {@link org.phenotips.vocabulary.Vocabulary} objects.
 *
 * @version $Id $
 * @since 1.4
 */
@Unstable("New API introduced in 1.4")
@Path("/vocabularies/categories/{category}")
@ParentResource(CategoriesResource.class)
@Relation("https://phenotips.org/rel/category")
public interface CategoryResource
{
    /**
     * Retrieves the resource used for working with the specified category.
     *
     * @param categoryName the vocabulary category name, for example {@code phenotype}
     * @return a {@link Category} representation
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Category getCategory(@PathParam("category") String categoryName);
}
