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

import org.phenotips.Constants;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.EntityPermissionsPreferencesManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.users.UserManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The default implementation of {@link EntityPermissionsPreferencesManager}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class DefaultEntityPermissionsPreferencesManager implements EntityPermissionsPreferencesManager
{
    private static final String CONFIGURATION_CLASS_NAME = "ConfigurationClass";

    private static final String DEFAULT_OWNER_PROPERTY_NAME = "defaultOwner";

    private static final String DEFAULT_WORKGROUP_PROPERTY_NAME = "defaultWorkgroup";

    private static final String DEFAULT_COLLABORATOR_PROPERTY_NAME = "defaultCollaborator";

    private static final String DEFAULT_VISIBILITY_PROPERTY_NAME = "defaultVisibility";

    private static final String DEFAULT_STUDY_PROPERTY_NAME = "defaultStudy";

    private static final String PROPERTY_NAME = "property";

    private static final String VALUE_NAME = "value";

    @Inject
    private Logger logger;

    @Inject
    private EntityAccessHelper helper;

    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    @Named("userOrGroup")
    private DocumentReferenceResolver<String> userOrGroupResolver;

    @Inject
    private GroupManager groupManager;

    @Inject
    private UserManager userManager;

    @Inject
    private EntityPermissionsManager permissions;

    @Inject
    @Named("none")
    private AccessLevel noAccess;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Override
    public DocumentReference getDefaultOwner(DocumentReference entity)
    {
        DocumentReference currentUserRef = entity == null ? this.helper.getCurrentUser() : entity;
        // Guest user or invalid entity
        if (currentUserRef == null) {
            return null;
        }

        String ownerID = getDefaultPreferenceStringValue(currentUserRef, DEFAULT_OWNER_PROPERTY_NAME);
        if (ownerID != null) {
            EntityReference defaultOwnerReference = this.userOrGroupResolver.resolve(ownerID);
            if (defaultOwnerReference != null) {
                return new DocumentReference(defaultOwnerReference);
            }
        }

        DocumentReference workGroupRef = getWorkgroupForUser(currentUserRef);
        // if the entity is not a user or user has no groups, we can not get further
        if (workGroupRef == null) {
            return null;
        }

        ownerID = getDefaultPreferenceStringValue(workGroupRef, DEFAULT_OWNER_PROPERTY_NAME);
        if (ownerID != null) {
            EntityReference defaultOwnerReference = this.userOrGroupResolver.resolve(ownerID);
            if (defaultOwnerReference != null) {
                return new DocumentReference(defaultOwnerReference);
            }
        }

        return null;
    }

    @Override
    public Map<EntityReference, Collaborator> getDefaultCollaborators(DocumentReference entity)
    {
        DocumentReference currentUserRef = entity == null ? this.helper.getCurrentUser() : entity;
        // Guest user or invalid entity
        if (currentUserRef == null) {
            return Collections.emptyMap();
        }

        Map<EntityReference, Collaborator> defaultCollabsMap = getDefaultPreferenceCollaborators(currentUserRef);
        // here null value will indicate the absence of any defaultCollaborator properties stored
        if (defaultCollabsMap != null) {
            return defaultCollabsMap;
        }

        DocumentReference workGroupRef = getWorkgroupForUser(currentUserRef);
        // if the entity is not a user or user has no groups, we can not get further
        if (workGroupRef != null) {
            return getDefaultPreferenceCollaborators(workGroupRef);
        }

        return Collections.emptyMap();
    }

    @Override
    public Visibility getDefaultVisibility(DocumentReference entity)
    {
        try {
            DocumentReference currentUserRef = entity == null ? this.helper.getCurrentUser() : entity;
            // Guest user or invalid entity
            if (currentUserRef == null) {
                return null;
            }

            String visibilityName = getDefaultPreferenceStringValue(currentUserRef, DEFAULT_VISIBILITY_PROPERTY_NAME);
            if (visibilityName != null) {
                Visibility defaultVisibility =
                    this.componentManager.get().getInstance(Visibility.class, visibilityName);
                if (defaultVisibility != null) {
                    return defaultVisibility;
                }
            }

            DocumentReference workGroupRef = getWorkgroupForUser(currentUserRef);
            // if the entity is not a user or user has no groups, we can not get further
            if (workGroupRef == null) {
                return null;
            }

            visibilityName = getDefaultPreferenceStringValue(workGroupRef, DEFAULT_VISIBILITY_PROPERTY_NAME);
            if (visibilityName != null) {
                Visibility defaultVisibility =
                    this.componentManager.get().getInstance(Visibility.class, visibilityName);
                return defaultVisibility;
            }
        } catch (ComponentLookupException ex) {
            this.logger.error("Failed to get the default visibility for entity [{}]: {}", entity.toString(),
                ex.getMessage(), ex);
        }

        return null;
    }

    @Override
    public DocumentReference getDefaultStudy(DocumentReference entity)
    {
        DocumentReference currentUserRef = entity == null ? this.helper.getCurrentUser() : entity;
        // Guest user or invalid entity
        if (currentUserRef == null) {
            return null;
        }

        String studyID = getDefaultPreferenceStringValue(currentUserRef, DEFAULT_STUDY_PROPERTY_NAME);
        if (studyID != null) {
            return this.userOrGroupResolver.resolve(studyID);
        }

        DocumentReference workGroupRef = getWorkgroupForUser(currentUserRef);
        // if the entity is not a user or user has no groups, we can not get further
        if (workGroupRef == null) {
            return null;
        }

        studyID = getDefaultPreferenceStringValue(workGroupRef, DEFAULT_STUDY_PROPERTY_NAME);
        if (studyID != null) {
            return this.userOrGroupResolver.resolve(studyID);
        }

        return null;
    }

    private DocumentReference getWorkgroupForUser(DocumentReference userRef)
    {
        // if the entity is not a user we can not get further
        if (!this.helper.isUser(userRef)) {
            return null;
        }

        // check user workgroups for defaultOwner
        Set<Group> userGroups =
            this.groupManager.getGroupsForUser(this.userManager.getUser(userRef.getName()));

        // look for XWiki.ConfigurationClass objects with that property in that workgroup’s profile document
        if (userGroups.size() == 1) {
            return userGroups.iterator().next().getReference();
        }

        // If there is more than one workgroup, look for XWiki.ConfigurationClass object
        // with defaultWorkgoup property in the user profile document
        if (userGroups.size() > 1) {
            String defaultWorkgroupID = getDefaultPreferenceStringValue(userRef, DEFAULT_WORKGROUP_PROPERTY_NAME);
            if (defaultWorkgroupID == null) {
                return null;
            }

            // check if workgroup entity exists AND the workgroup reference is valid AND it’s one of this user’s
            // workgroups
            Group group = this.groupManager.getGroup(defaultWorkgroupID);
            if (group != null && userGroups.contains(group)) {
                return group.getReference();
            }
        }

        return null;
    }

    /**
     * Get the first object of the {@link XWiki.ConfigurationClass} object that has a field name value matching the
     * given propertyName. When none found this method will return null.
     *
     * @param entity entity that stores the property of interest
     * @param propertyName value of the default configuration preference object
     * @return the String corresponding to the value of the propertyName.
     */
    private String getDefaultPreferenceStringValue(DocumentReference entity, String propertyName)
    {
        try {
            DocumentReference classRef = new DocumentReference(entity.getWikiReference().getName(),
                Constants.XWIKI_SPACE, CONFIGURATION_CLASS_NAME);
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument(entity);
            BaseObject obj = doc.getXObject(classRef, PROPERTY_NAME, propertyName);
            if (obj != null) {
                String property = obj.getStringValue(VALUE_NAME);
                if (StringUtils.isNotBlank(property)) {
                    return property;
                }
            }
        } catch (Exception ex) {
            // Do nothing
        }
        return null;
    }

    /**
     * Get the list of the {@link XWiki.ConfigurationClass} objects that store "defaultCollaborator". When none found
     * this method will return empty map.
     *
     * @param entity entity that stores the property of interest
     * @return the map of collaborators mapped to their corresponding entities.
     */
    private Map<EntityReference, Collaborator> getDefaultPreferenceCollaborators(DocumentReference entity)
    {
        try {
            DocumentReference classRef = new DocumentReference(entity.getWikiReference().getName(),
                Constants.XWIKI_SPACE, CONFIGURATION_CLASS_NAME);
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument(entity);
            Collection<BaseObject> configurationObjects = doc.getXObjects(classRef);
            if (configurationObjects == null) {
                return null;
            }

            Map<EntityReference, Collaborator> map = new TreeMap<EntityReference, Collaborator>();
            for (BaseObject obj : configurationObjects) {
                if (obj != null && DEFAULT_COLLABORATOR_PROPERTY_NAME.equals(obj.getStringValue(PROPERTY_NAME))
                    && StringUtils.isNotBlank(obj.getStringValue(VALUE_NAME))) {

                    // defaultCollaborator property value is stored as <a user or workgroup referece>[^<access level]
                    // Level can be view, edit, manage. If level is missing or is invalid, the default is view
                    // Examples: "xwiki:Groups.BloodDisorders^manage", "xwiki:XWiki.Mary"
                    String[] collab = obj.getStringValue(VALUE_NAME).split("\\^");
                    EntityReference userOrGroup = this.userOrGroupResolver.resolve(collab[0], EntityType.DOCUMENT);
                    if (userOrGroup == null) {
                        continue;
                    }

                    AccessLevel access =
                        collab.length > 1 ? this.permissions.resolveAccessLevel(collab[1]) : new ViewAccessLevel();
                    // If the level is missing or invalid, use "view" access level
                    if (access.equals(this.noAccess)) {
                        access = new ViewAccessLevel();
                    }
                    map.put(userOrGroup, new DefaultCollaborator(userOrGroup, access, null));
                }
            }

            return map;
        } catch (Exception e) {
            // Do nothing
        }

        return null;
    }
}
