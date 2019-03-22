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
package org.phenotips.data.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Holds solved data for patient.
 *
 * @version $Id$
 * @since 1.4
 */
public class SolvedData
{
    /** The solved status internal property. **/
    public static final String STATUS_PROPERTY_NAME = "solved";

    /** The solved status JSON key. **/
    public static final String STATUS_JSON_KEY = "status";

    /** The unsolved status JSON key. **/
    public static final String STATUS_UNSOLVED = "unsolved";

    /** The Pubmed ID internal property. **/
    public static final String PUBMED_ID_PROPERTY_NAME = "solved__pubmed_id";

    /** The Pubmed ID JSON key. **/
    public static final String PUBMED_ID_JSON_KEY = "pubmed_id";

    /** The solved notes JSON key. **/
    public static final String NOTES_JSON_KEY = "notes";

    /** The solved notes internal property. **/
    public static final String NOTES_PROPERTY_NAME = "solved__notes";

    private static final String STATUS_SOLVED_NUMERIC = "1";

    private static final String STATUS_UNSOLVED_NUMERIC = "0";

    private static final String STATUS_UNKNOWN = "";

    /** The solved status internal values. **/
    public static final List<String> STATUS_VALUES = Arrays.asList(STATUS_SOLVED_NUMERIC, STATUS_UNSOLVED_NUMERIC);

    /**
     * The solved notes.
     *
     * @see #getNotes()
     */
    private String notes;

    /**
     * The solved status, one of possible values: "0" (case is unsolved), "1" (case is solved).
     *
     * @see #getStatus()
     */
    private String status;

    /**
     * The Pubmed ID values.
     *
     * @see #getPubmedIds()
     */
    private List<String> pubmedIds;

    /**
     * Constructor that receives all the needed data as parameters.
     *
     * @param status solved status: "1" if case is solved, "0" if not
     * @param notes solved notes
     * @param pubmedIds The Pubmed ID values
     */
    public SolvedData(String status, String notes, List<String> pubmedIds)
    {
        this.setNotes(notes);
        this.setStatus(status);
        this.setPubmedIds(pubmedIds);
    }

    /**
     * Constructor for initializing from a JSON Object.
     *
     * @param jsonBlock JSON object that holds solved details info
     */
    public SolvedData(JSONObject jsonBlock)
    {
        if (jsonBlock.has(PUBMED_ID_JSON_KEY)) {
            Object object = jsonBlock.get(PUBMED_ID_JSON_KEY);

            if (object instanceof JSONArray) {
                JSONArray values = (JSONArray) object;
                List<String> ids = new ArrayList<String>();
                for (Object id : values) {
                    if (StringUtils.isNotBlank(id.toString())) {
                        ids.add(id.toString());
                    }
                }

                if (!ids.isEmpty()) {
                    this.setPubmedIds(ids);
                }
            } else {
                // v1.3.x json compatibility
                String pubmedId = jsonBlock.optString(PUBMED_ID_JSON_KEY);
                if (StringUtils.isNotBlank(pubmedId)) {
                    this.setPubmedIds(Arrays.asList(pubmedId));
                }
            }
        }
        if (jsonBlock.has(STATUS_JSON_KEY)) {
            String value = jsonBlock.optString(STATUS_JSON_KEY);
            this.setStatus(invertSolvedStatus(value));
        }
        if (jsonBlock.has(NOTES_JSON_KEY)) {
            this.setNotes(jsonBlock.optString(NOTES_JSON_KEY));
        }
    }

    /**
     * Get Pubmed ID values.
     *
     * @return Pubmed ID values
     */
    public List<String> getPubmedIds()
    {
        return this.pubmedIds;
    }

    /**
     * Get solved notes.
     *
     * @return solved notes
     */
    public String getNotes()
    {
        return this.notes;
    }

    /**
     * Get solved status.
     *
     * @return solved status
     */
    public String getStatus()
    {
        return this.status;
    }

    /**
     * Set gene Pubmed IDs.
     *
     * @param pubmedIds Pubmed IDs
     */
    public void setPubmedIds(List<String> pubmedIds)
    {
        if (pubmedIds != null && !pubmedIds.isEmpty()) {
            this.pubmedIds = pubmedIds;
        } else {
            this.pubmedIds = Collections.emptyList();
        }
    }

    /**
     * Set solved notes.
     *
     * @param notes solved notes
     */
    public void setNotes(String notes)
    {
        if (StringUtils.isNotBlank(notes)) {
            this.notes = notes;
        }
    }

    /**
     * Set solved status.
     *
     * @param status solved status
     */
    public void setStatus(String status)
    {
        if (StringUtils.isNotBlank(status) && STATUS_VALUES.contains(status.trim().toLowerCase())) {
            this.status = status;
        }
    }

    /**
     * Indicated whether case is solved, in other words has solved status set to "1".
     *
     * @return solved status
     */
    public boolean isSolved()
    {
        return STATUS_SOLVED_NUMERIC.equals(this.status);
    }

    /**
     * Indicated whether class holds no meaningfull data.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty()
    {
        return StringUtils.isBlank(this.status) && StringUtils.isBlank(this.notes)
            && (this.pubmedIds == null || this.pubmedIds.isEmpty());
    }

    /**
     * Export data to JSON format.
     *
     * @return solved data JSON object
     */
    public JSONObject toJSON()
    {
        Collection<String> properties =
            Arrays.asList(STATUS_PROPERTY_NAME, NOTES_PROPERTY_NAME, PUBMED_ID_PROPERTY_NAME);
        return this.toJSON(properties);
    }

    /**
     * Export data specified by properties to JSON format.
     *
     * @param selectedFieldNames selected fields to export to JSON
     * @return solved data JSON object
     */
    public JSONObject toJSON(Collection<String> selectedFieldNames)
    {
        JSONObject geneJson = new JSONObject();
        if (selectedFieldNames.contains(STATUS_PROPERTY_NAME) && StringUtils.isNotBlank(this.status)) {
            geneJson.put(STATUS_JSON_KEY, this.parseSolvedStatus(this.getStatus()));
        }
        if (selectedFieldNames.contains(NOTES_PROPERTY_NAME) && StringUtils.isNotBlank(this.notes)) {
            geneJson.put(NOTES_JSON_KEY, this.getNotes());
        }
        if (selectedFieldNames.contains(PUBMED_ID_PROPERTY_NAME)) {
            geneJson.put(PUBMED_ID_JSON_KEY, this.getPubmedIdsJson());
        }
        if (geneJson.length() == 0) {
            return null;
        }
        return geneJson;
    }

    /** Given a status converts it back into `1` or `0`, or if status is unknown into an {@code null}. */
    private String invertSolvedStatus(String status)
    {
        if (STATUS_PROPERTY_NAME.equals(status)) {
            return STATUS_SOLVED_NUMERIC;
        } else if (STATUS_UNSOLVED.equals(status)) {
            return STATUS_UNSOLVED_NUMERIC;
        } else {
            return STATUS_UNKNOWN;
        }
    }

    private String parseSolvedStatus(String status)
    {
        if ("1".equals(status)) {
            return STATUS_PROPERTY_NAME;
        } else if ("0".equals(status)) {
            return STATUS_UNSOLVED;
        } else {
            return STATUS_UNKNOWN;
        }
    }

    private JSONArray getPubmedIdsJson()
    {
        if (this.pubmedIds.isEmpty()) {
            return null;
        } else {
            JSONArray result = new JSONArray();
            for (String id : this.pubmedIds) {
                result.put(id);
            }
            return result;
        }
    }
}
