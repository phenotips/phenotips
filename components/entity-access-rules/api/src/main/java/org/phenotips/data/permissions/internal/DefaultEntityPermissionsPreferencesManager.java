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

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The default implementation of {@link EntityPermissionsPreferencesManager}.
 *
 * @version $Id$
 * @since 1.5M1
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
    @Named("current")
    private DocumentReferenceResolver<String> stringResolver;

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
        DocumentReference sourceEntity = getSourceDocument(entity);
        if (sourceEntity == null) {
            return null;
        }

        String ownerID = getDefaultPreferenceStringValue(sourceEntity, DEFAULT_OWNER_PROPERTY_NAME);

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
        DocumentReference sourceEntity = getSourceDocument(entity);
        if (sourceEntity == null) {
            return Collections.emptyMap();
        }

        Map<EntityReference, Collaborator> defaultCollabsMap = getDefaultPreferenceCollaborators(sourceEntity);
        // here null or empty map value will indicate the absence of any defaultCollaborator properties stored
        if (!MapUtils.isEmpty(defaultCollabsMap)) {
            return defaultCollabsMap;
        }

        return Collections.emptyMap();
    }

    @Override
    public Visibility getDefaultVisibility(DocumentReference entity)
    {
        try {
            DocumentReference sourceEntity = getSourceDocument(entity);
            if (sourceEntity == null) {
                return null;
            }

            String visibilityName = getDefaultPreferenceStringValue(sourceEntity, DEFAULT_VISIBILITY_PROPERTY_NAME);
            if (visibilityName != null) {
                Visibility defaultVisibility =
                    this.componentManager.get().getInstance(Visibility.class, visibilityName);
                if (defaultVisibility != null) {
                    return defaultVisibility;
                }
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
        DocumentReference sourceEntity = getSourceDocument(entity);
        if (sourceEntity == null) {
            return null;
        }

        String studyID = getDefaultPreferenceStringValue(sourceEntity, DEFAULT_STUDY_PROPERTY_NAME);
        if (studyID != null) {
            return this.stringResolver.resolve(String.valueOf(studyID), "Studies");
        }

        return null;
    }

    /**
     * Track down the reference of the document to get owner permission setting from.
     *
     * @param docRef entity to start search for permissions settings from, either user of group
     * @return the actual document to read permissions settings from
     */
    @SuppressWarnings("checkstyle:ReturnCount")
    private DocumentReference getSourceDocument(DocumentReference docRef)
    {
        // Guest user or invalid entity
        if (docRef == null) {
            return null;
        }

        // if the entity is not a user we can not get further
        // a defaultWorkgroup could be defined only in users profiles
        if (!this.helper.isUser(docRef)) {
            return docRef;
        }

        // The docRef is a user, look whether defaultWorkgroup property is set
        String defaultWorkgroupID = getDefaultPreferenceStringValue(docRef, DEFAULT_WORKGROUP_PROPERTY_NAME);
        // if no defaultWorkgroup defined in user profile document, we return user profile itself
        if (defaultWorkgroupID == null) {
            return docRef;
        }

        // check if workgroup entity exists
        Group group = this.groupManager.getGroup(defaultWorkgroupID);
        // user profile defaultWorkgroup doesn't exist anymore, nothing to do
        if (group == null) {
            return null;
        }

        // check that this group is one of this user’s workgroups
        Set<Group> userGroups =
            this.groupManager.getGroupsForUser(this.userManager.getUser(docRef.getName()));

        // note that Group and default Group implementation do not overwrite equals(), thus
        // using Set.contains() with groupManager.getGroup(groupName) is not possible
        String groupName = group.toString();
        for (Group ugroup : userGroups) {
            if (ugroup.toString().equals(groupName)) {
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
     * @return the map of collaborators mapped to their corresponding entities or null.
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
