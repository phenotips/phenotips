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
import org.phenotips.studies.family.Processing;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.LinkedList;
import java.util.List;

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
     * Does not do permission checks. Modifies pedigree's image style. Stores the modified image, and data (as is)
     * into the `document`.
     *
     * @param document destination for storing the pedigree
     * @param pedigree data section of a pedigree
     * @param image    could be null. If it is, no changes will be made to the image.
     * @param context  needed for XWiki calls
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
     * @param image    {@link #storePedigree(XWikiDocument, JSON, String, XWikiContext)}
     * @param context  {@link #storePedigree(XWikiDocument, JSON, String, XWikiContext)}
     * @param wiki     Used for saving the `document`
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
     * @param doc in which to look for a pedigree
     * @return null on error; an empty {@link org.phenotips.studies.family.internal.PedigreeUtils.Pedigree} if there
     * is no pedigree, or the existing pedigree.
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
                    pedigree.data = JSONObject.fromObject(data.toText());
                    pedigree.image = image.toText();
                    return pedigree;
                }
            }
            return pedigree;
        } catch (XWikiException ex) {
            return null;
        }
    }

    /**
     * Overwrites a pedigree in one document given an existing pedigree in another document.
     * Will not throw an exception if fails. Does not save any documents.
     *
     * @param from    in which to look for an existing pedigree
     * @param to      into which document to copy the pedigree found in the `from` document
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

    /** Simplifies passing around pedigree objects which consist of an SVG image and JSON data. */
    public static class Pedigree
    {
        private JSONObject data;

        private String image = "";

        /**
         * Checks if the `data` field is empty.
         *
         * @return true if data is {@link null} or if {@link JSONObject#isEmpty()} returns true
         */
        public boolean isEmpty()
        {
            return this.data == null || this.data.isEmpty();
        }

        /**
         * Getter for `data` which holds all of a pedigree's JSON.
         *
         * @return could be null
         */
        public JSONObject getData()
        {
            return this.data;
        }

        /**
         * Getter for `image` string (SVG).
         *
         * @return can not be null
         */
        public String getImage()
        {
            return this.image;
        }
    }
}
