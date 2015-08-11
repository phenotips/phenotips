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
import org.phenotips.studies.family.internal.Pedigree;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiException;

import net.sf.json.JSON;

/**
 * @version $Id$
 */
public interface Family
{
    /** The class used for storing family data. */
    EntityReference CLASS_REFERENCE = new EntityReference("FamilyClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The space where family data is stored. */
    EntityReference DATA_SPACE = new EntityReference("Families", EntityType.SPACE);

    /**
     * class that holds pedigree data (image, structure, etc).
     */
    EntityReference PEDIGREE_CLASS =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /**
     * @return family id
     */
    String getId();

    /**
     * @return reference to family document
     */
    DocumentReference getDocumentReference();

    /**
     * @return list of family members
     */
    List<Patient> getMembers();

    /**
     * @return list of family members ids
     */
    List<String> getMembersIds();

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

    /**
     * Retrieves medical reports for all family members.
     *
     * @return patient ids mapped to medical reports, which in turn are maps of report name to its link
     */
    Map<String, Map<String, String>> getMedicalReports();

    /**
     * @return external id
     */
    String getExternalId();

    /**
     * @param action to get URL for
     * @return URL
     */
    String getURL(String action);

    /**
     * Some pedigrees may contain sensitive information, which should be displayed on every edit of the pedigree. The
     * function returns a warning to display, or empty string
     *
     * @return warning message
     */
    String getWarningMessage();

    /**
     * Returns the pedigree associated with the family.
     *
     * @return Pedigree associated with the family.
     */
    Pedigree getPedigree();

    /**
     * Sets the pedigree for the family, and saves.
     *
     * @param pedigree to set
     * @throws XWikiException if there was an error in saving.
     */
    void setPedigree(Pedigree pedigree) throws XWikiException;

    /**
     * For every family member, read users and groups that has edit access on the patient, then gives edit access on the
     * family for any such user and group. After performing this method, if p is a member of the family, and x has edit
     * access on p, x has edit access of the family.
     */
    void updatePermissions();
}
