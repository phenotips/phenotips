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

import org.phenotips.Constants;
import org.phenotips.data.Consent;
import org.phenotips.data.ConsentManager;
import org.phenotips.data.ConsentStatus;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * {@link ConsentManager} that integrates with XWiki and the {@link DefaultConsent}.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Singleton
public class PatientXWikiConsentManager implements ConsentManager, Initializable
{
    private static final String GRANTED = "granted";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the XWiki data. */
    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private PatientRepository repository;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    private EntityReference consentReference =
        new EntityReference("PatientConsentConfiguration", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private EntityReference consentIdsHolderReference =
        new EntityReference("PatientConsent", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private EntityReference configurationPageReference =
        new EntityReference("XWikiPreferences", EntityType.DOCUMENT, Constants.XWIKI_SPACE_REFERENCE);

    /**
     * All the consents present in the system. Example usage includes checking if all possible consents present in the
     * system/database are present/filled out in a patient record.
     */
    private List<Consent> systemConsents = new LinkedList<>();

    @Override
    public void initialize() throws InitializationException
    {
        this.refreshSystemConsents();
    }

    @Override
    public List<Consent> getSystemConsents()
    {
        return systemConsents;
    }

    @Override
    public List<Consent> loadConsentsFromPatient(String patientId)
    {
        return this.loadConsentsFromPatient(repository.getPatientById(patientId));
    }

    @Override
    public List<Consent> loadConsentsFromPatient(Patient patient)
    {
        List<Consent> patientConsents = new LinkedList<>();
        /* List of consent ids a patient has agreed to, read from the database */
        List<String> xwikiPatientConsents = new LinkedList<>();

        try {
            DocumentModelBridge patientDocBridge = bridge.getDocument(patient.getDocument());
            XWikiDocument patientDoc = (XWikiDocument) patientDocBridge;
            xwikiPatientConsents = readConsentIdsFromPatientDoc(patientDoc);
        } catch (Exception ex) {
            this.logger.error("Could not load patient document {} or read consents. {}",
                patient == null ? "NULL" : patient.getId(), ex.getMessage());
        }

        /*
         * Using system consents to determine what consents a patient has agreed to, but not reusing the system consents
         * cache, since those should not have a status.
         */
        for (Consent systemConsent : this.systemConsents) {
            Consent copy = DefaultConsent.copy(systemConsent);
            if (xwikiPatientConsents.contains(systemConsent.getId())) {
                copy.setStatus(ConsentStatus.YES);
            } else {
                copy.setStatus(ConsentStatus.NO);
            }
            patientConsents.add(copy);
        }

        return patientConsents;
    }

    @SuppressWarnings("unchecked")
    private List<String> readConsentIdsFromPatientDoc(XWikiDocument doc)
    {
        List<String> ids = new LinkedList<>();
        BaseObject idsHolder = doc.getXObject(consentIdsHolderReference);
        if (idsHolder != null) {
            List<String> patientIds = idsHolder.getListValue(GRANTED);
            if (patientIds != null) {
                ids = patientIds;
            }
        }
        return ids;
    }

    @Override
    public boolean setPatientConsents(Patient patient, Iterable<String> consents)
    {
        try {
            List<Consent> existingConsents = this.selectFromSystem(consents);
            SaveablePatientConsentHolder holder = this.getPatientConsentHolder(patient);
            holder.setConsents(convertToIds(existingConsents));
            holder.save();
            return true;
        } catch (Exception ex) {
            this.logger.error("Could not update consents in patient record {}. {}", patient, ex.getMessage());
        }
        return false;
    }

    /** @return consents that exist in the system and correspond to the given ids */
    private List<Consent> selectFromSystem(Iterable<String> ids)
    {
        List<Consent> existingConsents = new LinkedList<>();
        for (String id : ids) {
            for (Consent consent : this.getSystemConsents()) {
                if (StringUtils.equals(consent.getId(), id)) {
                    existingConsents.add(consent);
                    break;
                }
            }
        }
        return existingConsents;
    }

    @Override
    public boolean grantConsent(Patient patient, String consentId)
    {
        return this.manageConsent(patient, consentId, true);
    }

    @Override
    public boolean revokeConsent(Patient patient, String consentId)
    {
        return this.manageConsent(patient, consentId, false);
    }

    @Override
    public JSONArray toJson(List<Consent> consents)
    {
        JSONArray json = new JSONArray();
        for (Consent consent : consents) {
            json.put(consent.toJson());
        }
        return json;
    }

    @Override
    public List<Consent> fromJson(JSONArray json)
    {
        return null;
    }

    private List<Consent> loadConsentsFromSystem()
    {
        List<Consent> consents = new LinkedList<>();
        try {
            DocumentReference configDocRef = referenceResolver.resolve(this.configurationPageReference);
            DocumentModelBridge configDocBridge = bridge.getDocument(configDocRef);
            XWikiDocument configDoc = (XWikiDocument) configDocBridge;
            List<BaseObject> consentObjects = configDoc.getXObjects(consentReference);
            if (consentObjects != null) {
                for (BaseObject consentObject : consentObjects) {
                    try {
                        consents.add(this.fromXWikiConsentConfiguration(consentObject, configDoc));
                    } catch (Exception ex) {
                        this.logger.warn("An XWiki consent configuration is improperly configured. {}",
                            ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            /* if configuration cannot be loaded, it cannot be loaded; nothing to be done */
            logger.error("Could not load the configurations for patient consents. {}", ex.getMessage());
        }
        return consents;
    }

    // fixme. must be run on every save of XWikiPreferences. There is no UI yet, however if there is to be one, that
    // must be a implemented.
    private void refreshSystemConsents()
    {
        this.systemConsents = loadConsentsFromSystem();
    }

    private Consent fromXWikiConsentConfiguration(BaseObject xwikiConsent, XWikiDocument configDoc)
    {
        String id = xwikiConsent.getStringValue("id");
        String description = configDoc.display("description", "view", xwikiConsent, contextProvider.get());
        /* removing divs and ps */
        description = cleanDescription(description);
        boolean required = intToBool(xwikiConsent.getIntValue("required"));
        return new DefaultConsent(id, description, required);
    }

    private static String cleanDescription(String toClean)
    {
        String clean = toClean;
        clean = clean.replace("<div>", "").replace("</div>", "");
        clean = clean.replace("<p>", "").replace("</p>", "");
        clean = clean.replaceAll("[{]{2}(/{0,1})html(.*?)[}]{2}", "");
        return clean;
    }

    /**
     * @param grant if true will grant the consent, otherwise will revoke
     * @return if operation was successful
     */
    private boolean manageConsent(Patient patient, String consentId, boolean grant)
    {
        if (!this.isValidId(consentId)) {
            this.logger.warn("Invalid consent id ({}) was supplied", consentId);
            return false;
        }
        try {
            SaveablePatientConsentHolder consentHolder = this.getPatientConsentHolder(patient);
            List<String> currentConsents = consentHolder.getConsents();
            if (grant) {
                if (!currentConsents.contains(consentId)) {
                    currentConsents.add(consentId);
                }
            } else {
                currentConsents.remove(consentId);
            }
            consentHolder.setConsents(currentConsents);
            consentHolder.save();
            return true;
        } catch (Exception ex) {
            this.logger
                .error("Could not update consent {} in patient record {}. {}", consentId, patient, ex.getMessage());
            return false;
        }
    }

    private SaveablePatientConsentHolder getPatientConsentHolder(Patient patient) throws Exception
    {
        DocumentModelBridge patientDocBridge = this.bridge.getDocument(patient.getDocument());
        XWikiDocument patientDoc = (XWikiDocument) patientDocBridge;
        return new SaveablePatientConsentHolder(getXWikiConsentHolder(patientDoc), patientDoc,
            this.contextProvider.get());
    }

    /** Either gets the existing consents holder object, or creates a new one. */
    private BaseObject getXWikiConsentHolder(XWikiDocument doc) throws XWikiException
    {
        BaseObject holder = doc.getXObject(this.consentIdsHolderReference);
        if (holder == null) {
            holder = doc.newXObject(this.consentIdsHolderReference, contextProvider.get());
        }
        return holder;
    }

    /** Checks if passed in consent id is actually configured (in the system). */
    private boolean isValidId(String consentId)
    {
        for (Consent consent : this.systemConsents) {
            if (StringUtils.equals(consentId, consent.getId())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> convertToIds(List<Consent> consents)
    {
        List<String> ids = new LinkedList<>();
        for (Consent consent : consents) {
            ids.add(consent.getId());
        }
        return ids;
    }

    private boolean intToBool(int value)
    {
        return value == 1;
    }

    /**
     * Keeps the XWiki patient document and the XWiki consents holder object in memory, allowing to change consents
     * granted and save the document, without reloading either the document or the consent {@link BaseObject}.
     */
    private class SaveablePatientConsentHolder
    {
        private BaseObject consentHolder;

        private XWikiDocument patientDoc;

        private XWikiContext context;

        SaveablePatientConsentHolder(BaseObject consentHolder, XWikiDocument patientDoc, XWikiContext context)
        {
            this.consentHolder = consentHolder;
            this.patientDoc = patientDoc;
            this.context = context;
        }

        @SuppressWarnings("unchecked")
        public List<String> getConsents() throws XWikiException
        {
            return this.consentHolder.getListValue(GRANTED);
        }

        public void setConsents(List<String> consents)
        {
            this.consentHolder.set(GRANTED, consents, context);
        }

        public void save() throws XWikiException
        {
            this.context.getWiki().saveDocument(this.patientDoc, "Changed patient consents", true, context);
        }
    }
}
