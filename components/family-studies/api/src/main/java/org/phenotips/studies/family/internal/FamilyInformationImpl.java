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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientRepository;
import org.phenotips.studies.family.FamilyInformation;
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Validation;

import org.xwiki.component.annotation.Component;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import groovy.lang.Singleton;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Default implementation of {@link FamilyInformation}. Contains private classes that allow for chaining of commands
 * while generating JSON.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Singleton
public class FamilyInformationImpl implements FamilyInformation
{
    @Inject
    private Validation validation;

    @Inject
    private FamilyUtils utils;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private UserManager userManager;

    @Override
    public JSON getBasicInfo(XWikiDocument familyDoc) throws XWikiException
    {
        List<String> members = this.utils.getFamilyMembers(familyDoc);
        FamilyInformationJson family = new FamilyInformationJson();
        User user = this.userManager.getCurrentUser();
        family.setFamily(familyDoc).setWarning(utils);

        for (String memberId : members) {
            Patient patient = this.patientRepository.getPatientById(memberId);
            FamilyMemberInformationJson member = new FamilyMemberInformationJson();
            FamilyMemberPermissionsJson permissions = new FamilyMemberPermissionsJson();
            permissions.setViewRights(patient, user).setEditRights(patient, user);
            member.setId(patient).setExternalId(patient).setName(patient).setMedicalReports(patient, user, this)
                .setUrl(patient, utils).setPermissions(permissions);
            family.addMember(member);
        }

        return family.getJson();
    }

    @Override
    public Map<String, Map<String, String>> getMedicalReports(XWikiDocument familyDoc)
        throws XWikiException
    {
        Map<String, Map<String, String>> allFamilyLinks = new HashMap<>();

        User currentUser = this.userManager.getCurrentUser();
        List<String> members = utils.getFamilyMembers(familyDoc);
        for (String member : members) {
            Patient patient = this.patientRepository.getPatientById(member);
            allFamilyLinks.put(patient.getId(), getMedicalReports(patient, currentUser));
        }
        return allFamilyLinks;
    }

    private Map<String, String> getMedicalReports(Patient patient, User user)
    {
        PatientData<String> links = patient.getData("medicalreports");
        Map<String, String> mapOfLinks = new HashMap<>();
        if (this.validation.hasPatientViewAccess(patient, user)) {
            if (links != null) {
                Iterator<Map.Entry<String, String>> iterator = links.dictionaryIterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    mapOfLinks.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return mapOfLinks;
    }

    private static class FamilyInformationJson extends AbstractInformationJson
    {
        private static final String FAMILY_MEMBERS = "familyMembers";

        private JSONArray members = new JSONArray();

        private XWikiDocument family;

        public FamilyInformationJson()
        {
            this.json.put(FAMILY_MEMBERS, members);
        }

        public FamilyInformationJson setFamily(XWikiDocument family)
        {
            this.family = family;
            this.json.put("familyPage", family == null ? null : family.getDocumentReference().getName());
            return this;
        }

        public FamilyInformationJson setWarning(FamilyUtils utils) throws XWikiException
        {
            this.json.put("warning", utils.getWarningMessage(this.family));
            return this;
        }

        public FamilyInformationJson addMember(FamilyMemberInformationJson member)
        {
            members.add(member.getJson());
            this.json.put(FAMILY_MEMBERS, members);
            return this;
        }
    }

    private static class FamilyMemberInformationJson extends AbstractInformationJson
    {
        public FamilyMemberInformationJson setId(Patient patient)
        {
            this.json.put("id", patient.getId());
            return this;
        }

        public FamilyMemberInformationJson setExternalId(Patient patient)
        {
            this.json.put("identifier", patient.getExternalId());
            return this;
        }

        public FamilyMemberInformationJson setName(Patient patient)
        {
            PatientData<String> names = patient.getData("patientName");

            this.json.put("name", String.format("%s %s", StringUtils.defaultString(names.get("first_name")),
                StringUtils.defaultString(names.get("last_name"))).trim());
            return this;
        }

        public FamilyMemberInformationJson setMedicalReports(Patient patient, User user,
            FamilyInformationImpl familyInfo)
        {
            this.json.put("reports", familyInfo.getMedicalReports(patient, user));
            return this;
        }

        public FamilyMemberInformationJson setUrl(Patient patient, FamilyUtils utils) {
            this.json.put("url", utils.getViewUrl(patient.getDocument()));
            return this;
        }

        public FamilyMemberInformationJson setPermissions(FamilyMemberPermissionsJson permissions)
        {
            this.json.put("permissions", permissions.getJson());
            return this;
        }
    }

    private class FamilyMemberPermissionsJson extends AbstractInformationJson
    {
        public FamilyMemberPermissionsJson setEditRights(Patient patient, User user)
        {
            this.json.put("hasEdit", validation.hasPatientEditAccess(patient, user));
            return this;
        }

        public FamilyMemberPermissionsJson setViewRights(Patient patient, User user)
        {
            this.json.put("hasView", validation.hasPatientViewAccess(patient, user));
            return this;
        }
    }

    private abstract static class AbstractInformationJson
    {
        protected JSONObject json = new JSONObject();

        public JSON getJson()
        {
            return json;
        }
    }
}

