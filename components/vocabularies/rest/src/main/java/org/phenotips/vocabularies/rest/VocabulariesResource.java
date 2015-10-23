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

import org.phenotips.vocabularies.rest.model.Vocabularies;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
/**
 * Root resource for working with vocabularies.
 *
 * @version $Id$
 * @since
 */
@Path("/vocabularies")
public interface VocabulariesResource
{
    /**
     * Entry resource for the Vocabularies RESTful API. Provides a list of available vocabulary resources.
     * @return A {@link Vocabularies} representing all the vocabularies that are currently available in PhenoTips.
     */
    @GET Vocabularies getAllVocabularies();
}
