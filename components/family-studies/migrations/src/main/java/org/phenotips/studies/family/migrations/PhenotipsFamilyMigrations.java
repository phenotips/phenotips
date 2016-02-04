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
package org.phenotips.studies.family.migrations;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.internal.PhenotipsFamilyPermissions;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Provides utility methods for migrating patient documents.
 *
 * @version $Id$
 */

public class PhenotipsFamilyMigrations
{
    private static final String FAMILY_PREFIX = "FAM";

    private static final String COLLABORATOR_PROPERTY_NAME = "collaborator";

    private static final String VISIBILITY_PROPERTY_NAME = "visibility";

    private static final String ACCESS_PROPERTY_NAME = "access";

    /** Family reference class reference. */
    public EntityReference familyReferenceClassReference = new EntityReference("FamilyReferenceClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private EntityReference rightsClassReference = new EntityReference("XWikiRights", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    private EntityReference familyParentReference = new EntityReference("WebHome", EntityType.DOCUMENT,
        Family.DATA_SPACE);

    private Session session;

    private XWikiContext context;

    @Inject
    private PhenotipsFamilyPermissions familyPermissions;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /**
     * Creates a new family document for given patient with pedigree data.
     *
     * @param patientXDoc patient document
     * @param pedigreeData pedigree data JSON
     * @param pedigreeImage pedigree SVG image text
     * @param xcontext xwiki context
     * @param hsession hibernate session
     * @return new family xwiki document
     * @throws XWikiException error when creating a family document, or QueryException or other Exception
     * @throws QueryException error when querying for a the largest family identifier id
     * @throws Exception error when other errors occur
     */
    public XWikiDocument createFamilyDocument(XWikiDocument patientXDoc, JSONObject pedigreeData, String pedigreeImage,
        XWikiContext xcontext, Session hsession) throws QueryException, XWikiException, Exception
    {
        context = xcontext;
        session = hsession;
        long nextId = getLastUsedId() + 1;
        String nextStringId = String.format("%s%07d", FAMILY_PREFIX, nextId);

        EntityReference newFamilyRef = new EntityReference(nextStringId, EntityType.DOCUMENT, Family.DATA_SPACE);
        XWikiDocument newFamilyXDocument = context.getWiki().getDocument(newFamilyRef, context);
        if (!newFamilyXDocument.isNew()) {
            throw new IllegalArgumentException("The new family id was already taken.");
        }

        this.setOwner(newFamilyXDocument, patientXDoc);
        this.setFamilyObject(newFamilyXDocument, patientXDoc, nextId);
        this.setPedigreeObject(newFamilyXDocument, pedigreeData, pedigreeImage);
        this.setPermissionsObject(newFamilyXDocument, patientXDoc);
        this.setVisibilityObject(newFamilyXDocument, patientXDoc);
        this.setCollaborators(newFamilyXDocument, patientXDoc);

        newFamilyXDocument.setAuthorReference(patientXDoc.getAuthorReference());
        newFamilyXDocument.setCreatorReference(patientXDoc.getCreatorReference());
        newFamilyXDocument.setContentAuthorReference(patientXDoc.getContentAuthorReference());
        newFamilyXDocument.setParentReference(familyParentReference);

        return newFamilyXDocument;
    }

    /**
     * Sets the reference to the family document in the patient document.
     *
     * @param patientDoc patient document
     * @param documentReference family document reference
     * @param xcontext context
     * @throws XWikiException error when creating the new object
     */
    public void setFamilyReference(XWikiDocument patientDoc, String documentReference, XWikiContext xcontext)
        throws XWikiException
    {
        context = xcontext;
        BaseObject pointer = patientDoc.getXObject(familyReferenceClassReference);
        if (pointer == null) {
            pointer = patientDoc.newXObject(familyReferenceClassReference, context);
        }
        pointer.setStringValue("reference", documentReference);
    }

    /**
     * Adds to the pedigree JSON: "phenotipsId": patient ID in XWiki; "probandNodeID": 0; "JSON_version": "1.0".
     *
     * @param data pedigree data
     * @param patientId patient Id
     * @return processed pedigree JSON object
     */
    public JSONObject processPedigree(JSONObject data, String patientId)
    {
        // Adding patient id under the patient prop
        JSONArray gg = (JSONArray) data.get("GG");
        for (Object nodeObj : gg) {
            JSONObject node = (JSONObject) nodeObj;

            int id = (int) node.get("id");
            if (id != 0) {
                continue;
            }

            JSONObject properties = (JSONObject) node.get("prop");
            properties.accumulate("phenotipsId", patientId);
            break;
        }

        data.accumulate("probandNodeID", 0);
        data.accumulate("JSON_version", "1.0");
        return data;
    }

    /**
     * Set owner object property.
     */
    private void setOwner(XWikiDocument familyXDocument, XWikiDocument patientXDoc) throws XWikiException
    {
        BaseObject owner = familyXDocument.newXObject(Owner.CLASS_REFERENCE, context);
        owner.setStringValue(Owner.PROPERTY_NAME, this.getOwner(patientXDoc));
    }

