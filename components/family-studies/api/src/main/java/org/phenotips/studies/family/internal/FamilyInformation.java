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

import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Validation;

import org.xwiki.component.annotation.Component;

import java.util.List;

import javax.inject.Inject;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import groovy.lang.Singleton;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Used for creating JSON responses containing information about a family.
 *
 * @since 1.3M1
 * @version $Id$
 */
@Component
@Singleton
public class FamilyInformation
{
    @Inject
    private Validation validation;

    @Inject
    private FamilyUtils utils;

    /**
     * Generates the response that describes the family and its members.
     * @param familyDoc the XWiki document of the family
     * @return JSON with info about the family, each member and the current user's permissions
     * @throws XWikiException could occur while getting the warning message
     */
    public JSON getResponse(XWikiDocument familyDoc) throws XWikiException
    {
        List<String> members = this.utils.getFamilyMembers(familyDoc);
        FamilyInformationJson family = new FamilyInformationJson();
        family.setFamily(familyDoc).setWarning(utils);

        for (String memberId : members) {
            FamilyMemberInformationJson member = new FamilyMemberInformationJson();
            FamilyMemberPermissionsJson permissions = new FamilyMemberPermissionsJson();
            permissions.setEditRights(memberId, validation);
            member.setId(memberId).setExternalId().setName().setPermissions(permissions);
            family.addMember(member);
        }

        return family.getJson();
    }

    private static class FamilyInformationJson extends AbstractInformationJson
    {
        private JSONArray members = new JSONArray();

        private XWikiDocument family;

        public FamilyInformationJson()
        {
            this.json.put("familyMembers", members);
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
            return this;
        }
    }

    private static class FamilyMemberInformationJson extends AbstractInformationJson
    {
        private JSONObject json = new JSONObject();

        private String id;

        public FamilyMemberInformationJson setId(String id)
        {
            this.id = id;
            this.json.put("id", id);
            return this;
        }

        public FamilyMemberInformationJson setExternalId()
        {
            // todo
            this.json.put("identifier", "");
            return this;
        }

        public FamilyMemberInformationJson setName()
        {
            // todo
            this.json.put("name", "");
            return this;
        }

        public FamilyMemberInformationJson setPermissions(FamilyMemberPermissionsJson permissions)
        {
            this.json.put("permisssions", permissions.getJson());
            return this;
        }
    }

    private static class FamilyMemberPermissionsJson extends AbstractInformationJson
    {
        public FamilyMemberPermissionsJson setEditRights(String id, Validation validation)
        {
            this.json.put("hasEdit", validation.hasPatientEditAccess(id));
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

