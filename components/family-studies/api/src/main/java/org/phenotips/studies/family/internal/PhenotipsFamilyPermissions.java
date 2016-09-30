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
import org.phenotips.studies.family.Family;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;

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

    /**
     * Returns all the users and groups that have the given right for the patient as array of two strings.
     * First string in returned array contains all the users that has this right for the patient,
     * second string contains all the groups that has the right.
     *
     * @param patientDoc patient to read permissions from
     * @param rightName a permission name ('view','edit' or 'delete'), if entity's permissions
     *        include this permission the entity is included in the result
     * @return array of entity names
     */
    public String[] getEntitiesWithAccessAsString(XWikiDocument patientDoc, String rightName)
    {
        String[] entityList = new String[2];

        // samity check - if given right is not contained in the full rights means it is a wrong accessString
        if (VIEWEDITDELETE_RIGHTS.contains(rightName)) {
            int i = 0;
            for (Set<String> category : this.getEntitiesWithAccess(patientDoc, rightName)) {
                entityList[i] = setToString(category);
                i++;
            }
        }
        return entityList;
    }

    /*
     * Returns all the users and groups that can edit the patient. First set in returned list contains all the users
     * that can edit the patient, second set contains all the groups that can edit the patient.
     */
    private List<Set<String>> getEntitiesWithAccess(XWikiDocument patientDoc, String accessString)
    {
        Collection<BaseObject> rightsObjects = patientDoc.getXObjects(RIGHTS_CLASS);
        Set<String> users = new HashSet<>();
        Set<String> groups = new HashSet<>();
        for (BaseObject rights : rightsObjects) {
            String rightsString = ((BaseStringProperty) rights.getField(RIGHTS_LEVELS_FIELD)).getValue();
            if (rightsString.contains(accessString)) {
                BaseStringProperty userAccessObject = (BaseStringProperty) rights.getField(RIGHTS_USERS_FIELD);
                BaseStringProperty groupAccessObject = (BaseStringProperty) rights.getField(RIGHTS_GROUPS_FIELD);
                if (userAccessObject != null) {
                    String[] usersAccess = userAccessObject.getValue().split(COMMA);
                    for (String user : Arrays.asList(usersAccess)) {
                        if (StringUtils.isNotEmpty(user)) {
                            users.add(user);
                        }
                    }
                }
                if (groupAccessObject != null) {
                    String[] groupsAccess = groupAccessObject.getValue().split(COMMA);
                    for (String group : Arrays.asList(groupsAccess)) {
                        if (StringUtils.isNotEmpty(group)) {
                            groups.add(group);
                        }
                    }
                }
            }
        }
        List<Set<String>> fullRights = new ArrayList<>();
        fullRights.add(users);
        fullRights.add(groups);
        return fullRights;
    }

    /**
     * For every family member, read users and groups that have either view or edit edit access on the patient,
     * then gives the sam elevel of access on the family for those users and groups.
     *
     * After performing this method, if p is a member of the family, and x has level y access on p, x has
     * level y access on the family.
     *
     * The user who is the owner of the family always has full access to the family.
     *
     * Note that the document is not saved to disk, changes are only made for the provided Family object and
     * its in-memory copy of the corresponding XWiki document.
     *
     * @param family to update permissions
     * @param context XWiki context to be used. The documnt will not be saved to disk, only changes in
     *        memory for the family document given will be made
     */
    public void updatePermissions(Family family, XWikiContext context)
    {
        XWiki wiki = context.getWiki();

        List<Patient> members = family.getMembers();

        this.updatePermissionsForOneRightLevel(VIEW_RIGHTS, members, family.getDocument(), wiki, context);
        // setting view-edit rights after view rights makes sure if a user has edit rights on one patient
        // and view rights on another the user still gets edit permissions for the family
        this.updatePermissionsForOneRightLevel(VIEWEDIT_RIGHTS, members, family.getDocument(), wiki, context);

        this.setOwnerPermissionsForUser(
                family.getDocument().getCreatorReference().toString(), family.getDocument(), context);
    }

    private void setOwnerPermissionsForUser(String user, XWikiDocument familyDocument, XWikiContext context)
    {
        // always give owner full rights
        BaseObject rightsObject = getOrCreateRightsObject(familyDocument, VIEWEDITDELETE_RIGHTS, context);
        rightsObject.set(RIGHTS_USERS_FIELD, user, context);
        rightsObject.set(RIGHTS_LEVELS_FIELD, VIEWEDITDELETE_RIGHTS, context);
        rightsObject.set(ALLOW, 1, context);
    }

    private void updatePermissionsForOneRightLevel(String rightsLevel,
            List<Patient> members, XWikiDocument familyDocument, XWiki wiki, XWikiContext context)
    {
        BaseObject rightsObject = getOrCreateRightsObject(familyDocument, rightsLevel, context);
        if (rightsObject == null) {
            return;
        }

        Set<String> usersUnion = new HashSet<>();
        Set<String> groupsUnion = new HashSet<>();

        for (Patient patient : members) {
            XWikiDocument patientDoc;
            try {
                patientDoc = wiki.getDocument(patient.getDocument(), context);
            } catch (XWikiException e) {
                this.logger.error("Can't retrieve patient document for patient {}: {}",
                    patient.getId(), e.getMessage());
                continue;
            }

            // TODO: what about users who have VIEW but not EDIT rights?
            List<Set<String>> patientRights = this.getEntitiesWithAccess(patientDoc, rightsLevel);

            usersUnion.addAll(patientRights.get(0));
            groupsUnion.addAll(patientRights.get(1));
        }

        rightsObject.set(RIGHTS_USERS_FIELD, setToString(usersUnion), context);
        rightsObject.set(RIGHTS_GROUPS_FIELD, setToString(groupsUnion), context);
        rightsObject.set(RIGHTS_LEVELS_FIELD, rightsLevel, context);
        rightsObject.set(ALLOW, 1, context);
    }

    private static String setToString(Set<String> set)
    {
        String finalString = "";
        for (String item : set) {
            if (StringUtils.isNotBlank(item)) {
                finalString += item + COMMA;
            }
        }
        return finalString;
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
