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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Owner;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiGroupService;

/**
 * The default implementation of {@link EntityAccessManager}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class DefaultEntityAccessManager implements EntityAccessManager
{
    private static final String OWNER = "owner";

    private static final String NONE = "none";

    private static final String NULL_STR = "null";

    private static final String MANAGE = "manage";

    private static final String COLLABORATOR = "collaborator";

    private static final String ACCESS = "access";

    @Inject
    private Logger logger;

    @Inject
    private EntityAccessHelper helper;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> entitySerializer;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<EntityReference> partialEntityResolver;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> stringEntityResolver;

    @Inject
    private AuthorizationManager rights;

    @Inject
    @Named("none")
    private AccessLevel noAccess;

    @Nonnull
    @Override
    public Collection<AccessLevel> listAccessLevels()
    {
        final Collection<AccessLevel> result = listAllAccessLevels();
        result.removeIf(accessLevel -> !accessLevel.isAssignable());
        return result;
    }

    @Nonnull
    @Override
    public Collection<AccessLevel> listAllAccessLevels()
    {
        try {
            return new TreeSet<>(this.componentManager.get().getInstanceList(AccessLevel.class));
        } catch (final ComponentLookupException ex) {
            return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public AccessLevel resolveAccessLevel(@Nullable final String name)
    {
        try {
            if (StringUtils.isNotBlank(name)) {
                final AccessLevel accessLevel = this.componentManager.get().getInstance(AccessLevel.class, name);
                // The component manager doesn't seem to throw the exception if looking up component with wrong name...
                return accessLevel == null ? this.noAccess : accessLevel;
            }
        } catch (ComponentLookupException ex) {
            this.logger.warn("Invalid entity access level requested: {}", name);
        }
        return this.noAccess;
    }

    @Nonnull
    @Override
    public AccessLevel getAccessLevel(@Nullable final PrimaryEntity entity, @Nullable final EntityReference userOrGroup)
    {
        AccessLevel result = resolveAccessLevel(NONE);
        if (entity == null || userOrGroup == null) {
            return result;
        }
        try {
            final Owner ownerObj = getOwner(entity);
            final EntityReference owner = ownerObj == null ? null : ownerObj.getUser();
            final Collection<Collaborator> collaborators = getCollaborators(entity);
            final Set<DocumentReference> processedEntities = new HashSet<>();

            final Queue<DocumentReference> entitiesToCheck = new LinkedList<>();
            entitiesToCheck.add((DocumentReference) userOrGroup);

            AccessLevel currentItemAccess;
            DocumentReference currentItem;
            final XWikiContext context = this.xcontextProvider.get();
            final XWikiGroupService groupService = context.getWiki().getGroupService(context);
            while (!entitiesToCheck.isEmpty()) {
                currentItem = entitiesToCheck.poll();
                currentItemAccess = getAccessLevel(currentItem, owner, collaborators);
                if (currentItemAccess.compareTo(result) > 0) {
                    result = currentItemAccess;
                }
                processedEntities.add(currentItem);
                final Collection<DocumentReference> groups =
                    groupService.getAllGroupsReferencesForMember(currentItem, 0, 0, context);
                groups.removeAll(processedEntities);
                entitiesToCheck.addAll(groups);
            }
        } catch (final XWikiException ex) {
            this.logger.warn("Failed to compute access level for [{}] on [{}]: {}", userOrGroup, entity.getId(),
                ex.getMessage());
        }
        return result;
    }

    /**
     * Gets the access level for the {@code userOrGroup}.
     *
     * @param userOrGroup the {@link EntityReference} to the user or group of interest
     * @param owner the {@link EntityReference} to the owner of the entity
     * @param collaborators the {@link Collaborator}s associated with the entity
     * @return the {@link AccessLevel} for the {@code userOrGroup}
     */
    private AccessLevel getAccessLevel(
        @Nonnull final EntityReference userOrGroup,
        @Nullable final EntityReference owner,
        @Nonnull final Collection<Collaborator> collaborators)
    {
        if (userOrGroup.equals(owner)) {
            return resolveAccessLevel(OWNER);
        }
        return collaborators.stream()
            // Find collaborator user that is the same as userOrGroup.
            .filter(collaborator -> collaborator.getUser().equals(userOrGroup))
            // Get the access level.
            .map(Collaborator::getAccessLevel)
            // Only interested in the first such occurrence.
            .findFirst()
            // If there are no such occurrences, access level is "none".
            .orElseGet(() -> resolveAccessLevel(NONE));
    }

    @Override
    public boolean isAdministrator(@Nullable final PrimaryEntity entity)
    {
        return isAdministrator(entity, this.helper.getCurrentUser());
    }

    @Override
    public boolean isAdministrator(@Nullable final PrimaryEntity entity, @Nullable final DocumentReference user)
    {
        return !this.helper.isGroup(user)
            && entity != null
            && entity.getDocumentReference() != null
            && this.rights.hasAccess(Right.ADMIN, user, entity.getDocumentReference());
    }

    @Nullable
    @Override
    public Owner getOwner(@Nullable final PrimaryEntity entity)
    {
        if (entity == null || entity.getDocumentReference() == null) {
            return null;
        }
        final DocumentReference entityRef = entity.getDocumentReference();
        final DocumentReference classReference = this.partialEntityResolver.resolve(Owner.CLASS_REFERENCE, entityRef);
        final String owner = this.helper.getStringProperty(entity.getXDocument(), classReference, OWNER);
        if (StringUtils.isNotBlank(owner) && !NULL_STR.equals(owner)) {
            return new DefaultOwner(this.stringEntityResolver.resolve(owner, entityRef), this.helper);
        }
        return new DefaultOwner(null, this.helper);
    }

    @Override
    public boolean setOwner(@Nullable final PrimaryEntity entity, @Nullable final EntityReference userOrGroup)
    {
        if (entity == null || entity.getDocumentReference() == null) {
            return false;
        }
        final DocumentReference classReference = this.partialEntityResolver.resolve(Owner.CLASS_REFERENCE,
            entity.getDocumentReference());
        // Try to replace the current owner with the new owner.
        try {
            final Owner owner = getOwner(entity);
            return classReference != null
                && changeOwnership(entity, classReference, owner == null ? null : owner.getUser(), userOrGroup);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Changes ownership from {@code previousOwner} to {@code newOwner}.
     *
     * @param entity the {@link PrimaryEntity} the ownership of which is being updated
     * @param classReference the {@link DocumentReference} object for an owner
     * @param previousOwner the previous {@link EntityReference owner} for {@code entity}
     * @param newOwner the new {@link EntityReference owner} for {@code entity}
     * @return true iff ownership was updated successfully, throws an exception otherwise
     * @throws Exception if owner cannot be updated
     */
    private boolean changeOwnership(
        @Nonnull final PrimaryEntity entity,
        @Nonnull final DocumentReference classReference,
        @Nullable final EntityReference previousOwner,
        @Nullable final EntityReference newOwner) throws Exception
    {
        final String owner = newOwner != null
            ? this.entitySerializer.serialize(this.partialEntityResolver.resolve(newOwner))
            : StringUtils.EMPTY;

        final XWikiDocument entityXDoc = entity.getXDocument();
        this.helper.setProperty(entityXDoc, classReference, OWNER, owner);
        // If there was a distinct previous owner, make them a collaborator.
        if (previousOwner != null && !previousOwner.equals(newOwner)) {
            final Collaborator collab = new DefaultCollaborator(previousOwner, resolveAccessLevel(MANAGE), null);
            addCollaborator(entity, collab, false);
        }
        // If the new owner was once a collaborator, remove the collaborator.
        final Collaborator oldCollaborator = new DefaultCollaborator(newOwner, null, null);
        removeCollaborator(entity, oldCollaborator, false);
        // Save the changes to the document.
        final XWikiContext context = this.xcontextProvider.get();
        context.getWiki().saveDocument(entityXDoc, "Set owner: " + owner, true, context);
        return true;
    }

    @Nonnull
    @Override
    public Collection<Collaborator> getCollaborators(@Nullable final PrimaryEntity entity)
    {
        if (entity == null || entity.getDocumentReference() == null) {
            return Collections.emptySet();
        }
        final DocumentReference entityRef = entity.getDocumentReference();
        try {
            final XWikiDocument entityDoc = entity.getXDocument();
            final DocumentReference classRef = this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE,
                entityRef);
            final Collection<BaseObject> storedCollaborators = entityDoc.getXObjects(classRef);
            final Map<EntityReference, Collaborator> collaborators = storedCollaborators == null
                ? Collections.emptyMap()
                : storedCollaborators.stream()
                    // Take only the non-null references.
                    .filter(Objects::nonNull)
                    // Collect into a TreeMap.
                    .collect(TreeMap::new, (map, v) -> collectCollaborator(map, v, entityRef), TreeMap::putAll);
            return collaborators.values();
        } catch (Exception e) {
            // This should not happen;
            this.logger.error("Unexpected exception occurred when retrieving collaborators for entity [{}]", entity);
        }
        return Collections.emptySet();
    }

    /**
     * Determines the correct {@link AccessLevel} for {@link EntityReference}, and adds it to {@code map} as an updated
     * {@link Collaborator} object.
     *
     * @param map a map of {@link EntityReference} to {@link Collaborator} objects
     * @param baseObject a {@link BaseObject} for a collaborator
     * @param entityRef the {@link DocumentReference} for entity of interest
     */
    private void collectCollaborator(
        @Nonnull final Map<EntityReference, Collaborator> map,
        @Nonnull final BaseObject baseObject,
        @Nonnull final DocumentReference entityRef)
    {
        final String collaboratorName = baseObject.getStringValue(COLLABORATOR);
        final String accessName = baseObject.getStringValue(ACCESS);
        if (StringUtils.isNotBlank(collaboratorName) && StringUtils.isNotBlank(accessName)) {
            final EntityReference userOrGroup = this.stringEntityResolver.resolve(collaboratorName, entityRef);
            final AccessLevel access = resolveAccessLevel(accessName);
            if (map.containsKey(userOrGroup)) {
                if (access.compareTo(map.get(userOrGroup).getAccessLevel()) > 0) {
                    map.put(userOrGroup, new DefaultCollaborator(userOrGroup, access, this.helper));
                }
            } else {
                map.put(userOrGroup, new DefaultCollaborator(userOrGroup, access, this.helper));
            }
        }
    }

    @Override
    public boolean setCollaborators(
        @Nullable final PrimaryEntity entity,
        @Nullable final Collection<Collaborator> newCollaborators)
    {
        if (entity == null || entity.getDocumentReference() == null) {
            return false;
        }
        try {
            final XWikiDocument patientDoc = entity.getXDocument();
            final DocumentReference classReference = this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE,
                entity.getDocumentReference());
            final XWikiContext context = this.xcontextProvider.get();
            final Owner owner = getOwner(entity);
            final EntityReference ownerUser = (owner != null) ? owner.getUser() : null;
            patientDoc.removeXObjects(classReference);
            if (newCollaborators != null) {
                newCollaborators.stream()
                    // Don't want any null collaborator objects.
                    .filter(Objects::nonNull)
                    // Don't want any collaborators that have no user name.
                    .filter(collaborator -> collaborator.getUser() != null)
                    // Don't want any collaborators that are owner.
                    .filter(collaborator -> owner != null && !collaborator.getUser().equals(ownerUser))
                    // Save the properties for each collaborator.
                    .forEach(collaborator -> saveCollaboratorData(collaborator, patientDoc, classReference, context));
            }
            patientDoc.setAuthorReference(this.helper.getCurrentUser());
            patientDoc.setMetaDataDirty(true);
            context.getWiki().saveDocument(patientDoc, "Updated collaborators", true, context);
            return true;
        } catch (Exception e) {
            // This should not happen;
            this.logger.error("Unexpected exception occurred when setting collaborators [{}] for entity [{}]",
                newCollaborators, entity);
        }
        return false;
    }

    /**
     * Saves the {@code collaborator} data to the {@code entityDoc}.
     *
     * @param collaborator the {@link Collaborator} whose data will be saved
     * @param entityDoc the {@link XWikiDocument} entity document
     * @param classReference the collaborator {@link DocumentReference}
     * @param context the {@link XWikiContext}
     */
    private void saveCollaboratorData(
        @Nonnull final Collaborator collaborator,
        @Nonnull final XWikiDocument entityDoc,
        @Nonnull final DocumentReference classReference,
        @Nonnull final XWikiContext context)
    {
        try {
            final BaseObject baseObject = entityDoc.newXObject(classReference, context);
            baseObject.setStringValue(COLLABORATOR, this.entitySerializer.serialize(collaborator.getUser()));
            final AccessLevel accessLevel = collaborator.getAccessLevel();
            // If an access level is not provided, set the most restrictive one.
            final AccessLevel validAccessLevel = accessLevel != null ? accessLevel : this.noAccess;
            baseObject.setStringValue(ACCESS, validAccessLevel.getName());
        } catch (final XWikiException e) {
            // This should not happen;
            this.logger.error("Unexpected exception occurred when setting properties for collaborator [{}]",
                collaborator);
        }
    }

    @Override
    public boolean addCollaborator(@Nullable final PrimaryEntity entity, @Nullable final Collaborator collaborator)
    {
        return addCollaborator(entity, collaborator, true);
    }

    /**
     * Adds a {@code collaborator} for the {@code entity}.
     *
     * @param entity the {@link PrimaryEntity} of interest
     * @param collaborator the {@link Collaborator} to add
     * @param saveDocument true iff the document should be saved, false otherwise
     * @return true if the collaborator was added successfully, false otherwise
     */
    private boolean addCollaborator(
        @Nullable final PrimaryEntity entity,
        @Nullable final Collaborator collaborator,
        final boolean saveDocument)
    {
        if (entity == null || entity.getDocumentReference() == null || collaborator == null) {
            return false;
        }
        try {
            final XWikiDocument entityDoc = entity.getXDocument();
            final XWikiContext context = this.xcontextProvider.get();

            final DocumentReference absoluteUserOrGroup = this.partialEntityResolver.resolve(collaborator.getUser());
            final String user = collaborator.getUser() != null
                ? this.entitySerializer.serialize(absoluteUserOrGroup)
                : StringUtils.EMPTY;

            final BaseObject o = getOrCreateCollaboratorObj(entity.getDocumentReference(), entityDoc, user, context);

            o.setStringValue(COLLABORATOR, StringUtils.defaultString(user));
            o.setStringValue(ACCESS, collaborator.getAccessLevel().getName());

            if (saveDocument) {
                entityDoc.setAuthorReference(this.helper.getCurrentUser());
                entityDoc.setMetaDataDirty(true);
                context.getWiki().saveDocument(entityDoc, "Added collaborator: " + user, true, context);
            }
            return true;
        } catch (Exception e) {
            // This should not happen;
            this.logger.error("Unexpected exception occurred when adding a collaborator [{}]", collaborator);
        }
        return false;
    }

    /**
     * Gets the collaborator {@link BaseObject} if it already exists in {@code entityDoc}, otherwise creates a new one.
     *
     * @param entityRef the {@link DocumentReference} for entity of interest
     * @param entityDoc the {@link XWikiDocument} for {@code entity}
     * @param user the collaborator's user identity
     * @param context the {@link XWikiContext}
     * @return the collaborator {@link BaseObject}
     * @throws XWikiException if a new {@link BaseObject} cannot be created
     */
    private BaseObject getOrCreateCollaboratorObj(
        @Nonnull final DocumentReference entityRef,
        @Nonnull final XWikiDocument entityDoc,
        @Nonnull final String user,
        @Nonnull final XWikiContext context) throws XWikiException
    {
        final DocumentReference classReference = this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE,
            entityRef);
        final BaseObject o = entityDoc.getXObject(classReference, COLLABORATOR, user, false);
        return o == null ? entityDoc.newXObject(classReference, context) : o;
    }

    @Override
    public boolean removeCollaborator(@Nullable final PrimaryEntity entity, @Nullable final Collaborator collaborator)
    {
        return removeCollaborator(entity, collaborator, true);
    }

    /**
     * Removes the {@code collaborator} from the {@code entity}.
     *
     * @param entity the {@link PrimaryEntity} of interest
     * @param collaborator the {@link Collaborator} to be removed
     * @param saveDocument true iff the document should be saved, false otherwise
     * @return true iff the collaborator was removed successfully, false otherwise
     */
    private boolean removeCollaborator(
        @Nullable final PrimaryEntity entity,
        @Nullable final Collaborator collaborator,
        final boolean saveDocument)
    {
        if (entity == null || entity.getDocumentReference() == null || collaborator == null) {
            return false;
        }

        try {
            final XWikiDocument patientDoc = entity.getXDocument();
            final DocumentReference classReference = this.partialEntityResolver.resolve(Collaborator.CLASS_REFERENCE,
                entity.getDocumentReference());
            final XWikiContext context = this.xcontextProvider.get();
            final DocumentReference absoluteUserOrGroup = this.partialEntityResolver.resolve(collaborator.getUser());
            final String user = collaborator.getUser() != null
                ? this.entitySerializer.serialize(absoluteUserOrGroup)
                : StringUtils.EMPTY;

            final BaseObject o = patientDoc.getXObject(classReference, COLLABORATOR, user, false);
            if (o != null) {
                patientDoc.removeXObject(o);
                if (saveDocument) {
                    context.getWiki().saveDocument(patientDoc, "Removed collaborator: " + user, true, context);
                }
                return true;
            }
        } catch (Exception e) {
            // This should not happen;
            this.logger.error("Unexpected exception occurred when removing a collaborator [{}]", collaborator);
        }
        return false;
    }
}
