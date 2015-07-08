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
package org.phenotips.studies.family.internal2;

import org.phenotips.components.ComponentManagerRegistry;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;

/**
 * Sets permissions of a family.
 *
 * @version $Id$
 */
@Component
@Singleton
public final class XWikiFamilyPermissions
{
    /** XWiki class that contains rights to XWiki documents. */
    private static final EntityReference RIGHTS_CLASS =
        new EntityReference("XWikiRights", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    private static final String RIGHTS_USERS_FIELD = "users";

    private static final String RIGHTS_LEVELS_FIELD = "levels";

    private static final String RIGHTS_GROUPS_FIELD = "groups";

    private static final String OWNER_RIGHTS = "view,edit,delete";

    private static final String COMMA = ",";

    private static final String ALLOW = "allow";

    @Inject
    private static Provider<XWikiContext> provider;

    @Inject
    private static UserManager userManager;

    static {
        try {
            XWikiFamilyPermissions.userManager =
                ComponentManagerRegistry.getContextComponentManager().getInstance(UserManager.class);
            XWikiFamilyPermissions.provider =
                ComponentManagerRegistry.getContextComponentManager().getInstance(XWikiContext.TYPE_PROVIDER);
        } catch (ComponentLookupException e) {
            e.printStackTrace();
        }
    }

    private XWikiFamilyPermissions()
    {
    }

    /**
     * Grants edit permission on the family to everyone who had edit permission on the patient.
     *
     * @param familyDoc family to give permissions to
     * @param patientDoc patient to read permissions from
     */
    public static void setFamilyPermissionsFromPatient(XWikiDocument familyDoc, XWikiDocument patientDoc)
    {
        // FIXME - The permissions for the family should be copied from the patient, and giving all permissions to the
        // creating user

        XWikiContext context = XWikiFamilyPermissions.provider.get();

        BaseObject permissions = familyDoc.getXObject(XWikiFamilyPermissions.RIGHTS_CLASS);
        String[] fullRights = XWikiFamilyPermissions.getEntitiesWithEditAccessAsString(patientDoc);
        permissions.set(RIGHTS_USERS_FIELD, fullRights[0], context);
        permissions.set(RIGHTS_GROUPS_FIELD, fullRights[1], context);
        permissions.set(RIGHTS_LEVELS_FIELD, "view,edit", context);
        permissions.set(ALLOW, 1, context);
    }

    /**
     * Grants owner permissions to current user.
     *
     * @param familyDoc family to give permissions on
     */
    public static void setFamilyPermissionsToCurrentUser(XWikiDocument familyDoc)
    {
        User currentUser = XWikiFamilyPermissions.userManager.getCurrentUser();
        XWikiContext context = XWikiFamilyPermissions.provider.get();

        BaseObject permissions = familyDoc.getXObject(XWikiFamilyPermissions.RIGHTS_CLASS);
        permissions.set(RIGHTS_USERS_FIELD, currentUser.getId(), context);
        permissions.set(RIGHTS_LEVELS_FIELD, OWNER_RIGHTS, context);
        permissions.set(ALLOW, 1, context);
    }

    private static String[] getEntitiesWithEditAccessAsString(XWikiDocument patientDoc)
    {
        String[] fullRights = new String[2];
        int i = 0;
        for (Set<String> category : XWikiFamilyPermissions.getEntitiesWithEditAccess(patientDoc)) {
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
    private static List<Set<String>> getEntitiesWithEditAccess(XWikiDocument patientDoc)
    {
        Collection<BaseObject> rightsObjects = patientDoc.getXObjects(XWikiFamilyPermissions.RIGHTS_CLASS);
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
}
