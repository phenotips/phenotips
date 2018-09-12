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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRecordInitializer;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Implementation of patient data access service using XWiki as the storage backend, where patients in documents having
 * an object of type {@code PhenoTips.PatientClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component(roles = PatientRepository.class)
@Singleton
public class PhenoTipsPatientRepository extends PatientEntityManager implements PatientRepository
{
    @Inject
    private Provider<List<PatientRecordInitializer>> initializers;

    @Override
    public Patient getPatientById(String id)
    {
        return get(id);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The name in this case is actually the external identifier, stored in the {@code external_id} property of the
     * {@code PatientClass} object.
     * </p>
     */
    @Override
    public Patient getByName(String name)
    {
        try {
            Query q = this.qm.createQuery("where doc.object(PhenoTips.PatientClass).external_id = :eid", Query.XWQL);
            q.bindValue("eid", name);
            List<String> results = q.execute();
            if (results.size() == 1) {
                DocumentReference reference =
                    this.stringResolver.resolve(results.get(0), Patient.DEFAULT_DATA_SPACE);
                return new PhenoTipsPatient((XWikiDocument) this.bridge.getDocument(reference));
            }
        } catch (QueryException ex) {
            this.logger.warn("Failed to search for the patient with external id [{}]: {}", name, ex.getMessage(),
                ex);
        } catch (Exception ex) {
            this.logger.warn("Failed to access patient with external id [{}]: {}", name, ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public Patient getPatientByExternalId(String externalId)
    {
        return getByName(externalId);
    }

    @Override
    public Patient loadPatientFromDocument(DocumentModelBridge document)
    {
        return load(document);
    }

    @Override
    public synchronized Patient createNewPatient()
    {
        return create();
    }

    @Override
    public synchronized Patient createNewPatient(DocumentReference creator)
    {
        return create(creator);
    }

    @Override
    public synchronized Patient create(DocumentReference creator)
    {
        try {
            XWikiContext context = this.xcontextProvider.get();
            Patient patient = super.create(creator);
            XWikiDocument doc =
                (XWikiDocument) this.bridge.getDocument(this.stringResolver.resolve(patient.getId(),
                    Patient.DEFAULT_DATA_SPACE));
            doc.getXObject(Patient.CLASS_REFERENCE).setLongValue("identifier",
                Integer.parseInt(patient.getId().replaceAll("\\D++", StringUtils.EMPTY)));
            context.getWiki().saveDocument(doc, context);
            for (PatientRecordInitializer initializer : this.initializers.get()) {
                try {
                    initializer.initialize(patient);
                } catch (Exception ex) {
                    // Initializers shouldn't block the creation of a new patient, especially since the new patient
                    // has already been saved...
                    this.logger.warn("Patient initializer [{}] failed: {}", initializer.getClass().getName(),
                        ex.getMessage(), ex);
                }
            }
            return patient;
        } catch (Exception ex) {
            this.logger.warn("Failed to create patient: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public boolean deletePatient(String id)
    {
        Patient patient = this.getPatientById(id);
        if (patient == null) {
            this.logger.warn("Can't delete patient record with id [{}], no such patient", id);
            return false;
        }
        return this.delete(patient);
    }

    @Override
    protected long getLastUsedId()
    {
        long crtMaxID = 0;
        try {
            Query q =
                this.qm.createQuery(
                    "select patient.identifier from Document doc, doc.object(PhenoTips.PatientClass) as patient"
                        + " where patient.identifier is not null order by patient.identifier desc",
                    Query.XWQL).setLimit(1);
            List<Long> crtMaxIDList = q.execute();
            if (!crtMaxIDList.isEmpty() && crtMaxIDList.get(0) != null) {
                crtMaxID = crtMaxIDList.get(0);
            }
            crtMaxID = Math.max(crtMaxID, 0);
        } catch (QueryException ex) {
            this.logger.warn("Failed to get the last used identifier: {}", ex.getMessage());
        }
        return crtMaxID;
    }
}
