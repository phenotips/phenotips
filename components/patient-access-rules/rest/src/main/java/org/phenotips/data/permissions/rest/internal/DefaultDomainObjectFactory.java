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
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.Relations;
import org.phenotips.data.permissions.script.SecurePatientAccess;
import org.phenotips.data.rest.model.Collaborators;
import org.phenotips.data.rest.model.Link;
import org.phenotips.data.rest.model.PatientVisibility;
import org.phenotips.data.rest.model.PhenotipsUser;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
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
    private DocumentReferenceResolver<String> stringResolver;

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

    private EntityReference userObjectReference;

    private EntityReference groupObjectReference;

    @Override
    public void initialize() throws InitializationException
    {
        this.userObjectReference = this.stringResolver.resolve("XWiki.XWikiUsers");
        this.groupObjectReference = this.stringResolver.resolve("PhenoTips.PhenoTipsGroupClass");
    }

    @Override
    public PhenotipsUser createPatientOwner(Patient patient)
    {
        // todo. is this allowed?
        PatientAccess patientAccess = new SecurePatientAccess(this.manager.getPatientAccess(patient), this.manager);
        Owner owner = patientAccess.getOwner();

        // links should be added at a later point, to allow the reuse of this method in different contexts

        return createPhenotipsUser(owner.getUser(), owner.getType(), owner.getUser());
    }

    private PhenotipsUser createPhenotipsUser(EntityReference user, String type, EntityReference reference)
    {
        PhenotipsUser result = new PhenotipsUser();

        result.withId(this.entitySerializer.serialize(user));
        result.withType(type);

        // there is a chance of not being able to retrieve the rest of the data,
        // which should not prevent the returning of `id` and `type`
        try {
            DocumentReference userRef = this.referenceResolver.resolve(reference);
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
    public PatientVisibility createPatientVisibility(Patient patient)
    {
        PatientVisibility result = new PatientVisibility();
        // todo. is this allowed?
        PatientAccess patientAccess = new SecurePatientAccess(this.manager.getPatientAccess(patient), this.manager);
        Visibility visibility = patientAccess.getVisibility();

        result.withLevel(visibility.getName());

        return result;
    }

    @Override
    public Collaborators createCollaborators(Patient patient, UriInfo uriInfo)
    {
        PatientAccess patientAccess = new SecurePatientAccess(this.manager.getPatientAccess(patient), this.manager);
        Collection<Collaborator> collaborators = patientAccess.getCollaborators();

        Collaborators result = new Collaborators();
        for (Collaborator collaborator : collaborators)
        {
            PhenotipsUser collaboratorObject = this.createCollaborator(collaborator);
            String href = uriInfo.getBaseUriBuilder().path(CollaboratorResource.class)
                .build(patient.getId(), collaborator.getUser().getName()).toString();
            collaboratorObject.withLinks(new Link().withRel(Relations.COLLABORATOR).withHref(href));

            result.withCollaborators(collaboratorObject);
        }

        return result;
    }

    @Override
    public PhenotipsUser createCollaborator(Patient patient, Collaborator collaborator)
    {
        return this.createPhenotipsUser(collaborator.getUser(), collaborator.getType(), collaborator.getUser());
    }

    private PhenotipsUser createCollaborator(Collaborator collaborator)
    {
        return this.createPhenotipsUser(collaborator.getUser(), collaborator.getType(), collaborator.getUser());
    }
}
