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
import org.phenotips.data.Patient;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.internal.PedigreeUtils;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;

/**
 * XWiki implementation of Family.
 *
 * @version $Id$
 */
public class XWikiFamily implements Family
{
    private static final String FAMILY_MEMBERS_FIELD = "members";

    private static final String RIGHTS_USERS_FIELD = "users";

    private static final String RIGHTS_GROUPS_FIELD = "groups";

    private static final String RIGHTS_LEVELS_FIELD = "levels";

    private static final String COMMA = ",";

    @Inject
    private Logger logger;

    private XWikiDocument familyDocument;

    /**
     * @param familyDocument not-null document associated with the family
     */
    public XWikiFamily(XWikiDocument familyDocument)
    {
        this.familyDocument = familyDocument;
    }

    @Override
    public String getId()
    {
        return this.familyDocument.getDocumentReference().getName();
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.familyDocument.getDocumentReference();
    }

    @Override
    public List<String> getMembers()
    {
        BaseObject familyObject = this.familyDocument.getXObject(CLASS_REFERENCE);
        if (familyObject == null) {
            return new LinkedList<String>();
        }

        DBStringListProperty xwikiRelativesList;
        try {
            xwikiRelativesList = (DBStringListProperty) familyObject.get(FAMILY_MEMBERS_FIELD);
        } catch (XWikiException e) {
            this.logger.error("error reading family members: {}", e);
            return null;
        }
        if (xwikiRelativesList == null) {
            return new LinkedList<String>();
        }
        return xwikiRelativesList.getList();
    }

    @Override
    public synchronized boolean addMember(Patient patient)
    {
        XWikiContext context = getXContext();
        XWiki wiki = context.getWiki();
        DocumentReference patientReference = patient.getDocument();
        XWikiDocument patientDocument;
        try {
            patientDocument = wiki.getDocument(patientReference, context);
        } catch (XWikiException e) {
            this.logger.error("Could not add patient [{}] to family. Error getting patient document: {}",
                patient.getId(), e.getMessage());
            return false;
        }

        // Add member to Xwiki family
        List<String> members = getMembers();
        members.add(patientReference.toString());
        BaseObject familyObject = this.familyDocument.getXObject(FamilyUtils.FAMILY_CLASS);
        familyObject.set(FAMILY_MEMBERS_FIELD, members, context);

        setXwikiFamilyPermissions(this.familyDocument, patientDocument);

        try {
            XWikiFamilyRepository.setFamilyReference(patientDocument, this.familyDocument, context);
        } catch (XWikiException e) {
            this.logger.error("Could not add patient [{}] to family. Error setting family reference: {}",
                patient.getId(), e.getMessage());
            return false;
        }

        PedigreeUtils.copyPedigree(patientDocument, this.familyDocument, context);

        try {
            wiki.saveDocument(this.familyDocument, context);
            wiki.saveDocument(patientDocument, context);
        } catch (XWikiException e) {
            this.logger.error("Could not save family/patient after adding: {}", e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    // TODO
    public synchronized boolean removeMember(Patient patient)
    {
        return false;
    }

    @Override
    public boolean isMember(Patient patient)
    {
        List<String> members = getMembers();
        if (members == null) {
            return false;
        }
        String patientId = patient.getDocument().toString();
        return members.contains(patientId);
    }

    // ///////////////////////////////////////
    private void setXwikiFamilyPermissions(XWikiDocument newFamilyDoc, XWikiDocument patientDoc)
    {
        // FIXME - The permissions for the family should be copied from the patient, and giving all permissions to the
        // creating user

        XWikiContext context = getXContext();
        BaseObject permissions = newFamilyDoc.getXObject(FamilyUtils.RIGHTS_CLASS);
        String[] fullRights = this.getEntitiesWithEditAccessAsString(patientDoc);
        permissions.set(RIGHTS_USERS_FIELD, fullRights[0], context);
        permissions.set(RIGHTS_GROUPS_FIELD, fullRights[1], context);
        permissions.set(RIGHTS_LEVELS_FIELD, "view,edit", context);
        permissions.set("allow", 1, context);
    }

    /** users, groups. */
    private String[] getEntitiesWithEditAccessAsString(XWikiDocument patientDoc)
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

    private List<Set<String>> getEntitiesWithEditAccess(XWikiDocument patientDoc)
    {
        Collection<BaseObject> rightsObjects = patientDoc.getXObjects(FamilyUtils.RIGHTS_CLASS);
        Set<String> users = new HashSet<>();
        Set<String> groups = new HashSet<>();
        for (BaseObject rights : rightsObjects) {
            String[] levels = ((StringProperty) rights.getField(RIGHTS_LEVELS_FIELD)).getValue().split(COMMA);
            if (Arrays.asList(levels).contains("edit")) {
                Object userAccessObject = rights.getField(RIGHTS_USERS_FIELD);
                Object groupAccessObject = rights.getField(RIGHTS_GROUPS_FIELD);
                if (userAccessObject != null) {
                    String[] usersAccess = ((LargeStringProperty) userAccessObject).getValue().split(COMMA);
                    users.addAll(Arrays.asList(usersAccess));
                }
                if (groupAccessObject != null) {
                    String[] groupsAccess = ((LargeStringProperty) groupAccessObject).getValue().split(COMMA);
                    groups.addAll(Arrays.asList(groupsAccess));
                }
            }
        }
        List<Set<String>> fullRights = new ArrayList<>();
        fullRights.add(users);
        fullRights.add(groups);
        return fullRights;
    }

    private XWikiContext getXContext()
    {
        Execution execution = null;
        try {
            execution = ComponentManagerRegistry.getContextComponentManager().getInstance(Execution.class);
        } catch (ComponentLookupException ex) {
            // Should not happen
            return null;
        }
        XWikiContext context = (XWikiContext) execution.getContext().getProperty("xwikicontext");
        return context;
    }
}
