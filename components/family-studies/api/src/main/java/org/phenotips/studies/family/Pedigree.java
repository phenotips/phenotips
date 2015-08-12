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
package org.phenotips.studies.family;

import org.phenotips.data.Patient;

import java.util.List;

import net.sf.json.JSONObject;

/**
 * @version $Id$
 */
public interface Pedigree
{
    /** Code for data. **/
    String DATA = "data";

    /** Code for image. **/
    String IMAGE = "image";

    /**
     * Checks if pedigree contains data.
     *
     * @return true if pedigree is empty
     */
    boolean isEmpty();

    /**
     * Getter for data which holds all of a pedigree's JSON.
     *
     * @return could be null
     */
    JSONObject getData();

    /**
     * Setter for 'data' which holds all of a pedigree's JSON.
     *
     * @param data the JSON data
     */
    void setData(JSONObject data);

    /**
     * Getter for `image` string (SVG).
     *
     * @return can not be null
     */
    String getImage();

    /**
     * Setter for 'image' string (SVG).
     *
     * @param image SVG image
     */
    void setImage(String image);

    /**
     * Remove a member from the pedigree.
     *
     * @param patientId id of the patient to remove
     */
    void removeMember(String patientId);

    /**
     * Extracts and returns all PhenoTips patient ids.
     *
     * @return all PhenoTips ids from pedigree nodes that have internal ids
     */
    List<String> extractIds();

    /**
     * Patients are representing in a list within the structure of a pedigree. Extracts JSON objects that belong to
     * patients.
     *
     * @return non-null and non-empty patient properties in JSON objects.
     */
    List<JSONObject> extractPatientJSONProperties();

    /**
     * Highlight proband in pedigree's image.
     *
     * @param proband of the family
     */
    void highlightProband(Patient proband);

}
