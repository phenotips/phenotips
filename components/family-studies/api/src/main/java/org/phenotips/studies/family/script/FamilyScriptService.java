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
package org.phenotips.studies.family.script;

import org.phenotips.studies.family.FamilyInformation;
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Processing;
import org.phenotips.studies.family.Validation;
import org.phenotips.studies.family.internal.PedigreeUtils;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * Script service for working with families.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Singleton
@Named("families")
public class FamilyScriptService
{
    private static final String FAMILY_NOT_FOUND = "Could not get patient's family {}";

    @Inject
    private Logger logger;

    @Inject
    private FamilyUtils utils;

    @Inject
    private Processing processing;

    @Inject
    private Validation validation;

    @Inject
    private FamilyInformation familyInformation;

    /**
     * Always creates a new family, with no family members.
     *
     * @return reference to the new family document. Can be {@link null}
     */
    public DocumentReference createFamily()
    {
        try {
            XWikiDocument familyDoc = this.utils.createProbandlessFamilyDoc(true);
            return familyDoc != null ? familyDoc.getDocumentReference() : null;
        } catch (Exception ex) {
            this.logger.error("Could not create a new blank family document: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Either creates a new family, or gets the existing one if a patient belongs to a family.
     *
     * @param patientId the id of the patient to use when searching for or creating a new family
     * @return reference to the family document. Can be {@link null}
     */
    public DocumentReference createFamily(String patientId)
    {
        try {
            XWikiDocument familyDoc = this.utils.getFamily(patientId);
            if (familyDoc == null) {
                familyDoc = this.utils.createFamilyDoc(patientId);
            }
            return familyDoc != null ? familyDoc.getDocumentReference() : null;
        } catch (Exception ex) {
            this.logger.error("Could not create a new family document for patient [{}]: {}",
                patientId, ex.getMessage());
        }
        return null;
    }

    /**
     * Gets a family, if the patient belongs to one.
     *
     * @param patient the id of the patient who might belong to a family
     * @return reference to the family document if the patient belongs to one, otherwise {@link null}
     */
    public DocumentReference getPatientsFamily(XWikiDocument patient)
    {
        try {
            XWikiDocument doc = this.utils.getFamilyDoc(patient);
            return doc != null ? doc.getDocumentReference() : null;
        } catch (XWikiException ex) {
            this.logger.error(FAMILY_NOT_FOUND, ex.getMessage());
        }
        return null;
    }

    /**
     * Gets a family with `id` (given a patient id, first finds the family the patient belongs to) and returns info
     * about the family.
     *
     * @param id must be a valid family id or a patient id (patient must belong to a family)
     * @return family id and all members that belong to it
     */
    public JSON getFamilyInfo(String id)
    {
        try {
            XWikiDocument doc = this.utils.getFromDataSpace(id);
            XWikiDocument familyDoc = this.utils.getFamilyDoc(doc);
            return this.familyInformation.getBasicInfo(familyDoc);
        } catch (XWikiException ex) {
            this.logger.error(FAMILY_NOT_FOUND, ex.getMessage());
            return new JSONObject(true);
        }
    }

    /**
     * Returns a patient's pedigree, which is the pedigree of a family that patient belongs to, or the patient's own
     * pedigree if the patient does not belong to a family.
     *
     * @param id must be a valid family id or a patient id
     * @return JSON of the data portion of a family or patient pedigree
     */
    public JSON getPedigree(String id)
    {
        try {
            return PedigreeUtils.getPedigree(id, this.utils);
        } catch (XWikiException ex) {
            this.logger.error("Error happend while retrieving pedigree of document with id {}. {}",
                id, ex.getMessage());
            return new JSONObject(true);
        }
    }

    /**
     * Verifies that a patient can be added to a family.
     *
     * @param thisId could be a family id or a patient id. If it is a patient id, finds the family that the patient
     *            belongs to. This family is the one into which the `otherId` patient is added to
     * @param otherId must be a valid patient id
     * @return {@link JSON} with 'validLink' field set to {@link false} if everything is ok, or {@link false} if the
     *         `otherId` patient is not linkable to `thisId` family. In case the linking is invalid, the JSON will also
     *         contain 'errorMessage' and 'errorType'
     */
    public JSON verifyLinkable(String thisId, String otherId)
    {
        try {
            return this.validation.canAddToFamily(thisId, otherId).asVerification();
        } catch (XWikiException ex) {
            return new JSONObject(true);
        }
    }

    /**
     * Performs several operations on the passed in data, and eventually saves it into appropriate documents.
     *
     * @param anchorId could be a family id or a patient id. If a patient does not belong to a family, there is no
     *            processing of the pedigree, and the pedigree is simply saved to that patient record. If the patient
     *            does belong to a family, or a family id is passed in as the `anchorId`, there is processing of the
     *            pedigree, which is then saved to all patient records that belong to the family and the family
     *            documents itself.
     * @param json part of the pedigree data
     * @param image svg part of the pedigree data
     * @return {@link JSON} with 'error' field set to {@link false} if everything is ok, or {@link false} if a known
     *         error has occurred. In case the linking is invalid, the JSON will also contain 'errorMessage' and
     *         'errorType'
     */
    public JSON processPedigree(String anchorId, String json, String image)
    {
        try {
            return this.processing.processPatientPedigree(anchorId, JSONObject.fromObject(json), image).asProcessing();
        } catch (Exception ex) {
            return new JSONObject(true);
        }
    }

    /**
     * Family page should aggregate medical reports of all its members.
     *
     * @param familyDoc to determine which patients' reports should be included
     * @return patient ids mapped to medical reports, which in turn are maps of report name to its link
     */
    public Map<String, Map<String, String>> getReports(XWikiDocument familyDoc)
    {
        try {
            return this.familyInformation.getMedicalReports(familyDoc);
        } catch (Exception ex) {
            this.logger.error("Could not retrieve medical reports from all members of the family. {}", ex.getMessage());
            return new HashMap<>();
        }
    }
}
