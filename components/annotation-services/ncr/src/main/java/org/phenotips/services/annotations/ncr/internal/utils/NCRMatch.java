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
 * A match object used for translating incoming Neural Concept Recognizer json responses into the json format expected
 * by the clinical-text-analysis-extension.
 *
 * @version $Id$
 * @since 1.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NCRMatch
{
    @JsonProperty("token")
    private NCRToken token;

    @JsonProperty("start")
    private int start;

    @JsonProperty("end")
    private int end;

    /**
     * Gets the token object.
     *
     * @return the {@link NCRToken}
     */
    @JsonProperty("token")
    NCRToken getToken()
    {
        return this.token;
    }

    /**
     * Sets the id for the matched token.
     *
     * @param id the id, as string, for {@link NCRToken}
     */
    @JsonProperty("hp_id")
    void setId(final String id)
    {
        final NCRToken tokenObj = new NCRToken();
        tokenObj.setId(id);
        this.token = tokenObj;
    }

    /**
     * Gets the starting position for the identified term.
     *
     * @return the start position, as integer
     */
    @JsonProperty("start")
    int getStart()
    {
        return this.start;
    }

    /**
     * Sets the {@code start} position for the identified term.
     *
     * @param start the start position, as integer
     */
    @JsonProperty("start")
    void setStart(final int start)
    {
        this.start = start;
    }

    /**
     * Gets the end position for the identified term.
     *
     * @return the end position, as integer
     */
    @JsonProperty("end")
    int getEnd()
    {
        return this.end;
    }

    /**
     * Sets the {@code end} position for the identified term.
     *
     * @param end the end position, as integer
     */
    @JsonProperty("end")
    void setEnd(final int end)
    {
        this.end = end;
    }
}
