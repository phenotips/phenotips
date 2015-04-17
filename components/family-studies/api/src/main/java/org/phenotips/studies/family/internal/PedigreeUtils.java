package org.phenotips.studies.family.internal;

import org.phenotips.studies.family.Processing;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Contains mainly functions for manipulating json from pedigrees.
 * Example usage would be extracting patient data objects from the json.
 */
public class PedigreeUtils
{

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

}
