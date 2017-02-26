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
package org.phenotips.studies.family.internal.export;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.studies.family.Family;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Contains information to be returned in a family search (specifically in PhenotipsFamilyExport).
 *
 * @version $Id$
 */
public class FamilySearchResult
{
    private String externalId;

    private String id;

    private String reference;

    private String url;

    private String description;

    /**
     * Create a new search result from a family and permissions.
     *
     * @param family contains the details of the family found
     * @param requiredPermissions permissions to extract the URL from
     */
    public FamilySearchResult(Family family, String requiredPermissions)
    {
        this.externalId = family.getExternalId();
        this.id = family.getId();
        this.reference = family.getDocumentReference().toString();
        this.url = family.getURL(requiredPermissions);

        setBasicDescription();
    }

    /**
     * Create a new search result based on a patient's family and permissions.
     *
     * @param patient patient
     * @param usePatientName if true the patient name will appear in the description
     * @param family contains the details of the family found
     * @param requiredPermissions permissions to extract the URL from
     */
    public FamilySearchResult(Patient patient, boolean usePatientName, Family family, String requiredPermissions)
    {
        this(family, requiredPermissions);
        addPatientDescription(patient, usePatientName);

    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof FamilySearchResult)) {
            return false;
        }
        return (this.id == ((FamilySearchResult) other).getId());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.id);
    }

    private void setBasicDescription()
    {
        StringBuilder descriptionSb = new StringBuilder(this.getId());
        if (StringUtils.isNotEmpty(this.externalId)) {
            descriptionSb.append(" (").append(this.externalId).append(")");
        }
        this.description = descriptionSb.toString();
    }

    private void addPatientDescription(Patient patient, boolean usePatientName)
    {
        StringBuilder descriptionSb = new StringBuilder(this.getDescription());

        descriptionSb.append(" [");
        descriptionSb.append("Patient ").append(patient.getId());

        String patientExternalId = patient.getExternalId();
        if (StringUtils.isNotEmpty(patientExternalId)) {
            descriptionSb.append(", identifier: ").append(patientExternalId);
        }

        if (usePatientName) {
            String patientName = "";
            if (usePatientName) {
                PatientData<String> patientNames = patient.getData("patientName");
                String firstName = StringUtils.defaultString(patientNames.get("first_name"));
                String lastName = StringUtils.defaultString(patientNames.get("last_name"));
                patientName = (firstName + " " + lastName).trim();
            }
            if (StringUtils.isNotEmpty(patientName)) {
                descriptionSb.append(", name: ").append(patientName);
            }
        }
        descriptionSb.append("]");

        this.description = descriptionSb.toString();
    }

    /**
     * @return external id
     */
    public String getExternalId()
    {
        return this.externalId;
    }

    /**
     * @return id
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return reference
     */
    public String getReference()
    {
        return this.reference;
    }

    /**
     * @return URL
     */
    public String getUrl()
    {
        return this.url;
    }

    /**
     * @return description
     */
    public String getDescription()
    {
        return this.description;
    }
}
