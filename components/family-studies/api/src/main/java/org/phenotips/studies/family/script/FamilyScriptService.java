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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
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
            XWikiDocument familyDoc;
            familyDoc = utils.getFamilyOfPatient(patientId);
            if (familyDoc == null) {
                familyDoc = utils.createFamilyDoc(patientId);
            }
            return familyDoc != null ? familyDoc.getDocumentReference() : null;
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
    public JSON getFamilyStatus(String id)
    {
        boolean isFamily = false;
        try {
            XWikiDocument doc = utils.getFromDataSpace(id);
            XWikiDocument familyDoc = utils.getFamilyDoc(doc);
            return familyStatusResponse(familyDoc, utils.getFamilyMembers(familyDoc));
        } catch (XWikiException ex) {
            logger.error("Could not get patient's family {}", ex.getMessage());
            return new JSONObject(true);
        }
    }

    /**
     * @return 200 if everything is ok, an error code if the patient is not linkable.
     */
    public JSON verifyLinkable(String thisId, String otherId)
    {
        try {
            return validation.canAddToFamily(thisId, otherId).asVerification();
        } catch (XWikiException ex) {
            return new JSONObject(true);
        }
    }

    public JSON processPedigree(String anchorId, String json, String image)
    {
        try {
            return this.processing.processPatientPedigree(anchorId, JSONObject.fromObject(json), image).asProcessing();
        } catch (Exception ex) {
            return new JSONObject(true);
        }
    }

    private static JSON familyStatusResponse(XWikiDocument family, List<String> members)
    {
        JSONObject json = new JSONObject();
        json.put("familyPage", family == null ? null : family.getDocumentReference().getName());

        JSONArray membersJson = new JSONArray();
        for (String member : members) {
            JSONObject memberJson = new JSONObject();
            memberJson.put("id", member);
            memberJson.put("identifier", "");
            memberJson.put("name", "");
            membersJson.add(memberJson);
        }

        json.put("familyMembers", membersJson);
        return json;
    }
}
