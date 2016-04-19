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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRecordInitializer;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Implementation of patient data access service using XWiki as the storage backend, where patients in documents having
 * an object of type {@code PhenoTips.PatientClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Singleton
public class PhenoTipsPatientRepository implements PatientRepository
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the current execution context. */
    @Inject
    private Execution execution;

    /** Provides access to the XWiki data. */
    @Inject
    private DocumentAccessBridge bridge;

    /** Runs queries for finding patients. */
    @Inject
    private QueryManager qm;

    /** Parses string representations of document references into proper references. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringResolver;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    @Override
    public Patient getPatientById(String id)
    {
        DocumentReference reference = this.stringResolver.resolve(id, Patient.DEFAULT_DATA_SPACE);
        try {
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument(reference);
            if (doc != null && doc.getXObject(Patient.CLASS_REFERENCE) != null) {
                return new PhenoTipsPatient(doc);
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to access patient with id [{}]: {}", id, ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public Patient getPatientByExternalId(String externalId)
    {
        try {
            Query q = this.qm.createQuery("where doc.object(PhenoTips.PatientClass).external_id = :eid", Query.XWQL);
            q.bindValue("eid", externalId);
            List<String> results = q.execute();
            if (results.size() == 1) {
                DocumentReference reference =
                    this.stringResolver.resolve(results.get(0), Patient.DEFAULT_DATA_SPACE);
                return new PhenoTipsPatient((XWikiDocument) this.bridge.getDocument(reference));
            }
        } catch (QueryException ex) {
            this.logger.warn("Failed to search for the patient with external id [{}]: {}", externalId, ex.getMessage(),
                ex);
        } catch (Exception ex) {
            this.logger.warn("Failed to access patient with external id [{}]: {}", externalId, ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public Patient loadPatientFromDocument(DocumentModelBridge document)
    {
        XWikiDocument xdocument = (XWikiDocument) document;
        if (xdocument.getXObject(Patient.CLASS_REFERENCE) == null) {
            throw new IllegalArgumentException("No patient stored in the provided document ["
                + document.getDocumentReference() + "]");
        }
        return new PhenoTipsPatient(xdocument);
    }

    @Override
    public synchronized Patient createNewPatient(DocumentReference creator)
    {
        try {
            // FIXME Take these from the configuration
            String prefix = "P";

            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            long id = getLastUsedId();
            DocumentReference newDoc;
            do {
                newDoc = this.referenceResolver.resolve(new EntityReference(
                    prefix + String.format("%07d", ++id), EntityType.DOCUMENT, Patient.DEFAULT_DATA_SPACE));
            } while (this.bridge.exists(newDoc));
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument(newDoc);
            doc.readFromTemplate(this.referenceResolver.resolve(PhenoTipsPatient.TEMPLATE_REFERENCE), context);
            doc.setTitle(newDoc.getName());
            doc.getXObject(Patient.CLASS_REFERENCE).setLongValue("identifier", id);
            if (creator != null) {
                doc.setCreatorReference(creator);
                doc.setAuthorReference(creator);
                doc.setContentAuthorReference(creator);
            }
            context.getWiki().saveDocument(doc, context);

            Patient patient = new PhenoTipsPatient(doc);
            List<PatientRecordInitializer> initializers = Collections.emptyList();
            try {
                initializers = ComponentManagerRegistry.getContextComponentManager().getInstanceList(
                    PatientRecordInitializer.class);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to get initializers", e);
            }

            for (PatientRecordInitializer initializer : initializers) {
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
    public synchronized Patient createNewPatient()
    {
        return createNewPatient(this.bridge.getCurrentUserReference());
    }

    private long getLastUsedId() throws QueryException
    {
        long crtMaxID = 0;
        Query q =
            this.qm.createQuery(
                "select patient.identifier from Document doc, doc.object(PhenoTips.PatientClass) as patient"
                    + " where patient.identifier is not null order by patient.identifier desc", Query.XWQL)
                .setLimit(1);
        List<Long> crtMaxIDList = q.execute();
        if (!crtMaxIDList.isEmpty() && crtMaxIDList.get(0) != null) {
            crtMaxID = crtMaxIDList.get(0);
        }
        crtMaxID = Math.max(crtMaxID, 0);
        return crtMaxID;
    }

    @Override
    public Collection<Patient> getAllPatients()
    {
        List<String> patientIds = null;
        try {
            Query q = this.qm.createQuery(
                "select doc.name "
                + "from Document doc, "
                + "doc.object(PhenoTips.PatientClass) as patient "
                + "where patient.identifier is not null ", Query.XWQL);
            patientIds = q.execute();
        } catch (QueryException e) {
            this.logger.error("Failed to read query all patients: {}", e);
            return null;
        }

        List<Patient> patients = new ArrayList<>(patientIds.size());
        for (String patientId : patientIds) {
            Patient patient = this.getPatientById(patientId);
            if (patient != null) {
                patients.add(patient);
            }
        }

        return patients;
    }
}
