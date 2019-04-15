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
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.EntityAccessHelper;
import org.phenotips.data.permissions.rest.CollaboratorResource;
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.model.CollaboratorRepresentation;
import org.phenotips.data.permissions.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.permissions.rest.model.OwnerRepresentation;
import org.phenotips.data.permissions.rest.model.PrincipalRepresentation;
import org.phenotips.data.permissions.rest.model.PrincipalsRepresentation;
import org.phenotips.data.permissions.rest.model.UserSummary;
import org.phenotips.data.permissions.rest.model.VisibilityRepresentation;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.groups.GroupManager;
import org.phenotips.rest.Autolinker;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.xpn.xwiki.api.Document;

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

    @Inject
    private GroupManager groupManager;

    @Inject
    private EntityAccessHelper helper;

    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private AuthorizationManager rights;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    @Inject
    @Named("manage")
    private AccessLevel manageAccess;

    @Inject
    @Named("edit")
    private AccessLevel editAccess;

    @Inject
    @Named("view")
    private AccessLevel viewAccess;

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
            CollaboratorRepresentation collaboratorObject = this.createCollaboratorRepresentation(collaborator);

            collaboratorObject.withLinks(this.autolinker.get()
                .forSecondaryResource(CollaboratorResource.class, uriInfo)
                .withExtraParameters("collaborator-id", this.entitySerializer.serialize(collaborator.getUser()))
                .withGrantedRight(entityAccess.getAccessLevel().getGrantedRight())
                .build());

            result.withCollaborators(collaboratorObject);
        }

        return result;
    }

    @Override
    public CollaboratorRepresentation createCollaboratorRepresentation(Collaborator collaborator)
    {
        CollaboratorRepresentation result = this.loadUserSummary(
            new CollaboratorRepresentation(), collaborator.getUser(), collaborator.getType());
        result.withLevel(collaborator.getAccessLevel().getName());
        return result;
    }

    @Override
    public PrincipalsRepresentation createPrincipalsRepresentation(PrimaryEntity entity, String entityType,
        UriInfo uriInfo)
    {
        PrincipalsRepresentation result = new PrincipalsRepresentation();
        // map of added principals names to the pair of principals and principal access levels for easier comparison
        Map<String, Pair<PrincipalRepresentation, AccessLevel>> addedPrincipals = new HashMap<>();

        addEntity(entity, entityType, addedPrincipals, result, false);

        return result;
    }

    /**
     * Create PrincipalRepresentation of the entity's associated principals like owners, collaborators and/or family
     * members, including all group and subgroup members.
     *
     * @param entity to whom we create principal representations
     * @param entityType the type of the entity
     * @param addedPrincipals cash of already added principals with access levels
     * @param result REST representation for a list of principals that have access to the {@link PrimaryEntity}
     * @param isFamily true if an entity is a family member
     */
    private void addEntity(PrimaryEntity entity, String entityType,
        Map<String, Pair<PrincipalRepresentation, AccessLevel>> addedPrincipals, PrincipalsRepresentation result,
        boolean isFamily)
    {
        EntityAccess entityAccess = this.manager.getEntityAccess(entity);
        Owner owner = entityAccess.getOwner();
        String ownerName = owner.getUsername();

        if (owner == null || StringUtils.isBlank(ownerName)) {
            return;
        }

        String role = "owner";
        AccessLevel access = this.manageAccess;
        PrincipalRepresentation principal = null;

        // Adding entity owner. Two possible cases:
        // 1. It is primary entity owner - in that case we do not need any checks
        // 2. It is a family member owner

        if (isFamily) {
            if (addedPrincipals.containsKey(ownerName)) {
                principal = addedPrincipals.get(ownerName).getLeft();
                // - if this principal was already added as family collaborator with "view" access,
                // then we update access level to "edit" (because owner of family member always
                // has "edit" access rights to the family)
                AccessLevel savedAccess = addedPrincipals.get(ownerName).getRight();
                if (savedAccess.equals(this.viewAccess)) {
                    checkAccessRight(addedPrincipals.get(ownerName).getLeft(), addedPrincipals.get(ownerName)
                        .getRight(), this.editAccess, "family-member-owner", isFamily);
                }
            } else {
                // - if this principal was never added and is Admin,
                // then we save it save with access "manage" and role "admin".
                DocumentReference ownerDocument = this.resolver.resolve(owner.getUser());
                boolean isAdmin =
                    this.rights.hasAccess(Right.ADMIN, ownerDocument, owner.getUser().getRoot());

                if (isAdmin) {
                    role = "admin";
                } else {
                    role = "family-member-owner";
                    // owner of family member always has "edit" access rights to the family entity
                    access = this.editAccess;
                }
            }
        }

        if (principal == null) {
            // Entity was never added
            Document doc = this.helper.getDocument(owner.getUser());
            principal = this.createPrincipalRepresentation(owner.getUser(), owner.getType(),
                access.getName(), role, "", doc.getURL());
            result.withPrincipals(principal);

            addedPrincipals.put(ownerName, new ImmutablePair<PrincipalRepresentation, AccessLevel>(principal,
                access));

            // Adding entity's (owner's) group members (principals)
            if (owner.isGroup()) {
                addGroupMembers(addedPrincipals, result, principal.getId(), principal.getName(), access, role,
                    isFamily);
            }
        }

        // Adding entity's collaborators (principals)
        addEntityCollaborators(entityAccess, addedPrincipals, result, isFamily);

        // Adding entity's family members and add their principals (owners and collaborators) and principal's group
        // members
        if ("families".equals(entityType)) {
            addFamilyMembers(entity, addedPrincipals, result);
        }
    }

    /**
     * Create PrincipalRepresentation of the group members and add to the list of principals.
     *
     * @param addedPrincipals cash of already added principals with access levels
     * @param result REST representation for a list of principals that have access to the {@link PrimaryEntity}
     * @param groupId ID of the group
     * @param groupName group name
     * @param accessLevel access level of the group to be inherited by all group members
     * @param role the group role to be inherited by all group members
     * @param isFamily true if a group is an owner or a collaborator of a family
     */
    private void addGroupMembers(Map<String, Pair<PrincipalRepresentation, AccessLevel>> addedPrincipals,
        PrincipalsRepresentation result, String groupId, String groupName, AccessLevel accessLevel, String role,
        boolean isFamily)
    {
        Set<Document> groupMemberDocs = this.groupManager.getAllMembersForGroup(groupId);

        for (Document member : groupMemberDocs) {

            String memberName = member.getName();
            AccessLevel memberAccess = accessLevel;
            String memberRole = role;

            boolean isAdmin =
                this.rights.hasAccess(Right.ADMIN, member.getDocumentReference(),
                    member.getDocumentReference().getRoot());

            // Adding group members
            if (!isFamily && "owner".equals(memberName)) {
                // If a primary entity owner is a group and we adding its' members
                // - no duplication checks and access updates needed
                continue;
            } else {
                // update access if user already added if needed: pick higher b/w saved and groups' passed accesses
                if (addedPrincipals.containsKey(memberName)) {
                    if (!this.manageAccess.equals(addedPrincipals.get(memberName).getRight())) {
                        checkAccessRight(addedPrincipals.get(memberName).getLeft(), addedPrincipals.get(memberName)
                            .getRight(), accessLevel, memberRole, isFamily);
                    }
                    continue;
                }
            }

            // if isAdmin -> save with access "manage" with role "admin"
            if (isAdmin) {
                memberRole = "admin";
                memberAccess = this.manageAccess;
            }

            EntityReference ref = member.getDocumentReference();

            PrincipalRepresentation principal = this.createPrincipalRepresentation(ref,
                this.helper.getType(ref), memberAccess.getName(), memberRole, groupName, member.getURL());
            result.withPrincipals(principal);

            addedPrincipals.put(ref.getName(), new ImmutablePair<PrincipalRepresentation, AccessLevel>(principal,
                accessLevel));
        }
    }

    /**
     * Create PrincipalRepresentation of entity collaborators and add to the list of principals.
     *
     * @param entityAccess access level of the entity whose collaborators we adding
     * @param addedPrincipals cash of already added principals with access levels
     * @param result REST representation for a list of principals that have access to the {@link PrimaryEntity}
     * @param isFamily true if a group is an owner or a collaborator of a family
     */
    private void addEntityCollaborators(EntityAccess entityAccess,
        Map<String, Pair<PrincipalRepresentation, AccessLevel>> addedPrincipals, PrincipalsRepresentation result,
        boolean isFamily)
    {
        Collection<Collaborator> collaborators = entityAccess.getCollaborators();

        for (Collaborator collaborator : collaborators) {

            AccessLevel collabAccess = collaborator.getAccessLevel();
            String role = "collaborator";

            if (isFamily) {
                role = "family-member-collaborator";
                // family member collaborators with manage access can have only edit access to family itself
                collabAccess = (collabAccess == this.manageAccess) ? this.editAccess : collabAccess;
            }

            DocumentReference collabDocument = this.resolver.resolve(collaborator.getUser());
            boolean isAdmin =
                this.rights.hasAccess(Right.ADMIN, collabDocument, collaborator.getUser().getRoot());
            if (isAdmin) {
                role = "admin";
                collabAccess = this.manageAccess;
            }

            // If a collaborator was already added - update role and access if needed
            String collabName = collaborator.getUsername();
            if (addedPrincipals.containsKey(collabName)) {
                checkAccessRight(addedPrincipals.get(collabName).getLeft(), addedPrincipals.get(collabName)
                    .getRight(), collabAccess, role, isFamily);
                continue;
            }

            Document collaboratorDoc = this.helper.getDocument(collaborator.getUser());
            PrincipalRepresentation principal = this.createPrincipalRepresentation(collaborator.getUser(),
                collaborator.getType(), collabAccess.getName(), role, "",
                collaboratorDoc.getURL());
            result.withPrincipals(principal);

            addedPrincipals.put(collabName, new ImmutablePair<PrincipalRepresentation, AccessLevel>(principal,
                collabAccess));

            if (collaborator.isGroup()) {
                addGroupMembers(addedPrincipals, result, principal.getId(), principal.getName(), collabAccess, role,
                    isFamily);
            }
        }
    }

    /**
     * Create PrincipalRepresentation of entity family members and add to the list of principals.
     *
     * @param entity to whom we create principal representations
     * @param addedPrincipals cash of already added principals with access levels
     * @param result REST representation for a list of principals that have access to the {@link PrimaryEntity}
     */
    private void addFamilyMembers(PrimaryEntity entity,
        Map<String, Pair<PrincipalRepresentation, AccessLevel>> addedPrincipals, PrincipalsRepresentation result)
    {
        Family family = this.familyRepository.get(entity.getId());
        List<Patient> members = family.getMembers();

        for (Patient member : members) {
            addEntity(member, "patients", addedPrincipals, result, true);
        }
    }

    // if we already added principal - checks the access right for added principal and updates access right and role
    // if the saved access right is lower that passed one
    private void checkAccessRight(PrincipalRepresentation principal, AccessLevel principalAccessLevel,
        AccessLevel accessLevelToCompare, String role, boolean isFamily)
    {
        if (principalAccessLevel.compareTo(accessLevelToCompare) < 0) {
            principal.setLevel(accessLevelToCompare.getName());
            principal.setRole(role);
        }
    }

    private PrincipalRepresentation createPrincipalRepresentation(EntityReference reference, String type, String level,
        String role, String group, String url)
    {
        PrincipalRepresentation result = this.loadUserSummary(new PrincipalRepresentation(), reference, type);
        result.withLevel(level);
        result.withRole(role);
        result.withGroup(group);
        result.withUrl(url);
        return result;
    }
}
