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
package org.phenotips.services.annotations.ncr.internal.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A token object used for translating incoming Neural Concept Recognizer json responses into the json format expected
 * by the clinical-text-analysis-extension.
 *
 * @version $Id$
 * @since 1.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class NCRToken
{
    /**
     * The token's id.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Get the token id.
     *
     * @return the id, as string
     */
    @JsonProperty("id")
    String getId()
    {
        return this.id;
    }

    /**
     * Set the id for token.
     *
     * @param id the token id
     */
    @JsonProperty("id")
    void setId(String id)
    {
        this.id = id;
    }
}
