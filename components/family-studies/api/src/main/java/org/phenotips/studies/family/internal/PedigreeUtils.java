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
 * Contains mainly functions for manipulating json from pedigrees.
 * Example usage would be extracting patient data objects from the json.
 */
public class PedigreeUtils
{
    public static final EntityReference PEDIGREE_CLASS =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /**
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

    /** @return non-null and non-empty patient properties JSON objects. */
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
     * Does not do permission checks.
     *
     * @param image could be null. If it is, no changes will be made to the image.
     */
    public static void storePedigree(XWikiDocument document, JSON pedigree, String image, XWikiContext context)
        throws XWikiException
    {
        BaseObject pedigreeObject = document.getXObject(PedigreeUtils.PEDIGREE_CLASS);
        if (image != null) {
            image = SvgUpdater.setPatientStylesInSvg(image, document.getDocumentReference().getName());
            pedigreeObject.set("image", image, context);
        }
        pedigreeObject.set("data", pedigree.toString(), context);
    }

    /**
     * Does not do permission checks.
     *
     * @param image could be null. If it is, no changes will be made to the image.
     */
    public static void storePedigreeWithSave(XWikiDocument document, JSON pedigree, String image, XWikiContext context,
        XWiki wiki) throws XWikiException
    {
        PedigreeUtils.storePedigree(document, pedigree, image, context);
        wiki.saveDocument(document, context);
    }


    /** @return null on error, an empty {@link net.sf.json.JSON} if there is no pedigree, or the existing pedigree. */
    public static Pedigree getPedigree(XWikiDocument doc)
    {
        try {
            Pedigree pedigree = new Pedigree();
            BaseObject pedigreeObj = doc.getXObject(PEDIGREE_CLASS);
            if (pedigreeObj != null) {
                LargeStringProperty data = (LargeStringProperty) pedigreeObj.get("data");
                LargeStringProperty image = (LargeStringProperty) pedigreeObj.get("image");
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

    public static class Pedigree
    {
        // these are package local on purpose
        JSONObject data;
        String image = "";

        /**
         * Checks if the `data` field is empty.
         * @return true if data is {@link null} or if {@link JSONObject#isEmpty()} returns true
         */
        boolean isEmpty() {
            return this.data == null || this.data.isEmpty();
        }
    }
}
