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

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.rest.CollaboratorResource;
import org.phenotips.data.permissions.rest.CollaboratorsResource;
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.Relations;
import org.phenotips.data.permissions.rest.internal.utils.RESTActionResolver;
import org.phenotips.data.permissions.script.SecurePatientAccess;
import org.phenotips.data.rest.model.CollaboratorRepresentation;
import org.phenotips.data.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.rest.model.Link;
import org.phenotips.data.rest.model.UserSummary;
import org.phenotips.data.rest.model.VisibilityRepresentation;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.stability.Unstable;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link DomainObjectFactory}.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Unstable
@Component
@Singleton
public class DefaultDomainObjectFactory implements DomainObjectFactory, Initializable
{
    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Parses string representations of document references into proper references. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    @Inject
    private PermissionsManager manager;

    @Inject
    private Logger logger;

    @Inject
    private EntityReferenceSerializer<String> entitySerializer;

    @Inject
    private RESTActionResolver restActionResolver;

    private EntityReference userObjectReference;

    private EntityReference groupObjectReference;

    @Override
    public void initialize() throws InitializationException
    {
        this.userObjectReference = this.referenceResolver.resolve(
            new EntityReference("XWikiUsers", EntityType.DOCUMENT), new EntityReference("XWiki", EntityType.SPACE));
        this.groupObjectReference =
            this.referenceResolver.resolve(new EntityReference("PhenoTipsGroupClass", EntityType.DOCUMENT),
                new EntityReference("PhenoTips", EntityType.SPACE));
    }

    @Override
    public UserSummary createOwnerRepresentation(Patient patient)
    {
        // todo. this method should not return UserSummary - it should return OwnerRepresentation, but the class
        // generator doesn't want to generate OwnerRepresentation

        // todo. is this allowed?
        PatientAccess patientAccess = new SecurePatientAccess(this.manager.getPatientAccess(patient), this.manager);
        Owner owner = patientAccess.getOwner();

        // links should be added at a later point, to allow the reuse of this method in different contexts

        return loadUserSummary(new UserSummary(), owner.getUser(), owner.getType());
    }

    private UserSummary loadUserSummary(UserSummary result, EntityReference user, String type)
    {
        result.withId(this.entitySerializer.serialize(user));
        result.withType(type);

        // there is a chance of not being able to retrieve the rest of the data,
        // which should not prevent the returning of `id` and `type`
        try {
            DocumentReference userRef = this.referenceResolver.resolve(user);
            XWikiDocument entityDocument = (XWikiDocument) this.documentAccessBridge.getDocument(userRef);
            NameEmail nameEmail = new NameEmail(type, entityDocument);

            result.withName(nameEmail.getName());
            result.withEmail(nameEmail.getEmail());
        } catch (Exception ex) {
            this.logger.error("Could not load user's or group's document", ex.getMessage());
        }
        return result;
    }

    private class NameEmail
    {
        private String name;
        private String email;
        NameEmail(String type, XWikiDocument document) throws Exception
        {
            if (StringUtils.equals("group", type))
            {
                fetchFromGroup(document);
            } else if (StringUtils.equals("user", type)) {
                fetchFromUser(document);
            } else {
                throw new Exception("The type does not match any know (user) type");
            }
        }

        private void fetchFromUser(XWikiDocument document)
        {
            BaseObject userObj = document.getXObject(userObjectReference);
            this.email = userObj.getStringValue("email");
            StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append(userObj.getStringValue("first_name"));
            nameBuilder.append(" ");
            nameBuilder.append(userObj.getStringValue("last_name"));
            this.name = nameBuilder.toString().trim();
        }

        private void fetchFromGroup(XWikiDocument document)
        {
            BaseObject groupObject = document.getXObject(groupObjectReference);
            this.email = groupObject.getStringValue("contact");
            this.name = document.getDocumentReference().getName();
        }

        public String getName()
        {
            return name;
        }

        public String getEmail()
        {
            return email;
        }
    }

    @Override
    public VisibilityRepresentation createVisibilityRepresentation(Patient patient)
    {
        // todo. is this allowed?
        PatientAccess patientAccess = new SecurePatientAccess(this.manager.getPatientAccess(patient), this.manager);
        Visibility visibility = patientAccess.getVisibility();

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
    public CollaboratorsRepresentation createCollaboratorsRepresentation(Patient patient, UriInfo uriInfo)
    {
        PatientAccess patientAccess = new SecurePatientAccess(this.manager.getPatientAccess(patient), this.manager);
        Collection<Collaborator> collaborators = patientAccess.getCollaborators();

        CollaboratorsRepresentation result = new CollaboratorsRepresentation();
        for (Collaborator collaborator : collaborators)
        {
            CollaboratorRepresentation collaboratorObject =
                this.createCollaboratorRepresentation(patientAccess, collaborator);
            String href = uriInfo.getBaseUriBuilder().path(CollaboratorResource.class)
                .build(patient.getId(), collaborator.getUser().getName()).toString();
            collaboratorObject.withLinks(new Link().withRel(Relations.COLLABORATOR)
                    .withHref(href)
                    .withAllowedMethods(restActionResolver.resolveActions(CollaboratorsResource.class,
                            patientAccess.getAccessLevel())));

            result.withCollaborators(collaboratorObject);
        }

        return result;
    }

    @Override
    public CollaboratorRepresentation createCollaboratorRepresentation(Patient patient, Collaborator collaborator)
    {
        PatientAccess patientAccess = new SecurePatientAccess(this.manager.getPatientAccess(patient), this.manager);
        return this.createCollaboratorRepresentation(patientAccess, collaborator);
    }

    private CollaboratorRepresentation createCollaboratorRepresentation(
        PatientAccess patientAccess, Collaborator collaborator)
    {
        String accessLevel = patientAccess.getAccessLevel(collaborator.getUser()).toString();
        CollaboratorRepresentation result = (CollaboratorRepresentation) this.loadUserSummary(
            new CollaboratorRepresentation(), collaborator.getUser(), collaborator.getType());
        result.withLevel(accessLevel);
        return result;
    }
}
