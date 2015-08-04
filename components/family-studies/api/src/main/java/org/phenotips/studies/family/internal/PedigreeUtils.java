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
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Patient;
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Processing;
import org.phenotips.studies.family.internal2.Pedigree;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Contains mainly functions for manipulating json from pedigrees. Example usage would be extracting patient data
 * objects from the json.
 *
 * @version $Id$
 * @since 1.2RC1
 */
public final class PedigreeUtils
{
    /**
     * XWiki class that holds pedigree data (image, structure, etc).
     */
    public static final EntityReference PEDIGREE_CLASS =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String DATA = "data";

    private static final String IMAGE = "image";

    @Inject
    private static FamilyUtils familyUtils;

    static {
        try {
            PedigreeUtils.familyUtils =
                ComponentManagerRegistry.getContextComponentManager().getInstance(FamilyUtils.class);
        } catch (ComponentLookupException e) {
            e.printStackTrace();
        }
    }

    private PedigreeUtils()
    {
    }

    /**
     * Given a pedigree, will extract and return all PhenoTips patient ids.
     *
     * @param pedigree data section of a pedigree
     * @return all PhenoTips ids from pedigree nodes that have internal ids
     */
    public static List<String> extractIdsFromPedigree(JSONObject pedigree)
    {
        List<String> extractedIds = new LinkedList<>();
        for (JSONObject properties : PedigreeUtils.extractPatientJSONPropertiesFromPedigree(pedigree)) {
            Object id = properties.get(Processing.PATIENT_LINK_JSON_KEY);
            if (id != null && StringUtils.isNotBlank(id.toString())) {
                extractedIds.add(id.toString());
            }
        }
        return extractedIds;
    }

    /**
     * Patients are representing in a list within the structure of a pedigree. Extracts JSON objects that belong to
     * patients.
     *
     * @param pedigree data section of a pedigree
     * @return non-null and non-empty patient properties in JSON objects.
     */
    public static List<JSONObject> extractPatientJSONPropertiesFromPedigree(JSONObject pedigree)
    {
        List<JSONObject> extractedObjects = new LinkedList<>();
        JSONArray gg = (JSONArray) pedigree.get("GG");
        // letting it throw a null exception on purpose
        for (Object nodeObj : gg) {
            JSONObject node = (JSONObject) nodeObj;
            JSONObject properties = (JSONObject) node.get("prop");
            if (properties == null || properties.isEmpty()) {
                continue;
            }
            extractedObjects.add(properties);
        }
        return extractedObjects;
    }

    /**
     * Does not do permission checks. Modifies pedigree's image style. Stores the modified image, and data (as is) into
     * the `document`.
     *
     * @param document destination for storing the pedigree
     * @param pedigree data section of a pedigree
     * @param image could be null. If it is, no changes will be made to the image.
     * @param context needed for XWiki calls
     * @throws XWikiException one of many possible XWiki exceptions
     */
    public static void storePedigree(XWikiDocument document, JSON pedigree, String image, XWikiContext context)
        throws XWikiException
    {
        BaseObject pedigreeObject = document.getXObject(PedigreeUtils.PEDIGREE_CLASS);
        if (image != null) {
            String updatedImage = SvgUpdater.setPatientStylesInSvg(image, document.getDocumentReference().getName());
            pedigreeObject.set(IMAGE, updatedImage, context);
        }
        pedigreeObject.set(DATA, pedigree.toString(), context);
    }

    /**
     * Wrapper around {@link #storePedigree(XWikiDocument, JSON, String, XWikiContext)} which saves the XWiki document.
     *
     * @param document {@link #storePedigree(XWikiDocument, JSON, String, XWikiContext)}
     * @param pedigree {@link #storePedigree(XWikiDocument, JSON, String, XWikiContext)}
     * @param image {@link #storePedigree(XWikiDocument, JSON, String, XWikiContext)}
     * @param context {@link #storePedigree(XWikiDocument, JSON, String, XWikiContext)}
     * @param wiki Used for saving the `document`
     * @throws XWikiException one of many possible XWikiExceptions
     */
    public static void storePedigreeWithSave(XWikiDocument document, JSON pedigree, String image, XWikiContext context,
        XWiki wiki) throws XWikiException
    {
        PedigreeUtils.storePedigree(document, pedigree, image, context);
        wiki.saveDocument(document, context);
    }

