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
import org.phenotips.studies.family.internal2.Pedigree;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;

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

    private PedigreeUtils()
    {
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
                LargeStringProperty data = (LargeStringProperty) fromPedigreeObj.get(Pedigree.DATA);
                LargeStringProperty image = (LargeStringProperty) fromPedigreeObj.get(Pedigree.IMAGE);
                if (StringUtils.isNotBlank(data.toText())) {
                    BaseObject toPedigreeObj = to.getXObject(PedigreeUtils.PEDIGREE_CLASS);
                    toPedigreeObj.set(Pedigree.DATA, data.toText(), context);
                    toPedigreeObj.set(Pedigree.IMAGE, image.toText(), context);
                }
            }
        } catch (XWikiException ex) {
            // do nothing
        }
    }

}
