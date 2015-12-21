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
package org.phenotips.studies.family.script.response;

import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.Pedigree;

import net.sf.json.JSONObject;

/**
 * JSON Response to client. Formats information from StatusResponse.
 *
 * @version $Id$
 */
public class FamilyInfoJSONResponse extends AbstractJSONResponse
{
    private Family family;

    /**
     * Default constructor, takes no parameters.
     *
     * @param family The family for which info was requested.
     */
    public FamilyInfoJSONResponse(Family family) {
        this.family = family;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject response = new JSONObject();
        response.put("family", this.family.toJSON());

        // a new family may not have a pedigree
        Pedigree pedigree = this.family.getPedigree();
        response.put("pedigree", (pedigree == null) ? null : pedigree.getData());

        return response;
    }

    @Override
    public boolean isErrorResponse() {
        return false;
    }
}
