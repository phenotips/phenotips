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

import org.phenotips.rest.Autolinker;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.FamilyTools;
import org.phenotips.studies.family.rest.FamilyResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Default implementation for {@link FamilyResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("org.phenotips.studies.family.rest.internal.DefaultFamilyResourceImpl")
@Singleton
public class DefaultFamilyResourceImpl extends XWikiResource implements FamilyResource
{
    private static final String NO_SUCH_FAMILY_ERROR_MESSAGE = "No such family record: [{}]";

    @Inject
    private Logger logger;

    @Inject
    private FamilyRepository repository;

    @Inject
    private AuthorizationManager access;

    @Inject
    private UserManager users;

    @Inject
    private FamilyTools familyTools;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public Response getFamily(String id)
    {
        this.logger.warn("Retrieving family record [{}] via REST", id);
        Family family = this.repository.get(id);
        if (family == null) {
            this.logger.warn(NO_SUCH_FAMILY_ERROR_MESSAGE, id);
            return Response.status(Status.NOT_FOUND).build();
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            family.getDocumentReference())) {
            this.logger.error("View access denied to user [{}] on family record [{}]", currentUser, id);
            return Response.status(Status.FORBIDDEN).build();
        }
        JSONObject json = family.toJSON();
        json.put("links", this.autolinker.get().forResource(getClass(), this.uriInfo).build());
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public Response deleteFamily(String id, Boolean deleteMembers)
    {
        this.logger.warn("Deleting family record [{}] via REST, deleteAllMembers = [{}]", id, deleteMembers);
        Family family = this.repository.get(id);
        if (family == null) {
            this.logger.warn(NO_SUCH_FAMILY_ERROR_MESSAGE, id);
            return Response.status(Status.NOT_FOUND).build();
        }
        if (this.familyTools.currentUserCanDeleteFamily(id, deleteMembers)) {
            this.logger.error("Delete access denied to user [{}] for family record [{}] with deleteMemebers=[{}]",
                this.users.getCurrentUser(), id, deleteMembers);
            return Response.status(Status.FORBIDDEN).build();
        }
        if (!this.familyTools.deleteFamily(id, deleteMembers)) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        this.logger.warn("Deleted family record [{}]", id);
        return Response.noContent().build();
    }
}
