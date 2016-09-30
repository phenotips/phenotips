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

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.List;

import org.json.JSONObject;

/**
 * @version $Id$
 * @since 1.4
 */
public interface Pedigree
{
    /**
     * XClass that holds pedigree data (image, structure, etc).
     */
    EntityReference CLASS_REFERENCE = new EntityReference("PedigreeClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** Code for data. **/
    String DATA = "data";

    /** Code for image. **/
    String IMAGE = "image";

    /**
     * Getter for data which holds all of a pedigree's JSON.
     *
     * @return could be null
     */
    JSONObject getData();

    /**
     * Getter for `image` string (SVG).
     *
     * @param highlightCurrentPatientId id of the patient which should be highlighted as the current patient. May be
     *            null, in which case no patient would be highlighed as current.
     * @return can not be null
     */
    String getImage(String highlightCurrentPatientId);

    /**
     * Getter for `image` string (SVG).
     *
     * @param highlightCurrentPatientId id of the patient which should be highlighted as the current patient. May be
     *            null, in which case no patient would be highlighed as current.
     * @param width sets the returned SVG width to this value. 0 or negative number means "leave as is".
     * @param height sets the returned SVG height to this value. 0 or negative number means "leave as is".
     * @return can not be null
     */
    String getImage(String highlightCurrentPatientId, int width, int height);

    /**
     * Extracts and returns all PhenoTips patient ids.
     *
     * @return all PhenoTips ids from pedigree nodes that have internal ids
     */
    List<String> extractIds();

    /**
     * @return phenotipsID of the patient which is the proband of the family, or null if
     * pedigree has no proband or proband node is not linked to a patient record
     */
    String getProbandId();

    /**
     * @return last name of the proband patient IFF it is linked to a PhenoTips patient, or null if
     * pedigree has no proband or proband node is not linked to a patient record, or last name is not set
     */
    String getProbandPatientLastName();

    /**
     * Patients are representing in a list within the structure of a pedigree. Extracts JSON objects that belong to
     * patients.
     *
     * @return non-null and non-empty patient properties in JSON objects.
     */
    List<JSONObject> extractPatientJSONProperties();

    /**
     * Remove a link to a PhenoTips patient from the pedigree (the pedigree node stays).
     *
     * @param linkedPatientId id of the linked patient to be removed
     */
    void removeLink(String linkedPatientId);
}
