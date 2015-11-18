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

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.response.JSONResponse;
import org.phenotips.studies.family.response.StatusResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides utility methods for working with family documents and patients.
 *
 * @version $Id$
 */
@Component
@Singleton
public class XWikiFamilyRepository implements FamilyRepository
{
    private static final String PREFIX = "FAM";

    private static final EntityReference FAMILY_TEMPLATE =
        new EntityReference("FamilyTemplate", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** XWiki class that represents objects that contain a string reference to a family document. */
    private static final EntityReference FAMILY_REFERENCE =
        new EntityReference("FamilyReference", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final EntityReference OWNER_CLASS =
        new EntityReference("OwnerClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String FAMILY_REFERENCE_FIELD = "reference";

    @Inject
    private static Logger logger;

    @Inject
    private static Provider<XWikiContext> provider;

    /** Runs queries for finding families. */
    @Inject
    private static QueryManager qm;

    @Inject
    private static UserManager userManager;

    @Inject
    @Named("current")
    private static DocumentReferenceResolver<String> referenceResolver;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private static DocumentReferenceResolver<EntityReference> entityReferenceResolver;

    @Inject
    private static XWikiFamilyPermissions familyPermissions;

    @Override
    public Family createFamily()
    {
        XWikiDocument newFamilyDocument = null;
        try {
            newFamilyDocument = this.createFamilyDocument();
        } catch (Exception e) {
            XWikiFamilyRepository.logger.error("Could not create a new family document: {}", e.getMessage());
        }
        if (newFamilyDocument == null) {
            XWikiFamilyRepository.logger.debug("Could not create a family document");
            return null;
        }

        return new XWikiFamily(newFamilyDocument);
    }

    @Override
    public Family getFamilyById(String id)
    {
        DocumentReference reference = XWikiFamilyRepository.referenceResolver.resolve(id, Family.DATA_SPACE);
        XWikiContext context = XWikiFamilyRepository.provider.get();
        try {
            XWikiDocument familyDocument = context.getWiki().getDocument(reference, context);
            if (familyDocument.getXObject(Family.CLASS_REFERENCE) != null) {
                return new XWikiFamily(familyDocument);
            }
        } catch (XWikiException ex) {
            XWikiFamilyRepository.logger.error("Failed to load document for family [{}]: {}", id, ex.getMessage(), ex);
        }
        XWikiFamilyRepository.logger.info("Requested family [{}] not found", id);
        return null;
    }

    @Override
    /**
     * Returns a Family object for patient.
     * If there's an XWiki family document but no XWikiFamily object associated with it in the cache,
     * a new XWikiFamily object will be created.
     * @param patient for which to look for a family
     * @return Family if there's an XWiki family document, otherwise null
     */
    public Family getFamilyForPatient(Patient patient)
    {
        String patientId = patient.getId();
        XWikiDocument patientDocument = null;
        try {
            patientDocument = getDocument(patient);
        } catch (XWikiException e) {
            XWikiFamilyRepository.logger.error("Can't find patient document for patient [{}]", patient.getId());
            return null;
        }

        DocumentReference familyReference = getFamilyReference(patientDocument);
        if (familyReference == null) {
            XWikiFamilyRepository.logger.debug("Family not found for patient [{}]", patientId);
            return null;
        }

        try {
            XWikiDocument document = getDocument(familyReference);
            return new XWikiFamily(document);
        } catch (XWikiException e) {
            XWikiFamilyRepository.logger.error("Can't find family document for patient [{}]", patient.getId());
            return null;
        }
    }

    /**
     * Sets the reference to the family document in the patient document.
     *
     * @param patientDoc to set the family reference
     * @param familyDoc family of the patient
     * @param context context
     * @throws XWikiException error when creating a reference
     */
    public static void setFamilyReference(XWikiDocument patientDoc, XWikiDocument familyDoc, XWikiContext context)
        throws XWikiException
    {
        BaseObject pointer = patientDoc.getXObject(FAMILY_REFERENCE);
        if (pointer == null) {
            pointer = patientDoc.newXObject(FAMILY_REFERENCE, context);
        }
        pointer.set(FAMILY_REFERENCE_FIELD, familyDoc.getDocumentReference().toString(), context);
    }

    /**
     * Removes a family reference from a patient.
     *
     * @param patientDoc to set the family reference
     * @return true if successful
     */
    public static boolean removeFamilyReference(XWikiDocument patientDoc)
    {
        BaseObject pointer = patientDoc.getXObject(FAMILY_REFERENCE);
        if (pointer != null) {
            return patientDoc.removeXObject(pointer);
        }
        return false;
    }

    @Override
    public JSONResponse canPatientBeAddedToFamily(Patient patient, Family family)
    {
        JSONResponse response = new JSONResponse(StatusResponse.OK);
        Family familyForPatient = this.getFamilyForPatient(patient);

        if (familyForPatient != null) {
            if (!familyForPatient.getId().equals(family.getId())) {
                response.setStatusResponse(StatusResponse.ALREADY_HAS_FAMILY);
                response.setMessage(patient.getId(), familyForPatient.getId());
            }
        }

        return response;
    }

    /*
     * returns a reference to a family document from an XWiki patient document.
     */
    private static DocumentReference getFamilyReference(XWikiDocument patientDocument)
    {
        BaseObject familyObject = patientDocument.getXObject(FAMILY_REFERENCE);
        if (familyObject == null) {
            return null;
        }

        String familyDocName = familyObject.getStringValue(FAMILY_REFERENCE_FIELD);
        if (StringUtils.isBlank(familyDocName)) {
            return null;
        }

        DocumentReference familyReference = referenceResolver.resolve(familyDocName, Family.DATA_SPACE);

        return familyReference;
    }

    /*
     * Creates a new document for the family. Only handles XWiki side and no XWikiFamily is created.
     */
    private synchronized XWikiDocument createFamilyDocument()
        throws IllegalArgumentException, QueryException, XWikiException
    {
        XWikiContext context = XWikiFamilyRepository.provider.get();
        XWiki wiki = context.getWiki();
        long nextId = getLastUsedId() + 1;
        String nextStringId = String.format("%s%07d", PREFIX, nextId);

        EntityReference newFamilyRef =
            new EntityReference(nextStringId, EntityType.DOCUMENT, Family.DATA_SPACE);
        XWikiDocument newFamilyDoc = wiki.getDocument(newFamilyRef, context);
        if (!newFamilyDoc.isNew()) {
            throw new IllegalArgumentException("The new family id was already taken.");
        }

        // Copying all objects from template to family
        newFamilyDoc.readFromTemplate(XWikiFamilyRepository.entityReferenceResolver.resolve(FAMILY_TEMPLATE), context);

        // Adding additional values to family
        User currentUser = XWikiFamilyRepository.userManager.getCurrentUser();
        BaseObject ownerObject = newFamilyDoc.newXObject(OWNER_CLASS, context);
        ownerObject.set("owner", currentUser.getId(), context);

        BaseObject familyObject = newFamilyDoc.getXObject(Family.CLASS_REFERENCE);
        familyObject.set("identifier", nextId, context);

        newFamilyDoc.setCreatorReference(currentUser.getProfileDocument());

        XWikiFamilyRepository.familyPermissions.setFamilyPermissionsToCurrentUser(newFamilyDoc);

        wiki.saveDocument(newFamilyDoc, context);

        return newFamilyDoc;
    }

    /*
     * Returns the largest family identifier id
     */
    private long getLastUsedId() throws QueryException
    {
        XWikiFamilyRepository.logger.debug("getLastUsedId()");

        long crtMaxID = 0;
        Query q = XWikiFamilyRepository.qm.createQuery("select family.identifier "
            + "from     Document doc, "
            + "         doc.object(PhenoTips.FamilyClass) as family "
            + "where    family.identifier is not null "
            + "order by family.identifier desc", Query.XWQL).setLimit(1);
        List<Long> crtMaxIDList = q.execute();
        if (crtMaxIDList.size() > 0 && crtMaxIDList.get(0) != null) {
            crtMaxID = crtMaxIDList.get(0);
        }
        crtMaxID = Math.max(crtMaxID, 0);
        return crtMaxID;
    }

    private XWikiDocument getDocument(Patient patient) throws XWikiException
    {
        DocumentReference document = patient.getDocument();
        XWikiDocument patientDocument = getDocument(document);
        return patientDocument;
    }

    private XWikiDocument getDocument(EntityReference docRef) throws XWikiException
    {
        XWikiContext context = XWikiFamilyRepository.provider.get();
        XWiki wiki = context.getWiki();
        return wiki.getDocument(docRef, context);
    }
}
