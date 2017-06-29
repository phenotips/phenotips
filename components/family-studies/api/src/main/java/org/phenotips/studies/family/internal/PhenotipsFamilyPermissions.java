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
package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.studies.family.Family;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Sets permissions of a family.
 *
 * @version $Id$
 */
@Component(roles = { PhenotipsFamilyPermissions.class })
@Singleton
public class PhenotipsFamilyPermissions
{
    /** XWiki class that contains rights to XWiki documents. */
    private static final EntityReference RIGHTS_CLASS =
        new EntityReference("XWikiRights", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    private static final String VIEW_RIGHTS = "view";

    private static final String VIEWEDIT_RIGHTS = "view,edit";

    private static final String VIEWEDITDELETE_RIGHTS = "view,edit,delete";

    private static final String RIGHTS_USERS_FIELD = "users";

    private static final String RIGHTS_LEVELS_FIELD = "levels";

    private static final String RIGHTS_GROUPS_FIELD = "groups";

    private static final String COMMA = ",";

    private static final String ALLOW = "allow";

    @Inject
    private Logger logger;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    @Named("view")
    private AccessLevel viewLevel;

    @Inject
    @Named("edit")
    private AccessLevel editLevel;

    @Inject
    @Named("manage")
    private AccessLevel manageLevel;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Returns all the users and groups that have the given right for the patient as array of two strings. First string
     * in returned array contains all the users that has this right for the patient, second string contains all the
     * groups that has the right.
     *
     * @param patient patient to read permissions from
     * @param targetAccessLevel an access level ('view','edit' or 'manage')
     * @return array of entity names
     */
    public String[] getEntitiesWithAccessAsString(Patient patient, AccessLevel targetAccessLevel)
    {
        String[] entityList = new String[2];

        int i = 0;
        for (Set<String> category : this.getEntitiesWithAccess(patient, targetAccessLevel)) {
            entityList[i] = setToString(category);
            i++;
        }
        return entityList;
    }

    /*
     * Returns all the users and groups that can edit the patient. First set in returned list contains all the users
     * that can edit the patient, second set contains all the groups that can edit the patient.
     */
    private List<Set<String>> getEntitiesWithAccess(Patient patient, AccessLevel targetAccessLevel)
    {
        Set<String> users = new HashSet<>();
        Set<String> groups = new HashSet<>();
        List<Set<String>> fullRights = new ArrayList<>();
        fullRights.add(users);
        fullRights.add(groups);
        PatientAccess access = this.permissionsManager.getPatientAccess(patient);
        for (Collaborator c : access.getCollaborators()) {
            if (targetAccessLevel.compareTo(c.getAccessLevel()) <= 0) {
                if (c.isGroup()) {
                    groups.add(this.serializer.serialize(c.getUser()));
                } else {
                    users.add(this.serializer.serialize(c.getUser()));
                }
            }
        }
        if (targetAccessLevel.compareTo(this.manageLevel) >= 0) {
            // Requested manager/owner access, include the owner in the result
            if (access.getOwner().isGroup()) {
                groups.add(this.serializer.serialize(access.getOwner().getUser()));
            } else {
                users.add(this.serializer.serialize(access.getOwner().getUser()));
            }
        }

        return fullRights;
    }

    /**
     * For every family member, read users and groups that have either view or edit edit access on the patient, then
     * gives the sam elevel of access on the family for those users and groups. After performing this method, if p is a
     * member of the family, and x has level y access on p, x has level y access on the family. The user who is the
     * owner of the family always has full access to the family. Note that the document is not saved to disk, changes
     * are only made for the provided Family object and its in-memory copy of the corresponding XWiki document.
     *
     * @param family to update permissions
     * @param context XWiki context to be used. The documnt will not be saved to disk, only changes in memory for the
     *            family document given will be made
     */
    public void updatePermissions(Family family, XWikiContext context)
    {
        List<Patient> members = family.getMembers();

        this.updatePermissions(members, family.getXDocument(), context);

        DocumentReference creatorReference = family.getXDocument().getCreatorReference();
        this.setOwnerPermissionsForUser(creatorReference == null ? "" : creatorReference.toString(),
            family.getXDocument(), context);
    }

    private void setOwnerPermissionsForUser(String user, XWikiDocument familyDocument, XWikiContext context)
    {
        // always give owner full rights
        BaseObject rightsObject = getOrCreateRightsObject(familyDocument, VIEWEDITDELETE_RIGHTS, context);
        rightsObject.set(RIGHTS_USERS_FIELD, user, context);
        rightsObject.set(RIGHTS_LEVELS_FIELD, VIEWEDITDELETE_RIGHTS, context);
        rightsObject.set(ALLOW, 1, context);
    }

    private void updatePermissions(List<Patient> members, XWikiDocument familyDocument, XWikiContext context)
    {
        Set<String> viewUsersUnion = new HashSet<>();
        Set<String> viewGroupsUnion = new HashSet<>();
        Set<String> editUsersUnion = new HashSet<>();
        Set<String> editGroupsUnion = new HashSet<>();

        for (Patient patient : members) {
            PatientAccess access = this.permissionsManager.getPatientAccess(patient);
            for (Collaborator c : access.getCollaborators()) {
                if (this.editLevel.compareTo(c.getAccessLevel()) <= 0) {
                    if (c.isGroup()) {
                        editGroupsUnion.add(this.serializer.serialize(c.getUser()));
                    } else {
                        editUsersUnion.add(this.serializer.serialize(c.getUser()));
                    }
                } else if (this.viewLevel.compareTo(c.getAccessLevel()) <= 0) {
                    if (c.isGroup()) {
                        viewGroupsUnion.add(this.serializer.serialize(c.getUser()));
                    } else {
                        viewUsersUnion.add(this.serializer.serialize(c.getUser()));
                    }
                }
            }
            if (access.getOwner().isGroup()) {
                editGroupsUnion.add(this.serializer.serialize(access.getOwner().getUser()));
            } else {
                editUsersUnion.add(this.serializer.serialize(access.getOwner().getUser()));
            }
        }

        BaseObject rightsObject = getOrCreateRightsObject(familyDocument, VIEW_RIGHTS, context);
        rightsObject.set(RIGHTS_USERS_FIELD, setToString(viewUsersUnion), context);
        rightsObject.set(RIGHTS_GROUPS_FIELD, setToString(viewGroupsUnion), context);
        rightsObject.set(RIGHTS_LEVELS_FIELD, VIEW_RIGHTS, context);
        rightsObject.set(ALLOW, 1, context);
        rightsObject = getOrCreateRightsObject(familyDocument, VIEWEDIT_RIGHTS, context);
        rightsObject.set(RIGHTS_USERS_FIELD, setToString(editUsersUnion), context);
        rightsObject.set(RIGHTS_GROUPS_FIELD, setToString(editGroupsUnion), context);
        rightsObject.set(RIGHTS_LEVELS_FIELD, VIEWEDIT_RIGHTS, context);
        rightsObject.set(ALLOW, 1, context);
    }

    private static String setToString(Set<String> set)
    {
        StringBuilder result = new StringBuilder();
        for (String item : set) {
            if (StringUtils.isNotBlank(item)) {
                result.append(item).append(COMMA);
            }
        }
        return result.toString();
    }

    /**
     * get the rights object for a family A document can have several rights objects.
     *
     * @return XWiki {@link BaseObject} that corresponds to the default rights
     */
    private BaseObject getOrCreateRightsObject(XWikiDocument familyDoc, String rightsLevel, XWikiContext context)
    {
        List<BaseObject> rights = familyDoc.getXObjects(RIGHTS_CLASS);
        for (BaseObject right : rights) {
            String level = right.getStringValue(RIGHTS_LEVELS_FIELD);
            if (StringUtils.equalsIgnoreCase(level, rightsLevel)) {
                return right;
            }
        }

        // rights object not found - create one
        try {
            BaseObject newRightObject = familyDoc.newXObject(RIGHTS_CLASS, context);
            return newRightObject;
        } catch (Exception ex) {
            this.logger.error("Can not create rights object for family {}", familyDoc.getId());
        }
        return null;
    }
}
