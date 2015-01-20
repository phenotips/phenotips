/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.studies.family.script;

import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Processing;
import org.phenotips.studies.family.Validation;
import org.phenotips.studies.family.internal.StatusResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSONObject;

@Component
@Singleton
@Named("family")
public class FamilyScriptService implements ScriptService
{
    @Inject
    FamilyUtils utils;

    @Inject
    Processing processing;

    @Inject
    Validation validation;

    @Inject
    Logger logger;

    /** Can return null */
    public DocumentReference createFamily(String patientId)
    {
        try {
            XWikiDocument doc = utils.createFamilyDoc(patientId);
            return doc != null ? doc.getDocumentReference() : null;
        } catch (Exception ex) {
            logger.error("Could not create a new family document {}", ex.getMessage());
        }
        return null;
    }

    /** Can return null. */
    public DocumentReference getPatientsFamily(XWikiDocument patient)
    {
        try {
            XWikiDocument doc = utils.getFamilyDoc(patient);
            return doc != null ? doc.getDocumentReference() : null;
        } catch (XWikiException ex) {
            logger.error("Could not get patient's family {}", ex.getMessage());
        }
        return null;
    }

    /** Can return null. */
    public String getFamilyStatus(String id)
    {
        boolean isFamily = false;
        try {
            XWikiDocument doc = utils.getFromDataSpace(id);
            XWikiDocument familyDoc = utils.getFamilyDoc(doc);
            boolean hasFamily = familyDoc != null;
            if (hasFamily) {
                isFamily = familyDoc.getDocumentReference() == doc.getDocumentReference();
            }
            return familyStatusResponse(isFamily, hasFamily);
        } catch (XWikiException ex) {
            logger.error("Could not get patient's family {}", ex.getMessage());
            return "";
        }
    }

    /**
     * @return 200 if everything is ok, an error code if the patient is not linkable.
     */
    public String verifyLinkable(String thisId, String otherId)
    {
        StatusResponse response = new StatusResponse();
        try {
            if (validation.hasFamily(otherId)) {
                response.statusCode = 501;
                response.errorType = "familyConflict";
                response.message = String.format("Patient %s belongs to a different family.", otherId);
                return response.asVerification();
            } else if (validation.isInFamily(thisId, otherId)) {
                response.statusCode = 208;
                response.errorType = "alreadyExists";
                response.message = String.format("Patient %s already exists in this family.", otherId);
                return response.asVerification();
            } else {
                return validation.canAddToFamily(thisId, otherId).asVerification();
            }
        } catch (XWikiException ex) {
            return "";
        }
    }

    public String processPedigree(String anchorId, String json, String image)
    {
        try {
            return this.processing.processPatientPedigree(anchorId, JSONObject.fromObject(json), image).asProcessing();
        } catch (Exception ex) {
            return "";
        }
    }

    private static String familyStatusResponse(boolean isFamily, boolean hasFamily) {
        JSONObject json = new JSONObject();
        json.put("isFamilyPage", isFamily);
        json.put("hasFamily", hasFamily);
        return json.toString();
    }
}
