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
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
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

    /** The set of rights awarded to any user that holds edit rights on any patient record that belongs to a family. */
    private static final String DEFAULT_RIGHTS = "view,edit";

    private static final String RIGHTS_USERS_FIELD = "users";

    private static final String RIGHTS_LEVELS_FIELD = "levels";

    private static final String RIGHTS_GROUPS_FIELD = "groups";

    private static final String OWNER_RIGHTS = "view,edit,delete";

    private static final String COMMA = ",";

    private static final String ALLOW = "allow";

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private UserManager userManager;

    @Inject
    private Logger logger;

    /**
     * Grants edit permission on the family to everyone who had edit permission on the patient.
     *
     * @param familyDoc family to give permissions to
     * @param patientDoc patient to read permissions from
     */
    public void setFamilyPermissionsFromPatient(XWikiDocument familyDoc, XWikiDocument patientDoc)
    {
        // FIXME - The permissions for the family should be copied from the patient, and giving all permissions to the
        // creating user

        XWikiContext context = this.provider.get();

        BaseObject permissions = familyDoc.getXObject(PhenotipsFamilyPermissions.RIGHTS_CLASS);
        String[] fullRights = this.getEntitiesWithEditAccessAsString(patientDoc);
        permissions.set(RIGHTS_USERS_FIELD, fullRights[0], context);
        permissions.set(RIGHTS_GROUPS_FIELD, fullRights[1], context);
        permissions.set(RIGHTS_LEVELS_FIELD, DEFAULT_RIGHTS, context);
        permissions.set(ALLOW, 1, context);
    }

    /**
     * Grants owner permissions to current user.
     *
     * @param familyDoc family to give permissions on
     */
    public void setFamilyPermissionsToCurrentUser(XWikiDocument familyDoc)
    {
        User currentUser = this.userManager.getCurrentUser();
        XWikiContext context = this.provider.get();

        BaseObject permissions = familyDoc.getXObject(PhenotipsFamilyPermissions.RIGHTS_CLASS);
        permissions.set(RIGHTS_USERS_FIELD, currentUser.getId(), context);
        permissions.set(RIGHTS_LEVELS_FIELD, OWNER_RIGHTS, context);
        permissions.set(ALLOW, 1, context);
    }

    /**
     * Gets a patient document owner permissions.
     *
     * @param patientDoc patient to read permissions from
     * @return array of rights
     */
    public String[] getEntitiesWithEditAccessAsString(XWikiDocument patientDoc)
    {
        String[] fullRights = new String[2];
        int i = 0;
        for (Set<String> category : this.getEntitiesWithEditAccess(patientDoc)) {
            String categoryString = "";
            for (String user : category) {
                categoryString += user + COMMA;
            }
            fullRights[i] = categoryString;
            i++;
        }
        return fullRights;
    }

    /*
     * Returns all the users and groups that can edit the patient. First set in returned list contains all the users
     * that can edit the patient, second set contains all the groups that can edit the patient.
     */
    private List<Set<String>> getEntitiesWithEditAccess(XWikiDocument patientDoc)
    {
        Collection<BaseObject> rightsObjects = patientDoc.getXObjects(PhenotipsFamilyPermissions.RIGHTS_CLASS);
        Set<String> users = new HashSet<>();
        Set<String> groups = new HashSet<>();
        for (BaseObject rights : rightsObjects) {
            String[] levels = ((BaseStringProperty) rights.getField(RIGHTS_LEVELS_FIELD)).getValue().split(COMMA);
            if (Arrays.asList(levels).contains("edit")) {
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
     * For every family member, read users and groups that has edit access on the patient, then gives edit access on the
     * family for any such user and group. After performing this method, if p is a member of the family, and x has edit
     * access on p, x has edit access of the family.
     *
     * The user who is the owner of the family always has access to the family.
     *
     * @param family to update permissions
     * @param familyDocument document of family to update permissions
     */
    public void updatePermissions(Family family, XWikiDocument familyDocument)
    {
        XWikiContext context = this.provider.get();
        XWiki wiki = context.getWiki();

        BaseObject rightsObject = getDefaultRightsObject(familyDocument);
        if (rightsObject == null) {
            this.logger.error(
                "Could not find a permission object attached to the family document {}",
                family.getId());
            return;
        }

        List<Patient> members = family.getMembers();

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
            List<Set<String>> patientRights = this.getEntitiesWithEditAccess(patientDoc);

            usersUnion.addAll(patientRights.get(0));
            groupsUnion.addAll(patientRights.get(1));
        }

        // add users and.or group who is the current owner of the family
        usersUnion.add(familyDocument.getCreatorReference().toString());

        rightsObject.set(RIGHTS_USERS_FIELD, setToString(usersUnion), context);
        rightsObject.set(RIGHTS_GROUPS_FIELD, setToString(groupsUnion), context);
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
    private BaseObject getDefaultRightsObject(XWikiDocument familyDoc)
    {
        List<BaseObject> rights = familyDoc.getXObjects(PhenotipsFamilyPermissions.RIGHTS_CLASS);
        for (BaseObject right : rights) {
            String level = right.getStringValue(RIGHTS_LEVELS_FIELD);
            if (StringUtils.equalsIgnoreCase(level, PhenotipsFamilyPermissions.DEFAULT_RIGHTS)) {
                return right;
            }
        }
        return null;
    }

}
