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
import org.phenotips.data.internal.SecurePatient;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.Pedigree;

import org.xwiki.model.reference.DocumentReference;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A wrapper around PhenotipsFamily with no access to underlying XWikiDocument.
 *
 * @version $Id$
 * @since 1.4
 */
public class SecureFamily implements Family
{
    private Family family;

    /**
     * Constructor that wraps around another (supposedly non-secure) instance of a Family.
     *
     * @param family the family to have a secure wrapper around
     */
    public SecureFamily(Family family)
    {
        if (family == null) {
            throw new UnsupportedOperationException();
        }
        this.family = family;
    }

    /**
     * Disallow access to XWikiDocument.
     *
     * @return always throws
     */
    @Override
    public XWikiDocument getXDocument()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId()
    {
        return this.family.getId();
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.family.getDocumentReference();
    }

    @Override
    public List<String> getMembersIds()
    {
        return this.family.getMembersIds();
    }

    @Override
    public List<Patient> getMembers()
    {
        List<Patient> members = this.family.getMembers();

        List<Patient> result = new LinkedList<>();
        for (Patient p : members) {
            result.add(new SecurePatient(p));
        }
        return result;
    }

    @Override
    public String getProbandId()
    {
        return this.family.getProbandId();
    }

    @Override
    public boolean isMember(Patient patient)
    {
        return this.family.isMember(patient);
    }

    @Override
    public JSONObject toJSON()
    {
        return this.family.toJSON();
    }

    @Override
    public Map<String, Map<String, String>> getMedicalReports()
    {
        return this.family.getMedicalReports();
    }

    @Override
    public String getExternalId()
    {
        return this.family.getExternalId();
    }

    @Override
    public String getURL(String actions)
    {
        return this.family.getURL(actions);
    }

    @Override
    public String getWarningMessage()
    {
        return this.family.getWarningMessage();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof SecureFamily)) {
            return false;
        }
        SecureFamily otherFamily = (SecureFamily) obj;
        return this.family.equals(otherFamily.family);
    }

    @Override
    public int hashCode()
    {
        return this.family.hashCode();
    }

    @Override
    public Pedigree getPedigree()
    {
        return this.family.getPedigree();
    }
}
