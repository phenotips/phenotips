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

import org.phenotips.studies.family.Pedigree;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles pedigrees in the new SimpleJSON format. Some of the methods (used for converting pedigree JSOn to patient
 * data) are not yet supported for this format.
 *
 * @todo add support and rename class to SimpleJSONPedigree or alike.
 * @version $Id$
 * @since 1.3M4
 */
public class NewFormatPedigree extends AbstractBasePedigree implements Pedigree
{
    /** The main key under which pedigree data is stored. */
    private static final String PEDIGREE_JSON_DATA_KEY = "data";

    /** The name of the property specifying the PhenoTips linked patient id. */
    private static final String PATIENT_LINK_JSON_KEY = "phenotipsId";

    private static final String PATIENT_LASTNAME_JSON_KEY = "lastName";

    /**
     * Create a new default pedigree from data (in "simpleJSON" format) and image (a text representing SVG).
     *
     * @param data pedigree data
     * @param image SVG 'image'
     */
    public NewFormatPedigree(JSONObject data, String image)
    {
        super(data, image);
    }

    /**
     * Checks that the provided JSON objects represents a pedigree in the supported format.
     *
     * @param data JSON object ot check
     * @return true if JSON represents a pedigree format supported by this class
     */
    public static boolean isSupportedPedigreeFormat(JSONObject data)
    {
        return (data.optJSONArray(PEDIGREE_JSON_DATA_KEY) != null);
    }

    @Override
    public List<String> extractIds()
    {
        // these are only used (for now) when pedigree is saved, and saved pedigrees
        // do not use new format yet. Gene42 may need this soon though.
        throw new UnsupportedOperationException();
    }

    @Override
    public List<JSONObject> extractPatientJSONProperties()
    {
        // these are only used (for now) when pedigree is saved, and saved pedigrees
        // do not use new format yet. Gene42 may need this soon though.
        throw new UnsupportedOperationException();
    }

    @Override
    protected Pair<String, String> getProbandInfo()
    {
        JSONArray nodeList = data.optJSONArray(PEDIGREE_JSON_DATA_KEY);
        if (nodeList != null) {
            for (Object nodeObj : nodeList) {
                JSONObject node = (JSONObject) nodeObj;
                if (node.optBoolean("proband", false)) {
                    String patientLinkId = node.optString(NewFormatPedigree.PATIENT_LINK_JSON_KEY);
                    if (!StringUtils.isBlank(patientLinkId)) {
                        String lastName = node.optString(NewFormatPedigree.PATIENT_LASTNAME_JSON_KEY);
                        return Pair.of(patientLinkId, lastName);
                    }
                    break;
                }
            }
        }
        return Pair.of(null, null);
    }

    @Override
    protected void removeLinkFromPedigreeJSON(String linkedPatientId)
    {
        JSONArray nodeList = data.optJSONArray(PEDIGREE_JSON_DATA_KEY);
        if (nodeList != null) {
            for (Object nodeObj : nodeList) {
                JSONObject node = (JSONObject) nodeObj;
                String nodeLinkId = node.optString(NewFormatPedigree.PATIENT_LINK_JSON_KEY);
                if (StringUtils.equals(linkedPatientId, nodeLinkId)) {
                    node.remove(NewFormatPedigree.PATIENT_LINK_JSON_KEY);
                    break;
                }
            }
        }
    }
}
