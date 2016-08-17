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
package org.phenotips.data.permissions.rest.internal;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.rest.CollaboratorsResource;
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.internal.utils.PatientAccessContext;
import org.phenotips.data.permissions.rest.internal.utils.SecureContextFactory;
import org.phenotips.data.permissions.rest.model.CollaboratorRepresentation;
import org.phenotips.data.permissions.rest.model.CollaboratorsRepresentation;
import org.phenotips.rest.Autolinker;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.users.User;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * Default implementation for {@link CollaboratorsResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("org.phenotips.data.permissions.rest.internal.DefaultCollaboratorsResourceImpl")
@Singleton
public class DefaultCollaboratorsResourceImpl extends XWikiResource implements CollaboratorsResource
{
    @Inject
    private Logger logger;

    @Inject
    private SecureContextFactory secureContextFactory;

    @Inject
    @Named("userOrGroup")
    private DocumentReferenceResolver<String> userOrGroupResolver;

    @Inject
    private DomainObjectFactory factory;

    @Inject
    private PermissionsManager manager;

    @Inject
    private Container container;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public CollaboratorsRepresentation getCollaborators(String patientId)
    {
        this.logger.debug("Retrieving collaborators of patient record [{}] via REST", patientId);
        // Besides getting the patient, checks that the user has view access
        PatientAccessContext patientAccessContext = this.secureContextFactory.getReadContext(patientId);

        CollaboratorsRepresentation result =
            this.factory.createCollaboratorsRepresentation(patientAccessContext.getPatient(), this.uriInfo);

        result.withLinks(this.autolinker.get().forResource(this.getClass(), this.uriInfo)
            .withGrantedRight(patientAccessContext.getPatientAccess().getAccessLevel().getGrantedRight())
            .build());

        return result;
    }

    @Override
    public Response addCollaborators(CollaboratorsRepresentation collaborators, String patientId)
    {
        this.logger.debug("Adding {} collaborators to patient record [{}] via REST",
            collaborators.getCollaborators().size(), patientId);
        return this.setCollaborators(collaborators.getCollaborators(), patientId, false);
    }

    @Override
    public Response addCollaborators(String patientId)
    {
        this.logger.debug("Adding new collaborators to patient record [{}] via REST", patientId);
        List<Object> collaborators = this.container.getRequest().getProperties("collaborator");
        List<Object> accessLevels = this.container.getRequest().getProperties("level");

        PatientAccessContext patientAccessContext = this.secureContextFactory.getWriteContext(patientId);
        PatientAccess patientAccess = patientAccessContext.getPatientAccess();

        if (collaborators.size() != accessLevels.size()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("The number of collaborator identifiers and access levels don't match").build());
        }

        Map<EntityReference, AccessLevel> internalCollaborators = new LinkedHashMap<>(collaborators.size());
        for (int i = 0; i < collaborators.size(); ++i) {
            String collaboratorId = (String) collaborators.get(i);
            String accessLevelName = (String) accessLevels.get(i);
            patientAccessContext.checkCollaboratorInfo(collaboratorId, accessLevelName);
            internalCollaborators.put(this.userOrGroupResolver.resolve(collaboratorId),
                this.manager.resolveAccessLevel(accessLevelName));
        }

        for (Map.Entry<EntityReference, AccessLevel> e : internalCollaborators.entrySet()) {
            patientAccess.addCollaborator(e.getKey(), e.getValue());
        }
        this.manager.fireRightsUpdateEvent(patientId);
        return Response.ok().build();
    }

    @Override
    public Response deleteAllCollaborators(String patientId)
    {
        this.logger.debug("Deleting all collaborators from patient record [{}] via REST", patientId);
        return setCollaborators(Collections.<CollaboratorRepresentation>emptyList(), patientId, true);
    }

    @Override
    public Response setCollaborators(CollaboratorsRepresentation collaborators, String patientId)
    {
        this.logger.debug("Setting {} collaborators to patient record [{}] via REST",
            collaborators.getCollaborators().size(), patientId);
        return this.setCollaborators(collaborators.getCollaborators(), patientId, true);
    }

    private Response setCollaborators(Collection<CollaboratorRepresentation> collaborators, String patientId,
        boolean replace)
    {
        PatientAccessContext patientAccessContext = this.secureContextFactory.getWriteContext(patientId);
        PatientAccess patientAccess = patientAccessContext.getPatientAccess();

        Map<EntityReference, Collaborator> internalCollaborators = new LinkedHashMap<>();
        if (!replace) {
            for (Collaborator c : patientAccess.getCollaborators()) {
                internalCollaborators.put(c.getUser(), c);
            }
        }
        for (CollaboratorRepresentation collaborator : collaborators) {
            EntityReference collaboratorReference = this.userOrGroupResolver.resolve(collaborator.getId());
            patientAccessContext.checkCollaboratorInfo(collaborator.getId(), collaborator.getLevel());
            internalCollaborators.put(collaboratorReference,
                new StubCollaborator(collaboratorReference, this.manager.resolveAccessLevel(collaborator.getLevel())));
        }
        patientAccess.updateCollaborators(internalCollaborators.values());
        this.manager.fireRightsUpdateEvent(patientId);
        return Response.ok().build();
    }

    private static final class StubCollaborator implements Collaborator
    {
        private EntityReference user;

        private AccessLevel access;

        private StubCollaborator(EntityReference user, AccessLevel access)
        {
            this.user = user;
            this.access = access;
        }

        @Override
        public String getType()
        {
            return "";
        }

        @Override
        public boolean isUser()
        {
            return false;
        }

        @Override
        public boolean isGroup()
        {
            return false;
        }

        @Override
        public EntityReference getUser()
        {
            return this.user;
        }

        @Override
        public String getUsername()
        {
            return null;
        }

        @Override
        public AccessLevel getAccessLevel()
        {
            return this.access;
        }

        @Override
        public Collection<String> getAllUserNames()
        {
            return Collections.emptyList();
        }

        @Override
        public boolean isUserIncluded(User user)
        {
            return false;
        }
    }
}
