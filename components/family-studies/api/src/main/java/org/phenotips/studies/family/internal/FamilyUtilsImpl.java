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

import org.phenotips.Constants;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyUtils;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;

/**
 * Provides utility methods for working with family documents and patients.
 *
 * @version $Id$
 */
@Component
@Singleton
public class FamilyUtilsImpl implements FamilyUtils
{
    private static final String PREFIX = "FAM";

    private static final EntityReference FAMILY_TEMPLATE =
        new EntityReference("FamilyTemplate", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final EntityReference RELATIVE_REFERENCE =
        new EntityReference("RelativeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final EntityReference OWNER_CLASS =
        new EntityReference("OwnerClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String ALLOW = "allow";

    private static final String OWNER_RIGHTS = "view,edit,delete";

    private static final String FAMILY_REFERENCE_FIELD = "reference";

    private static final String FAMILY_MEMBERS_FIELD = "members";

    private static final String RIGHTS_USERS_FIELD = "users";

    private static final String RIGHTS_GROUPS_FIELD = "groups";

    private static final String RIGHTS_LEVELS_FIELD = "levels";

    private static final String COMMA = ",";

    @Inject
    private Provider<XWikiContext> provider;

    /**
     * Runs queries for finding families.
     */
    @Inject
    private QueryManager qm;

    @Inject
    private UserManager userManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Override
    public XWikiDocument getDoc(EntityReference docRef) throws XWikiException
    {
        XWikiContext context = this.provider.get();
        XWiki wiki = context.getWiki();
        return wiki.getDocument(docRef, context);
    }

    @Override
    public EntityReference getFamilyReference(XWikiDocument patientDoc) throws XWikiException
    {
        if (patientDoc == null) {
            throw new IllegalArgumentException("Document reference for the patient was null");
        }
        BaseObject familyPointer = patientDoc.getXObject(FAMILY_REFERENCE);
        if (familyPointer != null) {
            String familyDocName = familyPointer.getStringValue(FAMILY_REFERENCE_FIELD);
            if (StringUtils.isNotBlank(familyDocName)) {
                return this.referenceResolver.resolve(familyDocName, Family.DATA_SPACE);
            }
        }
        return null;
    }

    @Override
    public XWikiDocument getFamilyDoc(XWikiDocument anchorDoc) throws XWikiException
    {
        BaseObject familyObject = anchorDoc.getXObject(FamilyUtils.FAMILY_CLASS);
        if (familyObject != null) {
            return anchorDoc;
        } else {
            EntityReference reference = getFamilyReference(anchorDoc);
            if (reference != null) {
                return getDoc(reference);
            }
        }
        return null;
    }

    @Override
    public Collection<String> getRelatives(XWikiDocument patientDoc) throws XWikiException
    {
        if (patientDoc != null) {
            List<BaseObject> relativeObjects = patientDoc.getXObjects(RELATIVE_REFERENCE);
            if (relativeObjects == null) {
                return Collections.emptySet();
            }
            Set<String> relativeIds = new HashSet<String>();
            for (BaseObject relative : relativeObjects) {
                String id = relative.getStringValue("relative_of");
                if (StringUtils.isNotBlank(id)) {
                    relativeIds.add(id);
                }
            }
            return relativeIds;
        }
        return Collections.emptySet();
    }

    @Override
    public synchronized XWikiDocument createFamilyDoc(XWikiDocument probandDoc, boolean save)
        throws IllegalArgumentException, QueryException, XWikiException
    {
        XWikiContext context = this.provider.get();
        XWikiDocument newFamilyDoc = createProbandlessFamilyDoc(false);
        BaseObject familyObject = newFamilyDoc.getXObject(FAMILY_CLASS);
        // adding the creating patient as a member
        List<String> members = new LinkedList<>();
        members.add(probandDoc.getDocumentReference().getName());
        familyObject.set(FAMILY_MEMBERS_FIELD, members, context);

        if (save) {
            XWiki wiki = context.getWiki();
            wiki.saveDocument(newFamilyDoc, context);
        }
        return newFamilyDoc;
    }

    @Override
    public synchronized XWikiDocument createProbandlessFamilyDoc(boolean save)
        throws IllegalArgumentException, QueryException, XWikiException
    {
        XWikiContext context = this.provider.get();
        XWiki wiki = context.getWiki();
        long nextId = getLastUsedId() + 1;
        String nextStringId = String.format("%s%07d", PREFIX, nextId);
        EntityReference nextRef = new EntityReference(nextStringId, EntityType.DOCUMENT, Family.DATA_SPACE);
        XWikiDocument newFamilyDoc = wiki.getDocument(nextRef, context);

        User currentUser = this.userManager.getCurrentUser();
        newFamilyDoc.setCreatorReference(currentUser.getProfileDocument());
        if (!newFamilyDoc.isNew()) {
            throw new IllegalArgumentException("The new family id was already taken.");
        } else {
            XWikiDocument template = getDoc(FAMILY_TEMPLATE);
            // copying all objects from template
            for (Map.Entry<DocumentReference, List<BaseObject>> templateObject : template.getXObjects().entrySet()) {
                newFamilyDoc.newXObject(templateObject.getKey(), context);
            }
            BaseObject familyObject = newFamilyDoc.getXObject(FAMILY_CLASS);
            familyObject.set("identifier", nextId, context);

            BaseObject ownerObject = newFamilyDoc.newXObject(OWNER_CLASS, context);
            ownerObject.set("owner", currentUser.getId(), context);

            BaseObject permissions = newFamilyDoc.getXObject(RIGHTS_CLASS);
            permissions.set(RIGHTS_USERS_FIELD, currentUser.getId(), context);
            permissions.set(RIGHTS_LEVELS_FIELD, OWNER_RIGHTS, context);
            permissions.set(ALLOW, 1, context);

            if (save) {
                wiki.saveDocument(newFamilyDoc, context);
            }
        }
        return newFamilyDoc;
    }

    @Override
    public List<Set<String>> getEntitiesWithEditAccess(XWikiDocument patientDoc)
    {
        Collection<BaseObject> rightsObjects = patientDoc.getXObjects(RIGHTS_CLASS);
        Set<String> users = new HashSet<>();
        Set<String> groups = new HashSet<>();
        for (BaseObject rights : rightsObjects) {
            String[] levels = ((StringProperty) rights.getField(RIGHTS_LEVELS_FIELD)).getValue().split(COMMA);
            if (Arrays.asList(levels).contains("edit")) {
                Object userAccessObject = rights.getField(RIGHTS_USERS_FIELD);
                Object groupAccessObject = rights.getField(RIGHTS_GROUPS_FIELD);
                if (userAccessObject != null) {
                    String[] usersAccess = ((LargeStringProperty) userAccessObject).getValue().split(COMMA);
                    users.addAll(removeEmptyFromArray(usersAccess));
                }
                if (groupAccessObject != null) {
                    String[] groupsAccess = ((LargeStringProperty) groupAccessObject).getValue().split(COMMA);
                    groups.addAll(removeEmptyFromArray(groupsAccess));
                }
            }
        }
        List<Set<String>> fullRights = new ArrayList<>();
        fullRights.add(users);
        fullRights.add(groups);
        return fullRights;
    }

    private List<String> removeEmptyFromArray(String[] array)
    {
        List<String> cleanList = new LinkedList<>();
        for (String element : array) {
            if (StringUtils.isNotBlank(element)) {
                cleanList.add(element);
            }
        }
        return cleanList;
    }

    @Override
    public void setFamilyReference(XWikiDocument patientDoc, XWikiDocument familyDoc, XWikiContext context)
        throws XWikiException
    {
        BaseObject pointer = patientDoc.getXObject(FAMILY_REFERENCE);
        if (pointer == null) {
            pointer = patientDoc.newXObject(FAMILY_REFERENCE, context);
        }
        pointer.set(FAMILY_REFERENCE_FIELD, familyDoc.getDocumentReference().getName(), context);
    }

    private long getLastUsedId() throws QueryException
    {
        long crtMaxID = 0;
        Query q =
            this.qm.createQuery(
                "select family.identifier from Document doc, doc.object(PhenoTips.FamilyClass) as family"
                    + " where family.identifier is not null order by family.identifier desc", Query.XWQL)
                    .setLimit(1);
        List<Long> crtMaxIDList = q.execute();
        if (crtMaxIDList.size() > 0 && crtMaxIDList.get(0) != null) {
            crtMaxID = crtMaxIDList.get(0);
        }
        crtMaxID = Math.max(crtMaxID, 0);
        return crtMaxID;
    }

    @Override
    public List<String> getFamilyMembers(XWikiDocument familyDoc) throws XWikiException
    {
        return this.getFamilyMembers(familyDoc.getXObject(FAMILY_CLASS));
    }

    @Override
    public List<String> getFamilyMembers(BaseObject familyObject) throws XWikiException
    {
        DBStringListProperty xwikiRelativesList = (DBStringListProperty) familyObject.get(FAMILY_MEMBERS_FIELD);
        return xwikiRelativesList == null ? new LinkedList<String>() : xwikiRelativesList.getList();
    }

    @Override
    public void setFamilyMembers(XWikiDocument familyDoc, List<String> members) throws XWikiException
    {
        BaseObject familyObject = familyDoc.getXObject(FAMILY_CLASS);
        XWikiContext context = this.provider.get();
        familyObject.set(FAMILY_MEMBERS_FIELD, members, context);
        context.getWiki().saveDocument(familyDoc, context);
    }

    @Override
    public String getWarningMessage(XWikiDocument familyDoc) throws XWikiException
    {
        BaseObject familyObject = familyDoc.getXObject(FAMILY_CLASS);
        if (familyObject.getIntValue("warning") == 0) {
            return "";
        } else {
            return familyObject.getStringValue("warning_message");
        }
    }

    @Override
    public String getViewUrl(DocumentReference ref)
    {
        XWikiContext context = this.provider.get();
        return context.getWiki().getURL(ref, "view", context);
    }
}
