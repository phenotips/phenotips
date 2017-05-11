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
package org.phenotips.data.internal;

import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Set;

import org.json.JSONObject;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A read-only wrapper around a PhenotipsPatient with no access to underlying XWikiDocument.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public class SecurePatient implements Patient
{
    private Patient patient;

    /**
     * Constructor that wraps around another (supposedly non-secure) instance of a Patient.
     *
     * @param patient the patient to have a secure wrapper around
     */
    public SecurePatient(Patient patient)
    {
        this.patient = patient;
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

    /**
     * Disallow updates for read-only patients.
     *
     * @param json a JSON object
     */
    @Override
    public void updateFromJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getExternalId()
    {
        return this.patient.getExternalId();
    }

    @Override
    public DocumentReference getReporter()
    {
        return this.patient.getReporter();
    }

    @Override
    public Set<? extends Feature> getFeatures()
    {
        return this.patient.getFeatures();
    }

    @Override
    public Set<? extends Disorder> getDisorders()
    {
        return this.patient.getDisorders();
    }

    @Override
    public <T> PatientData<T> getData(String name)
    {
        return this.patient.getData(name);
    }

    @Override
    public JSONObject toJSON()
    {
        return this.patient.toJSON();
    }

    @Override
    public JSONObject toJSON(Collection<String> selectedFields)
    {
        return this.patient.toJSON(selectedFields);
    }

    @Override
    public EntityReference getType()
    {
        return this.patient.getType();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #getDocumentReference()} instead
     */
    @Deprecated
    @Override
    public DocumentReference getDocument()
    {
        return this.patient.getDocumentReference();
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.patient.getDocumentReference();
    }

    @Override
    public Document getSecureDocument()
    {
        return this.patient.getSecureDocument();
    }

    @Override
    public String getId()
    {
        return this.patient.getId();
    }

    @Override
    public String getName()
    {
        return this.patient.getName();
    }

    @Override
    public String getDescription()
    {
        return this.patient.getDescription();
    }
}
