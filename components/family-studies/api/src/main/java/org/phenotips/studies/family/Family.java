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
import org.phenotips.data.Patient;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.List;

import net.sf.json.JSON;

/**
 * @version $Id$
 */
public interface Family
{
    /** The XClass used for storing family data. */
    EntityReference CLASS_REFERENCE = new EntityReference("FamilyClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The space where family data is stored. */
    EntityReference DATA_SPACE = new EntityReference("Families", EntityType.SPACE);

    /**
     * @return family id
     */
    String getId();

    /**
     * @return reference to family document
     */
    DocumentReference getDocumentReference();

    /**
     * @return list of family members ids
     */
    List<String> getMembers();

    /**
     * @param patient check if the patient belongs to this family
     * @return true if a patient is a member of the family
     */
    boolean isMember(Patient patient);

    /**
     * @param patient to add to family
     * @return true if addition was successful
     */
    boolean addMember(Patient patient);

    /**
     * @param patient to remove from family
     * @return true if removal was successful
     */
    boolean removeMember(Patient patient);

    /**
     * Generates a JSON data structure that describes the family and its members.
     *
     * @return JSON with info about the family, each member and the current user's permissions.
     */
    JSON getInformationAsJSON();

}