    /**
     * Set family object properties.
     */
    private void setFamilyObject(XWikiDocument familyXDocument, XWikiDocument patientXDoc, long id)
        throws XWikiException
    {
        BaseObject familyObject = familyXDocument.newXObject(Family.CLASS_REFERENCE, context);
        familyObject.setLongValue("identifier", id);
        familyObject.setStringListValue("members", Arrays.asList(patientXDoc.getDocumentReference().getName()));
        familyObject.setStringValue("external_id",
            this.getPropertyValue(patientXDoc, Patient.CLASS_REFERENCE, "last_name", ""));
        familyObject.setIntValue("warning", 0);
        familyObject.setStringValue("warning_message", "");
    }

    /**
     * Set pedigree object properties.
     */
    private void setPedigreeObject(XWikiDocument familyXDocument, JSONObject data, String pedigreeImage)
        throws XWikiException
    {
        BaseObject pedigreeObject = familyXDocument.newXObject(Pedigree.CLASS_REFERENCE, context);
        pedigreeObject.set(Pedigree.IMAGE, pedigreeImage, context);
        pedigreeObject.set(Pedigree.DATA, data.toString(), context);
    }

    /**
     * Set permissions object properties.
     */
    private void setPermissionsObject(XWikiDocument familyXDocument, XWikiDocument patientXDoc)
        throws XWikiException
    {
        BaseObject permissionsObject = familyXDocument.newXObject(rightsClassReference, context);
        String[] fullRights = familyPermissions.getEntitiesWithEditAccessAsString(patientXDoc);
        permissionsObject.setStringValue("groups", fullRights[1]);
        permissionsObject.setStringValue("levels", "view,edit");
        permissionsObject.setStringValue("users", fullRights[0]);
        permissionsObject.setIntValue("allow", 1);
    }

    /**
     * Set visibility object.
     */
    private void setVisibilityObject(XWikiDocument familyXDocument, XWikiDocument patientXDoc)
        throws XWikiException
    {
        BaseObject visibilityObject = familyXDocument.newXObject(Visibility.CLASS_REFERENCE, context);
        visibilityObject.setStringValue(VISIBILITY_PROPERTY_NAME,
            this.getPropertyValue(patientXDoc, Visibility.CLASS_REFERENCE, VISIBILITY_PROPERTY_NAME, ""));
    }

    /**
     * Set collaborators objects.
     */
    private void setCollaborators(XWikiDocument familyXDocument, XWikiDocument patientXDoc) throws XWikiException
    {
        List<BaseObject> collaborators = patientXDoc.getXObjects(Collaborator.CLASS_REFERENCE);
        if (collaborators == null || collaborators.isEmpty()) {
            return;
        }
        for (BaseObject o : collaborators) {
            if (o == null) {
                continue;
            }
            String collaboratorName = o.getStringValue(COLLABORATOR_PROPERTY_NAME);
            String accessName = o.getStringValue(ACCESS_PROPERTY_NAME);
            if (StringUtils.isBlank(collaboratorName) || StringUtils.isBlank(accessName)) {
                continue;
            }

            BaseObject collaboratorObject =
                familyXDocument.newXObject(Collaborator.CLASS_REFERENCE, context);
            collaboratorObject.setStringValue(COLLABORATOR_PROPERTY_NAME, collaboratorName);
            collaboratorObject.setStringValue(ACCESS_PROPERTY_NAME, accessName);
        }
    }

    private String getPropertyValue(XWikiDocument doc, EntityReference classReference, String propertyName,
        String defaultValue)
    {
        String value = "";
        BaseObject obj = doc.getXObject(classReference);
        if (obj != null) {
            value = obj.getStringValue(propertyName);
        }
        return StringUtils.defaultString(value, defaultValue);
    }

    private String getOwner(XWikiDocument patientDoc)
    {
        String owner = getPropertyValue(patientDoc, Owner.CLASS_REFERENCE, Owner.PROPERTY_NAME, "");
        if (StringUtils.isNotBlank(owner)) {
            return owner;
        }
        if (StringUtils.isNotBlank(patientDoc.getCreatorReference().getName())) {
            return this.serializer.serialize(patientDoc.getCreatorReference());
        }
        return owner;
    }

    /**
     * Returns the largest family identifier id.
     */
    private long getLastUsedId() throws QueryException
    {
        long crtMaxID = 0;
        Query q = this.session.createQuery(
            "select prop.id.value from BaseObject as obj, LongProperty as prop "
                + "where obj.className='PhenoTips.FamilyClass' and obj.id=prop.id.id "
                + "and prop.id.name='identifier' and prop.id.value is not null order by prop.id.value desc");
        @SuppressWarnings("unchecked")
        List<Long> crtMaxIDList = q.list();
        if (crtMaxIDList.size() > 0 && crtMaxIDList.get(0) != null) {
            crtMaxID = crtMaxIDList.get(0);
        }
        crtMaxID = Math.max(crtMaxID, 0);
        return crtMaxID;
    }
}
