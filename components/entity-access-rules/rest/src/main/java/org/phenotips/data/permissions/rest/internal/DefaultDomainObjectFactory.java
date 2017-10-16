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

import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.rest.CollaboratorResource;
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.model.CollaboratorRepresentation;
import org.phenotips.data.permissions.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.permissions.rest.model.OwnerRepresentation;
import org.phenotips.data.permissions.rest.model.UserSummary;
import org.phenotips.data.permissions.rest.model.VisibilityRepresentation;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.rest.Autolinker;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.stability.Unstable;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Default implementation of {@link DomainObjectFactory}.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Unstable
@Component
@Singleton
public class DefaultDomainObjectFactory implements DomainObjectFactory
{
    @Inject
    @Named("secure")
    private EntityPermissionsManager manager;

    @Inject
    private NameAndEmailExtractor nameAndEmailExtractor;

    @Inject
    private EntityReferenceSerializer<String> entitySerializer;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public OwnerRepresentation createOwnerRepresentation(PrimaryEntity entity)
    {
        EntityAccess entityAccess = this.manager.getEntityAccess(entity);
        Owner owner = entityAccess.getOwner();

        // links should be added at a later point, to allow the reuse of this method in different contexts

        return loadUserSummary(new OwnerRepresentation(), owner.getUser(), owner.getType());
    }

    private <E extends UserSummary> E loadUserSummary(E result, EntityReference reference, String type)
    {
        result.withId(this.entitySerializer.serialize(reference));
        result.withType(type);
        Pair<String, String> nameAndEmail = this.nameAndEmailExtractor.getNameAndEmail(type, reference);
        if (nameAndEmail != null) {
            if (!StringUtils.isBlank(nameAndEmail.getLeft())) {
                result.withName(nameAndEmail.getLeft());
            }
            if (!StringUtils.isBlank(nameAndEmail.getRight())) {
                result.withEmail(nameAndEmail.getRight());
            }
        }
        return result;
    }

    @Override
    public VisibilityRepresentation createVisibilityRepresentation(PrimaryEntity entity)
    {
        EntityAccess entityAccess = this.manager.getEntityAccess(entity);
        Visibility visibility = entityAccess.getVisibility();

        return this.createVisibilityRepresentation(visibility);
    }

    @Override
    public VisibilityRepresentation createVisibilityRepresentation(Visibility visibility)
    {
        if (visibility == null) {
            return null;
        }
        return (new VisibilityRepresentation())
            .withLevel(visibility.getName())
            .withLabel(visibility.getLabel())
            .withDescription(visibility.getDescription());
    }

    @Override
    public CollaboratorsRepresentation createCollaboratorsRepresentation(PrimaryEntity entity, UriInfo uriInfo)
    {
        EntityAccess entityAccess = this.manager.getEntityAccess(entity);
        Collection<Collaborator> collaborators = entityAccess.getCollaborators();

        CollaboratorsRepresentation result = new CollaboratorsRepresentation();
        for (Collaborator collaborator : collaborators) {
            CollaboratorRepresentation collaboratorObject =
                this.createCollaboratorRepresentation(entityAccess, collaborator);

            collaboratorObject.withLinks(this.autolinker.get().forSecondaryResource(CollaboratorResource.class, uriInfo)
                .withExtraParameters("collaborator-id", this.entitySerializer.serialize(collaborator.getUser()))
                .withGrantedRight(entityAccess.getAccessLevel().getGrantedRight())
                .build());

            result.withCollaborators(collaboratorObject);
        }

        return result;
    }

    @Override
    public CollaboratorRepresentation createCollaboratorRepresentation(PrimaryEntity entity, Collaborator collaborator)
    {
        EntityAccess entityAccess = this.manager.getEntityAccess(entity);
        return this.createCollaboratorRepresentation(entityAccess, collaborator);
    }

    private CollaboratorRepresentation createCollaboratorRepresentation(EntityAccess entityAccess,
        Collaborator collaborator)
    {
        CollaboratorRepresentation result = this.loadUserSummary(
            new CollaboratorRepresentation(), collaborator.getUser(), collaborator.getType());
        result.withLevel(collaborator.getAccessLevel().getName());
        return result;
    }
}
