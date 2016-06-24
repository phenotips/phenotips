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
package org.phenotips.studies.family.rest.internal;

import org.phenotips.data.rest.Relations;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyTools;
import org.phenotips.studies.family.rest.PatientFamilyResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Default implementation for {@link PatientFamilyResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("org.phenotips.studies.family.rest.internal.DefaultPatientFamilyResourceImpl")
@Singleton
public class DefaultPatientFamilyResourceImpl extends XWikiResource implements PatientFamilyResource
{
    @Inject
    private Logger logger;

    @Inject
    private FamilyTools familyTools;

    @Override
    public Response getFamily(String id)
    {
        this.logger.debug("Retrieving patient [{}] family information via REST", id);

        Family family = familyTools.getFamilyForPatient(id);

        if (family == null) {
            this.logger.debug("No patint with id [{}] or the patient has no family", id);
            return Response.status(Status.NOT_FOUND).build();
        }

        JSONObject json = family.toJSON();
        JSONObject link = new JSONObject().accumulate("rel", Relations.SELF).accumulate("href",
                this.uriInfo.getRequestUri().toString());
        json.append("links", link);
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
