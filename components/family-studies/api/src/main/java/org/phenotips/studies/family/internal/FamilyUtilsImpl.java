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
package org.phenotips.studies.family.internal;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.studies.family.FamilyUtils;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

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

    private static final EntityReference RELATIVEREFERENCE =
        new EntityReference("RelativeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String FAMILY_REFERENCE_FIELD = "reference";

    private static final String FAMILY_MEMBERS_FIELD = "members";

    private static final String RIGHTS_USERS_FIELD = "users";

    private static final String RIGHTS_GROUPS_FIELD = "groups";

    private static final String RIGHTS_LEVELS_FIELD = "levels";

    private static final String COMMA = ",";

    @Inject
    private Provider<XWikiContext> provider;

    /** Runs queries for finding families. */
    @Inject
    private QueryManager qm;

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
    public XWikiDocument getFromDataSpace(String id) throws XWikiException
    {
        return getDoc(this.referenceResolver.resolve(id, Patient.DEFAULT_DATA_SPACE));
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
                return this.referenceResolver.resolve(familyDocName, Patient.DEFAULT_DATA_SPACE);
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
    public XWikiDocument getFamilyOfPatient(String patientId) throws XWikiException
    {
        DocumentReference patientRef = this.referenceResolver.resolve(patientId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument patientDoc = this.getDoc(patientRef);
        return this.getFamilyDoc(patientDoc);
    }

    @Override
    public Collection<String> getRelatives(XWikiDocument patientDoc) throws XWikiException
    {
        if (patientDoc != null) {
            List<BaseObject> relativeObjects = patientDoc.getXObjects(RELATIVEREFERENCE);
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
    public XWikiDocument createFamilyDoc(String patientId) throws IllegalArgumentException, QueryException,
        XWikiException
    {
        DocumentReference docRef = this.referenceResolver.resolve(patientId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument doc = getDoc(docRef);
        return createFamilyDoc(doc);
    }

    @Override
    public synchronized XWikiDocument createFamilyDoc(XWikiDocument patientDoc)
        throws QueryException, XWikiException
    {
        XWikiContext context = this.provider.get();
        XWiki wiki = context.getWiki();
        XWikiDocument newFamilyDoc = this.createFamilyDoc(patientDoc, false);

        BaseObject permissions = newFamilyDoc.getXObject(RIGHTS_CLASS);
        String[] fullRights = this.getEntitiesWithEditAccessAsString(patientDoc);
        permissions.set(RIGHTS_USERS_FIELD, fullRights[0], context);
        permissions.set(RIGHTS_GROUPS_FIELD, fullRights[1], context);
        permissions.set(RIGHTS_LEVELS_FIELD, "view,edit", context);
        permissions.set("allow", 1, context);

        this.setFamilyReference(patientDoc, newFamilyDoc, context);

        PedigreeUtils.copyPedigree(patientDoc, newFamilyDoc, context);

        wiki.saveDocument(newFamilyDoc, context);
        wiki.saveDocument(patientDoc, context);
        return newFamilyDoc;
    }

    @Override
    public synchronized XWikiDocument createFamilyDoc(XWikiDocument probandDoc, boolean save)
        throws IllegalArgumentException, QueryException, XWikiException
    {
        XWikiContext context = this.provider.get();
        XWiki wiki = context.getWiki();
        long nextId = getLastUsedId() + 1;
        String nextStringId = String.format("%s%07d", PREFIX, nextId);
        EntityReference nextRef = new EntityReference(nextStringId, EntityType.DOCUMENT, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument newFamilyDoc = wiki.getDocument(nextRef, context);
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

            // adding the creating patient as a member
            List<String> members = new LinkedList<>();
            members.add(probandDoc.getDocumentReference().getName());
            familyObject.set("members", members, context);

            if (save) {
                wiki.saveDocument(newFamilyDoc, context);
            }
        }
        return newFamilyDoc;
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
}
