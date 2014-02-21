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
        return Arrays.<Event> asList(new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;
        if (isPatient(doc)) {
            clearRights(doc);
            updateDefaultRights(doc, context);
            updateOwnerRights(doc, context);
            updateCollaboratorsRights(doc, context);
        }
    }

    private boolean isPatient(XWikiDocument doc)
    {
        return (doc.getXObject(Patient.CLASS_REFERENCE) != null)
            && !"PatientTemplate".equals(doc.getDocumentReference().getName());
    }

    private void clearRights(XWikiDocument doc)
    {
        doc.removeXObjects(RIGHTS_CLASS);
    }

    private void updateDefaultRights(XWikiDocument doc, XWikiContext context)
    {
        try {
            Visibility visibility = getVisibility(doc);
            if (visibility == null || "none".equals(visibility.getDefaultAccessLevel().getName())) {
                return;
            }
            BaseObject right = doc.newXObject(RIGHTS_CLASS, context);
            right.setLargeStringValue("groups", "XWiki.XWikiAllGroup");
            right.setIntValue("allow", 1);
            if ("view".equals(visibility.getDefaultAccessLevel().getName())) {
                right.setStringValue("levels", "view");
            } else if ("edit".equals(visibility.getDefaultAccessLevel().getName())) {
                right.setStringValue("levels", "view,edit");
            }
        } catch (XWikiException ex) {
            this.logger.error("Failed to update rights: {}", ex.getMessage(), ex);
        }
    }

    private void updateOwnerRights(XWikiDocument doc, XWikiContext context)
    {
        try {
            DocumentReference owner = getOwner(doc);
            if (owner == null || !(isUser(owner) || isGroup(owner))) {
                return;
            }
            BaseObject right = doc.newXObject(RIGHTS_CLASS, context);
            if (isUser(owner)) {
                right.setLargeStringValue("users", owner.toString());
            } else if (isGroup(owner)) {
                right.setLargeStringValue("groups", owner.toString());
            }
            right.setStringValue("levels", "view,edit,delete");
            right.setIntValue("allow", 1);
        } catch (XWikiException ex) {
            this.logger.error("Failed to update rights: {}", ex.getMessage(), ex);
        }
    }

    private void updateCollaboratorsRights(XWikiDocument doc, XWikiContext context)
    {
        try {
            for (Map.Entry<AccessLevel, List<DocumentReference>> entry : getCollaborators(doc).entrySet()) {
                BaseObject right = doc.newXObject(RIGHTS_CLASS, context);
                if ("manage".equals(entry.getKey().getName()) || "owner".equals(entry.getKey().getName())) {
                    right.setStringValue("levels", "view,edit,delete");
                } else if ("edit".equals(entry.getKey().getName())) {
                    right.setStringValue("levels", "view,edit");
                } else if ("view".equals(entry.getKey().getName())) {
                    right.setStringValue("levels", "view");
                }
                right.setIntValue("allow", 1);
                List<String> users = new LinkedList<String>();
                List<String> groups = new LinkedList<String>();
                for (DocumentReference userOrGroup : entry.getValue()) {
                    if (isUser(userOrGroup)) {
                        users.add(userOrGroup.toString());
                    } else if (isGroup(userOrGroup)) {
                        groups.add(userOrGroup.toString());
                    }
                }
                right.setLargeStringValue("users", StringUtils.join(users, ","));
                right.setLargeStringValue("groups", StringUtils.join(groups, ","));
            }
        } catch (XWikiException ex) {
            this.logger.error("Failed to update rights: {}", ex.getMessage(), ex);
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
        } else if (doc.getCreatorReference() != null) {
            return doc.getCreatorReference();
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
}
