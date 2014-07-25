/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This listener is in charge of keeping the patient records' rights objects updated. There are 3 possible rights
 * combinations: "view", "view,edit", and "view,edit,delete". There are 3 rights objects that correspond to these
 * combinations.
 *
 * @version $Id$
 */
@Component
@Named("phenotips-patient-rights-updater")
@Singleton
public class RightsUpdateEventListener implements EventListener
{
    private static final EntityReference USER_CLASS = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final EntityReference GROUP_CLASS = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final EntityReference RIGHTS_CLASS = new EntityReference("XWikiRights", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    /** The list of all the possible rights combinations for this particular application. */
    private static final List<String> rightsCombinations = Arrays.asList("view", "view,edit", "view,edit,delete");

    private static final String USERS = "users";

    private static final String GROUPS = "groups";

    @Inject
    private Logger logger;

    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private PermissionsManager manager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringEntityResolver;

    @Override
    public String getName()
    {
        return "phenotips-patient-rights-updater";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;
        if (isPatient(doc)) {
            // Map of permissions to users/groups
            Map<String, Map<String, String>> oldRights = new HashMap<String, Map<String, String>>();
            Map<String, BaseObject> rightsObjects = findRights(doc);
            List<String> missingRights = findMissingRights(rightsObjects);
            clearRights(rightsObjects, oldRights);
            // Create rights after clearRights, because it saves unnecessary resetting of groups and users
            createRights(missingRights, rightsObjects, doc, context);
            updateDefaultRights(rightsObjects, doc);
            updateOwnerRights(rightsObjects, oldRights, doc);
            updateCollaboratorsRights(rightsObjects, doc);
        }
    }

    private boolean isPatient(XWikiDocument doc)
    {
        return (doc.getXObject(Patient.CLASS_REFERENCE) != null)
            && !"PatientTemplate".equals(doc.getDocumentReference().getName());
    }

    /**
     * Finds all existing rights objects.
     *
     * @param doc XWikiDocument
     * @return map of rights combination as keys, and corresponding existing rights objects as values
     */
    private Map<String, BaseObject> findRights(XWikiDocument doc)
    {
        List<BaseObject> allRights = doc.getXObjects(RIGHTS_CLASS);
        if (allRights == null) {
            return new HashMap<String, BaseObject>();
        }
        Map<String, BaseObject> rightsObjects = new HashMap<String, BaseObject>();
        for (BaseObject right : allRights) {
            // getXObjects returns an ArrayList that could be lacking elements
            if (right == null) {
                continue;
            }
            String rightLevel = right.getStringValue("levels");
            if (rightsCombinations.contains(rightLevel)) {
                rightsObjects.put(rightLevel, right);
            }
        }
        return rightsObjects;
    }

    /**
     * Finds the rights combinations that do not have a corresponding existing object.
     *
     * @param rightsObjects map of existing rights objects
     * @return a list of rights combinations that are missing
     */
    private List<String> findMissingRights(Map<String, BaseObject> rightsObjects)
    {
        List<String> missingRights = new LinkedList<String>(rightsCombinations);
        for (String rightLevel : rightsObjects.keySet()) {
            if (missingRights.contains(rightLevel)) {
                missingRights.remove(rightLevel);
            }
        }
        return missingRights;
    }

    /**
     * Clears all users and groups from the existing rights objects. Stores the old rights in case of reversion.
     */
    private void clearRights(Map<String, BaseObject> rightsObjects, Map<String, Map<String, String>> oldRights)
    {
        for (BaseObject right : rightsObjects.values()) {
            Map<String, String> entityList = new HashMap<String, String>();
            entityList.put(GROUPS, right.getStringValue(GROUPS));
            entityList.put(USERS, right.getStringValue(USERS));
            oldRights.put(right.getStringValue("levels"), entityList);
            right.setLargeStringValue(GROUPS, "");
            right.setLargeStringValue(USERS, "");
        }
    }

    /**
     * Loops over the {@code rightsCombinations} and attaches a new rights object for each combination to the document.
     *
     * @param rightsCombinations the string array containing all the combinations for which there should be an object
     *            created
     * @param rightsObjects the map of existing rights objects
     * @param doc XWikiDocument
     * @param context XWikiContext
     */
    private void createRights(List<String> rightsCombinations, Map<String, BaseObject> rightsObjects, XWikiDocument doc,
        XWikiContext context)
    {
        for (String rights : rightsCombinations) {
            try {
                BaseObject newRightObject = doc.newXObject(RIGHTS_CLASS, context);
                newRightObject.setStringValue("levels", rights);
                newRightObject.setIntValue("allow", 1);
                rightsObjects.put(rights, newRightObject);
            } catch (XWikiException ex) {
                this.logger.error("Failed to create rights: {}", ex.getMessage(), ex);
            }
        }
    }

    private void updateDefaultRights(Map<String, BaseObject> rightsObjects, XWikiDocument doc)
    {
        Visibility visibility = getVisibility(doc);
        if (visibility == null || "none".equals(visibility.getDefaultAccessLevel().getName())) {
            return;
        }
        BaseObject right;
        if ("view".equals(visibility.getDefaultAccessLevel().getName())) {
            right = rightsObjects.get("view");
        } else if ("edit".equals(visibility.getDefaultAccessLevel().getName())) {
            right = rightsObjects.get("view,edit");
        } else {
            return;
        }
        setRights(right, "groups", "XWiki.XWikiAllGroup");
    }

    private void updateOwnerRights(Map<String, BaseObject> rightsObjects, Map<String, Map<String, String>> oldRights, XWikiDocument doc)
    {
        String ownerPermissions = "view,edit,delete";
        DocumentReference owner = getOwner(doc);
        BaseObject right = rightsObjects.get(ownerPermissions);
        if (owner == null) {
            setRights(right, USERS, "");
        } else if (isUser(owner)) {
            setRights(right, USERS, owner.toString());
        } else if (isGroup(owner)) {
            setRights(right, GROUPS, owner.toString());
        } else {
            for (Map.Entry<String, String> oldOwnerRights : oldRights.get(ownerPermissions).entrySet()) {
                setRights(right, oldOwnerRights.getKey(), oldOwnerRights.getValue());
            }
        }
    }

    private void updateCollaboratorsRights(Map<String, BaseObject> rightsObjects, XWikiDocument doc)
    {
        for (Map.Entry<AccessLevel, List<DocumentReference>> entry : getCollaborators(doc).entrySet()) {
            BaseObject right;
            if ("manage".equals(entry.getKey().getName()) || "owner".equals(entry.getKey().getName())) {
                right = rightsObjects.get("view,edit,delete");
            } else if ("edit".equals(entry.getKey().getName())) {
                right = rightsObjects.get("view,edit");
            } else if ("view".equals(entry.getKey().getName())) {
                right = rightsObjects.get("view");
            } else {
                return;
            }
            List<String> users = new LinkedList<String>();
            List<String> groups = new LinkedList<String>();
            for (DocumentReference userOrGroup : entry.getValue()) {
                if (isUser(userOrGroup)) {
                    users.add(userOrGroup.toString());
                } else if (isGroup(userOrGroup)) {
                    groups.add(userOrGroup.toString());
                }
            }
            setRights(right, "users", StringUtils.join(users, ","));
            setRights(right, "groups", StringUtils.join(groups, ","));
        }
    }

    private Visibility getVisibility(XWikiDocument doc)
    {
        String visibility = null;
        BaseObject visibilityObj = doc.getXObject(Visibility.CLASS_REFERENCE);
        if (visibilityObj != null) {
            visibility = visibilityObj.getStringValue("visibility");
        }
        return this.manager.resolveVisibility(StringUtils.defaultIfBlank(visibility, "private"));
    }

    private DocumentReference getOwner(XWikiDocument doc)
    {
        String owner = null;
        BaseObject ownerObj = doc.getXObject(Owner.CLASS_REFERENCE);
        if (ownerObj != null) {
            owner = ownerObj.getStringValue("owner");
        }
        if (StringUtils.isNotBlank(owner)) {
            return this.stringEntityResolver.resolve(owner);
        }
        return null;
    }

    private Map<AccessLevel, List<DocumentReference>> getCollaborators(XWikiDocument doc)
    {
        Map<AccessLevel, List<DocumentReference>> collaborators =
            new LinkedHashMap<AccessLevel, List<DocumentReference>>();
        List<BaseObject> collaboratorObjects = doc.getXObjects(Collaborator.CLASS_REFERENCE);
        if (collaboratorObjects == null || collaboratorObjects.isEmpty()) {
            return Collections.emptyMap();
        }
        for (BaseObject collaborator : collaboratorObjects) {
            if (collaborator == null) {
                continue;
            }
            String collaboratorName = collaborator.getStringValue("collaborator");
            String accessName = collaborator.getStringValue("access");

            if (StringUtils.isBlank(collaboratorName) || StringUtils.isBlank(accessName)) {
                continue;
            }
            DocumentReference userOrGroup =
                this.stringEntityResolver.resolve(collaboratorName, doc.getDocumentReference());
            AccessLevel access = this.manager.resolveAccessLevel(accessName);
            List<DocumentReference> list = collaborators.get(access);
            if (list == null) {
                list = new LinkedList<DocumentReference>();
                collaborators.put(access, list);
            }
            list.add(userOrGroup);
        }
        return collaborators;
    }

    private boolean isUser(DocumentReference profile)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument(profile);
            return doc == null ? false : (doc.getXObject(USER_CLASS) != null);
        } catch (Exception e) {
        }
        return false;
    }

    private boolean isGroup(DocumentReference profile)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument(profile);
            return doc == null ? false : (doc.getXObject(GROUP_CLASS) != null);
        } catch (Exception e) {
        }
        return false;
    }

    private void setRights(BaseObject rightsObject, String field, String value)
    {
        String currentValue = rightsObject.getStringValue(field);
        if (!StringUtils.isEmpty(currentValue)) {
            currentValue += ",";
        }
        rightsObject.setLargeStringValue(field, currentValue + value);
    }
}
