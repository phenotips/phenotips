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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @version $Id$
 */
public class DefaultPedigree extends AbstractBasePedigree implements Pedigree
{
    private static final String PEDIGREE_JSON_MEMBERS_KEY = "members";

    private static final String PEDIGREE_JSON_PROBAND_KEY = "proband";

    private static final String PATIENT_NODEID_JSON_KEY = "id";

    /** The key which contains patient data in PhenoTips JSON format. */
    private static final String PATIENT_DATA_JSON_KEY = "properties";

    /** The name under which the linked patient id resides under in patient JSON properties in pedigree. */
    private static final String PATIENT_DATA_PHENOTIPSID_KEY = "id";

    private static final String PATIENT_DATA_NAMES_KEY = "patient_name";

    private static final String PATIENT_DATA_NAMES_LASTNAME_KEY = "last_name";

    /**
     * Create a new default pedigree from data (in "old internal" format) and image (a text representing SVG).
     *
     * @param data pedigree data
     * @param image SVG 'image'
     */
    public DefaultPedigree(JSONObject data, String image)
    {
        super(data, image);
    }

    /**
     * Checks that the provided JSON objects represents a pedigree in the supported format.
     *
     * It is hard to check that everything is in order, but at the very least every pedigree has to have
     * at least one member, which means at the very least there should be a "members" key in the JSON.
     *
     * @param data JSON object ot check
     * @return true if JSON represents a pedigree format supported by this class
     */
    public static boolean isSupportedPedigreeFormat(JSONObject data)
    {
        return data.has(PEDIGREE_JSON_MEMBERS_KEY);
    }

    @Override
    public List<String> extractIds()
    {
        List<String> extractedIds = new LinkedList<>();
        for (JSONObject properties : this.extractPatientJSONProperties()) {
            Object id = properties.get(DefaultPedigree.PATIENT_DATA_PHENOTIPSID_KEY);
            extractedIds.add(id.toString());
        }
        return extractedIds;
    }

    @Override
    public List<JSONObject> extractPatientJSONProperties()
    {
        List<JSONObject> extractedObjects = new LinkedList<>();
        JSONArray members = (JSONArray) this.data.opt(PEDIGREE_JSON_MEMBERS_KEY);
        // letting it throw a null exception on purpose
        for (Object nodeObj : members) {
            JSONObject node = (JSONObject) nodeObj;

            JSONObject properties = (JSONObject) node.opt(PATIENT_DATA_JSON_KEY);
            if (properties == null || properties.length() == 0) {
                continue;
            }

            Object id = properties.opt(DefaultPedigree.PATIENT_DATA_PHENOTIPSID_KEY);
            if (id == null || StringUtils.isBlank(id.toString())) {
                continue;
            }

            extractedObjects.add(properties);
        }
        return extractedObjects;
    }

    @Override
    protected Pair<String, String> getProbandInfo()
    {
        int probandNodeId = this.data.optInt(PEDIGREE_JSON_PROBAND_KEY, -1);
        if (probandNodeId == -1) {
            // no proband ID, no proband last name
            return Pair.of(null, null);
        }
        JSONArray members = (JSONArray) this.data.opt(PEDIGREE_JSON_MEMBERS_KEY);
        for (Object nodeObj : members) {
            JSONObject node = (JSONObject) nodeObj;
            if (probandNodeId == node.optInt(PATIENT_NODEID_JSON_KEY, -1)) {
                JSONObject properties = (JSONObject) node.opt(PATIENT_DATA_JSON_KEY);
                if (properties != null) {
                    String id = properties.optString(DefaultPedigree.PATIENT_DATA_PHENOTIPSID_KEY);
                    if (!StringUtils.isBlank(id)) {
                        JSONObject names = properties.optJSONObject(DefaultPedigree.PATIENT_DATA_NAMES_KEY);
                        String lastName = (names == null)
                                          ? null : names.optString(DefaultPedigree.PATIENT_DATA_NAMES_LASTNAME_KEY);
                        return Pair.of(id, lastName);
                    }
                }
                break;
            }
        }
        return Pair.of(null, null);
    }

    @Override
    protected void removeLinkFromPedigreeJSON(String linkedPatientId)
    {
        List<JSONObject> patientProperties = this.extractPatientJSONProperties();
        for (JSONObject properties : patientProperties) {
            Object patientLink = properties.opt(DefaultPedigree.PATIENT_DATA_PHENOTIPSID_KEY);
            if (patientLink != null && StringUtils.equalsIgnoreCase(patientLink.toString(), linkedPatientId)) {
                properties.remove(DefaultPedigree.PATIENT_DATA_PHENOTIPSID_KEY);
            }
        }
    }
}
