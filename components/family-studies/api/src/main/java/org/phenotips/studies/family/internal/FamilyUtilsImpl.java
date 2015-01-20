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
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.LargeStringProperty;

import groovy.lang.Singleton;
import net.sf.json.JSONObject;

@Component
@Singleton
public class FamilyUtilsImpl implements FamilyUtils
{
    private final String PREFIX = "FAM";

    private final EntityReference FAMILY_TEMPLATE =
        new EntityReference("FamilyTemplate", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private final EntityReference RELATIVEREFERENCE =
        new EntityReference("RelativeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    @Inject
    private Provider<XWikiContext> provider;

    /** Runs queries for finding families. */
    @Inject
    private QueryManager qm;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    /** can return null */
    public XWikiDocument getDoc(EntityReference docRef) throws XWikiException
    {
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        return wiki.getDocument(docRef, context);
    }

    public XWikiDocument getFromDataSpace(String id) throws XWikiException
    {
        return getDoc(referenceResolver.resolve(id, Patient.DEFAULT_DATA_SPACE));
    }

    /**
     * @return String could be null in case there is no pointer found
     */
    public EntityReference getFamilyReference(XWikiDocument patientDoc) throws XWikiException
    {
        if (patientDoc == null) {
            throw new IllegalArgumentException("Document reference for the patient was null");
        }
        BaseObject familyPointer = patientDoc.getXObject(FAMILY_REFERENCE);
        if (familyPointer != null) {
            String familyDocName = familyPointer.getStringValue("reference");
            if (StringUtils.isNotBlank(familyDocName)) {
                return referenceResolver.resolve(familyDocName, Patient.DEFAULT_DATA_SPACE);
            }
        }
        return null;
    }

    /**
     * can return null. Checks if the document is a family document, and returns it. If not tries to find the family
     * document attached to the passed in document.
     */
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

    /**
     * Does not check for nulls while retrieving the family document. Will throw an exception if any of the 'links in
     * the chain' are not present.
     */
    public XWikiDocument getFamilyOfPatient(String patientId) throws XWikiException
    {
        DocumentReference patientRef = referenceResolver.resolve(patientId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument patientDoc = this.getDoc(patientRef);
        return this.getFamilyDoc(patientDoc);
    }

    /** @return null on error, an empty {@link net.sf.json.JSON} if there is no pedigree, or the existing pedigree. */
    public JSONObject getPedigree(XWikiDocument doc)
    {
        try {
            BaseObject pedigreeObj = doc.getXObject(PEDIGREE_CLASS);
            if (pedigreeObj != null) {
                LargeStringProperty data = (LargeStringProperty) pedigreeObj.get("data");
                if (StringUtils.isNotBlank(data.toText())) {
                    return JSONObject.fromObject(data.toText());
                }
            }
            return new JSONObject(true);
        } catch (XWikiException ex) {
            return null;
        }
    }

    /**
     * Relatives are patients that are stored in the RelativeClass (old interface).
     *
     * @return collection of patient ids that the patient has links to on their report
     */
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

    public XWikiDocument createFamilyDoc(String patientId) throws NamingException, QueryException, XWikiException
    {
        DocumentReference docRef = referenceResolver.resolve(patientId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument doc = getDoc(docRef);
        return createFamilyDoc(doc);
    }

    /**
     * Creates a new family document and set that new document as the patients family, overwriting the existing family.
     */
    public synchronized XWikiDocument createFamilyDoc(XWikiDocument patientDoc)
        throws NamingException, QueryException, XWikiException
    {
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        long nextId = getLastUsedId() + 1;
        String nextStringId = String.format("%s%07d", PREFIX, nextId);
        EntityReference nextRef = new EntityReference(nextStringId, EntityType.DOCUMENT, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument newFamilyDoc = wiki.getDocument(nextRef, context);
        if (!newFamilyDoc.isNew()) {
            throw new NamingException("The new family id was already taken.");
        } else {
            XWikiDocument template = getDoc(FAMILY_TEMPLATE);
            // copying from template
            for (Map.Entry<DocumentReference, List<BaseObject>> templateObject : template.getXObjects().entrySet()) {
                newFamilyDoc.newXObject(templateObject.getKey(), context);
            }
            BaseObject familyObject = newFamilyDoc.getXObject(FAMILY_CLASS);
            familyObject.set("identifier", nextId, context);

            // adding the creating patient as a member
            List<String> members = new LinkedList<>();
            members.add(patientDoc.getDocumentReference().getName());
            familyObject.set("members", members, context);
            this.setFamilyReference(patientDoc, newFamilyDoc, context);

            wiki.saveDocument(newFamilyDoc, context);
            wiki.saveDocument(patientDoc, context);
        }
        return newFamilyDoc;
    }

    public void setFamilyReference(XWikiDocument patientDoc, XWikiDocument familyDoc, XWikiContext context)
        throws XWikiException
    {
        BaseObject pointer = patientDoc.getXObject(FAMILY_REFERENCE);
        if (pointer == null) {
            pointer = patientDoc.newXObject(FAMILY_REFERENCE, context);
        }
        pointer.set("reference", familyDoc.getDocumentReference().getName(), context);
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

    public List<String> getFamilyMembers(XWikiDocument familyDoc) throws XWikiException
    {
        return this.getFamilyMembers(familyDoc.getXObject(FAMILY_CLASS));
    }

    public List<String> getFamilyMembers(BaseObject familyObject) throws XWikiException
    {
        DBStringListProperty xwikiRelativesList = (DBStringListProperty) familyObject.get("members");
        return xwikiRelativesList.getList();
    }

    public void setFamilyMembers(XWikiDocument familyDoc, List<String> members) throws XWikiException
    {
        BaseObject familyObject = familyDoc.getXObject(FAMILY_CLASS);
        DBStringListProperty xwikiRelativesList = (DBStringListProperty) familyObject.get("members");
        xwikiRelativesList.setList(members);
        XWikiContext context = provider.get();
        context.getWiki().saveDocument(familyDoc, context);
    }
}