    /**
     * Retrieves a pedigree (both image and data).
     *
     * @param document in which to look for a pedigree
     * @return null on error; an empty {@link org.phenotips.studies.family.internal.PedigreeUtils.Pedigree} if there is
     *         no pedigree, or the existing pedigree.
     */
    public static Pedigree getPedigree(DocumentReference document)
    {
        XWikiDocument doc;
        try {
            doc = familyUtils.getDoc(document);
        } catch (XWikiException e) {
            return null;
        }
        return getPedigree(doc);
    }

    /**
     * Retrieves a pedigree (both image and data).
     *
     * @param doc in which to look for a pedigree
     * @return null on error; an empty {@link org.phenotips.studies.family.internal.PedigreeUtils.Pedigree} if there is
     *         no pedigree, or the existing pedigree.
     */
    public static Pedigree getPedigree(XWikiDocument doc)
    {
        try {
            Pedigree pedigree = new Pedigree();
            BaseObject pedigreeObj = doc.getXObject(PEDIGREE_CLASS);
            if (pedigreeObj != null) {
                LargeStringProperty data = (LargeStringProperty) pedigreeObj.get(DATA);
                LargeStringProperty image = (LargeStringProperty) pedigreeObj.get(IMAGE);
                if (StringUtils.isNotBlank(data.toText())) {
                    pedigree.setData(JSONObject.fromObject(data.toText()));
                    pedigree.setImage(image.toText());
                    return pedigree;
                }
            }
            return pedigree;
        } catch (XWikiException ex) {
            return null;
        }
    }

    /**
     * Checks if a patient has pedigree.
     *
     * @param patient to check
     * @return true if user has a non empty pedigree
     */
    public static boolean hasPedigree(Patient patient)
    {
        Pedigree pedigree = PedigreeUtils.getPedigree(patient.getDocument());
        if (pedigree == null || pedigree.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Overwrites a pedigree in one document given an existing pedigree in another document. Will not throw an exception
     * if fails. Does not save any documents.
     *
     * @param from in which to look for an existing pedigree
     * @param to into which document to copy the pedigree found in the `from` document
     * @param context needed for overwriting pedigree fields in the `to` document
     */
    public static void copyPedigree(XWikiDocument from, XWikiDocument to, XWikiContext context)
    {
        try {
            BaseObject fromPedigreeObj = from.getXObject(PedigreeUtils.PEDIGREE_CLASS);
            if (fromPedigreeObj != null) {
                LargeStringProperty data = (LargeStringProperty) fromPedigreeObj.get(DATA);
                LargeStringProperty image = (LargeStringProperty) fromPedigreeObj.get(IMAGE);
                if (StringUtils.isNotBlank(data.toText())) {
                    BaseObject toPedigreeObj = to.getXObject(PedigreeUtils.PEDIGREE_CLASS);
                    toPedigreeObj.set(DATA, data.toText(), context);
                    toPedigreeObj.set(IMAGE, image.toText(), context);
                }
            }
        } catch (XWikiException ex) {
            // do nothing
        }
    }

    /**
     * A patient can either have their own pedigree or a family pedigree (if they belong to one).
     *
     * @param anchorId a valid family or patient id
     * @param utils an instance of {@link FamilyUtils} for working with XWiki
     * @return data portion of a pedigree, which could be the family pedigree or the patient's own pedigree
     * @throws XWikiException can occur while getting patient document or family document
     */
    public static JSON getPedigree(String anchorId, FamilyUtils utils) throws XWikiException
    {
        XWikiDocument docWithPedigree = utils.getFamily(anchorId);
        if (docWithPedigree == null) {
            // either anchor id is invalid or it is a patient id
            docWithPedigree = utils.getFromDataSpace(anchorId);
        }
        Pedigree pedigree = PedigreeUtils.getPedigree(docWithPedigree);
        if (pedigree != null && !pedigree.isEmpty()) {
            return pedigree.getData();
        } else {
            return new JSONObject(true);
        }
    }
}
