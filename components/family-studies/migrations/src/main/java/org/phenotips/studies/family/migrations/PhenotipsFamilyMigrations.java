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

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.internal.PhenotipsFamilyPermissions;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides utility methods for migrating patient documents.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component(roles = { PhenotipsFamilyMigrations.class })
@Singleton
public class PhenotipsFamilyMigrations
{
    private static final String FAMILY_PREFIX = "FAM";

    private static final String COLLABORATOR_PROPERTY_NAME = "collaborator";

    private static final String VISIBILITY_PROPERTY_NAME = "visibility";

    private static final String ACCESS_PROPERTY_NAME = "access";

    private static final String VIEW_RIGHT = "view";

    private static final String EDIT_RIGHT = "view,edit";

    private static final String FULL_RIGHT = "view,edit,delete";

    private static final String JSON_TOP_LEVEL = "GG";

    private static final String JSON_NODE_PROPERTIES = "prop";

    private static final String JSON_NODE_PEDIGREE_ID = "id";

    private static final String JSON_EXTERNALID = "externalID";

    private static final String JSON_PHENOTIPSID = "phenotipsId";

    private static final String JSON_COMMENTS = "comments";

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

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /**
     * Creates a new family document for given patient with pedigree data.
     *
     * @param patientXDoc patient document
     * @param pedigreeData pedigree data JSON
     * @param pedigreeImage pedigree SVG image text
     * @param xcontext xwiki context
     * @param hsession hibernate session
     * @return new family xwiki document or null in case of any errors
     */
    public XWikiDocument createFamilyDocument(XWikiDocument patientXDoc, JSONObject pedigreeData, String pedigreeImage,
        XWikiContext xcontext, Session hsession)
    {
        try {
            this.context = xcontext;
            this.session = hsession;
            long nextId = getLastUsedId() + 1;
            String nextStringId = String.format("%s%07d", FAMILY_PREFIX, nextId);

            EntityReference newFamilyRef = new EntityReference(nextStringId, EntityType.DOCUMENT, Family.DATA_SPACE);
            XWikiDocument newFamilyXDocument = this.context.getWiki().getDocument(newFamilyRef, this.context);
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
            newFamilyXDocument.setParentReference(this.familyParentReference);
            newFamilyXDocument.setSyntax(Syntax.XWIKI_2_1);

            return newFamilyXDocument;
        } catch (Exception ex) {
            this.logger.error("Error creating new family document: {} [{}]", ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Sets the reference to the family document in the patient document.
     *
     * @param patientDoc patient document
     * @param documentReference family document reference
     * @param xcontext context
     * @return true if operation was successful, false otherwise (e.g. when family reference creation failed)
     */
    public boolean setFamilyReference(XWikiDocument patientDoc, String documentReference, XWikiContext xcontext)
    {
        try {
            this.context = xcontext;
            BaseObject pointer = patientDoc.getXObject(Family.REFERENCE_CLASS_REFERENCE, true, this.context);
            pointer.setStringValue("reference", documentReference);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Adds to the pedigree JSON: "phenotipsId": patient ID in XWiki; "probandNodeID": 0; "JSON_version": "1.0".
     *
     * @param pedigree pedigree JSON, supposedly in 1.2.x format
     * @param patientId patient Id
     * @return processed pedigree JSON object
     */
    public JSONObject processPedigree(JSONObject pedigree, String patientId)
    {
        JSONArray gg = pedigree.getJSONArray(JSON_TOP_LEVEL);
        for (int pedigreeId = 0; pedigreeId < gg.length(); pedigreeId++) {
            JSONObject node = gg.optJSONObject(pedigreeId);
            if (node == null) {
                continue;
            }

            int id = node.optInt(JSON_NODE_PEDIGREE_ID, -1);
            if (id != 0) {
                continue;
            }

            JSONObject properties = node.optJSONObject(JSON_NODE_PROPERTIES);
            if (properties == null) {
                properties = new JSONObject();
            }
            properties.put(JSON_PHENOTIPSID, patientId);
            node.put(JSON_NODE_PROPERTIES, properties);
            gg.put(pedigreeId, node);
            pedigree.put(JSON_TOP_LEVEL, gg);
            break;
        }

        pedigree.put("probandNodeID", 0);
        pedigree.put("JSON_version", "1.0");
        return pedigree;
    }

    /**
     * Asigns nodes to PhenoTips records and adds comments to nodes.
     *
     *  - Finds all nodes with the given external IDs and - if not already linked to a PT record - links them to
     *    the given PT record
     *  - For each node linked to a PT record (including nodes linked in step1 above) appends the comment
     *    provided
     *
     * @param pedigree pedigree as JSON. The pedigree will be modified in place
     * @param externalIdToPhenotipsId a map of externalIDs which, if found in pedigree, should be linked
     *        to the corresponding phenotips records
     * @param commentsByPhenotipsId a map of phenotips ids to comments that shouldbe added for those nodes.
     *        The list may include nodes linked based on the `externalIdToPhenotipsId` list
     * @return a set of phenotips IDs that were newly linked based on externalID
     */
    public Set<String> updatePedigree(JSONObject pedigree, Map<String, String> externalIdToPhenotipsId,
            Map<String, String> commentsByPhenotipsId)
    {
        Set<String> linkedIds = new HashSet<>();

        JSONArray gg = pedigree.getJSONArray(JSON_TOP_LEVEL);
        for (int pedigreeId = 0; pedigreeId < gg.length(); pedigreeId++) {
            JSONObject node = gg.optJSONObject(pedigreeId);
            if (node == null) {
                continue;
            }

            JSONObject properties = node.optJSONObject(JSON_NODE_PROPERTIES);
            if (properties == null) {
                continue;
            }

            String externalId = properties.optString(JSON_EXTERNALID);
            String linkPatientId = properties.optString(JSON_PHENOTIPSID, null);

            if (externalId == null && linkPatientId == null) {
                continue;
            }

            if (linkPatientId == null && externalIdToPhenotipsId.containsKey(externalId)) {
                linkPatientId = externalIdToPhenotipsId.get(externalId);
                properties.put(JSON_PHENOTIPSID, linkPatientId);
                linkedIds.add(linkPatientId);
            }

            if (commentsByPhenotipsId.containsKey(linkPatientId)) {
                // update comments
                String comment = properties.optString(JSON_COMMENTS, null);
                String commentAddition = commentsByPhenotipsId.get(linkPatientId);
                if (comment == null) {
                    comment = commentAddition;
                } else {
                    comment += "\n" + commentAddition;
                }
                properties.put(JSON_COMMENTS, comment);
            }

            node.put(JSON_NODE_PROPERTIES, properties);
            gg.put(pedigreeId, node);
            pedigree.put(JSON_TOP_LEVEL, gg);
        }
        return linkedIds;
    }

    /**
     * Returns the owner of the patient.
     *
     * @param patientDoc patient XDocument
     * @return owner as string
     */
    public String getOwner(XWikiDocument patientDoc)
    {
        String owner = getPropertyValue(patientDoc, Owner.CLASS_REFERENCE, Owner.PROPERTY_NAME, "");
        if (StringUtils.isNotBlank(owner)) {
            return owner;
        }
        if (patientDoc.getCreatorReference() != null) {
            return this.serializer.serialize(patientDoc.getCreatorReference());
        }
        return owner;
    }


    /**
     * Set owner object property.
     */
    private void setOwner(XWikiDocument familyXDocument, XWikiDocument patientXDoc) throws XWikiException
    {
        BaseObject owner = familyXDocument.newXObject(Owner.CLASS_REFERENCE, this.context);
        owner.setStringValue(Owner.PROPERTY_NAME, this.getOwner(patientXDoc));
    }

    /**
     * Set family object properties.
     */
    private void setFamilyObject(XWikiDocument familyXDocument, XWikiDocument patientXDoc, long id)
        throws XWikiException
    {
        BaseObject familyObject = familyXDocument.newXObject(Family.CLASS_REFERENCE, this.context);
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
        BaseObject pedigreeObject = familyXDocument.newXObject(Pedigree.CLASS_REFERENCE, this.context);
        pedigreeObject.set(Pedigree.IMAGE, pedigreeImage, this.context);
        pedigreeObject.set(Pedigree.DATA, data.toString(), this.context);
    }

    /**
     * Set permissions object properties.
     */
    private void setPermissionsObject(XWikiDocument familyXDocument, XWikiDocument patientXDoc)
        throws XWikiException
    {
        setRights(familyXDocument, patientXDoc, VIEW_RIGHT);
        setRights(familyXDocument, patientXDoc, EDIT_RIGHT);
        setRights(familyXDocument, patientXDoc, FULL_RIGHT);
    }

    /**
     * Helper method - set permissions object properties for one access level.
     */
    private void setRights(XWikiDocument familyXDocument, XWikiDocument patientXDoc, String rightLevel)
        throws XWikiException
    {
        BaseObject permissionsObject = familyXDocument.newXObject(this.rightsClassReference, this.context);
        String[] rightHolders = this.familyPermissions.getEntitiesWithAccessAsString(patientXDoc, VIEW_RIGHT);
        permissionsObject.setStringValue("users", rightHolders[0]);
        permissionsObject.setStringValue("groups", rightHolders[1]);
        permissionsObject.setStringValue("levels", rightLevel);
        permissionsObject.setIntValue("allow", 1);
    }

    /**
     * Set visibility object.
     */
    private void setVisibilityObject(XWikiDocument familyXDocument, XWikiDocument patientXDoc)
        throws XWikiException
    {
        BaseObject visibilityObject = familyXDocument.newXObject(Visibility.CLASS_REFERENCE, this.context);
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
                familyXDocument.newXObject(Collaborator.CLASS_REFERENCE, this.context);
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
