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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An annotations object used for translating incoming Neural Concept Recognizer json responses into the json format
 * expected by the clinical-text-analysis-extension.
 *
 * @version $Id$
 * @since 1.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NCRResponse
{
    @JsonProperty("matches")
    private List<NCRMatch> matches;

    /**
     * Gets the list of retrieved {@link NCRMatch matches}.
     *
     * @return the list of {@link NCRMatch} objects
     */
    @JsonProperty("matches")
    public List<NCRMatch> getMatches()
    {
        return this.matches;
    }

    /**
     * Sets the list of {@code matches}.
     *
     * @param matches the list of {@link NCRMatch} objects
     */
    @JsonProperty("matches")
    void setMatches(final List<NCRMatch> matches)
    {
        this.matches = matches;
    }
}
