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
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSON;
import net.sf.json.JSONArray;

/**
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Singleton
public class PatientXWikiConsentManager implements ConsentManager, Initializable
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

    @Inject
    private PatientRepository repository;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    private EntityReference consentReference =
        new EntityReference("PatientConsentClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private EntityReference configurationPageReference =
        new EntityReference("XWikiPreferences", EntityType.DOCUMENT, Constants.XWIKI_SPACE_REFERENCE);

    /**
     * All the consents present in the system. Example usage includes checking if all possible consents present in the
     * system/database are present/filled out in a patient record.
     */
    private List<Consent> systemConsents = new LinkedList<>();

    @Override public void initialize() throws InitializationException
    {
        this.consentReference = referenceResolver.resolve(this.consentReference);
        this.configurationPageReference = referenceResolver.resolve(this.configurationPageReference);
        this.refreshSystemConsents();
    }

    private List<Consent> loadConsentsFromSystem()
    {
        List<Consent> consents = new LinkedList<>();
        try {
            DocumentModelBridge configDocBridge = bridge.getDocument(new DocumentReference(configurationPageReference));
            XWikiDocument configDoc = (XWikiDocument) configDocBridge;
            List<BaseObject> consentObjects = configDoc.getXObjects(consentReference);
            for (BaseObject consentObject : consentObjects) {
                consents.add(this.fromXWikiConsentConfiguration(consentObject));
            }
        } catch (Exception ex) {
            /* if configuration cannot be loaded, it cannot be loaded; nothing to be done */
            logger.error("Could not load the configurations for patient consents. {}", ex.getMessage());
        }
        return consents;
    }

    private void refreshSystemConsents()
    {
        this.systemConsents = loadConsentsFromSystem();
    }

    private Consent fromXWikiConsentConfiguration(BaseObject xwikiConsent)
    {
        String id = xwikiConsent.getStringValue("id");
        String description = xwikiConsent.displayView("description", provider.get());
        Integer level = xwikiConsent.getIntValue("level");
        boolean required = intToBool(xwikiConsent.getIntValue("required"));
        return new DefaultConsent(id, description, level, required);
    }

    @Override public List<Consent> loadConsentsFromPatient(String patientId)
    {
        return this.loadConsentsFromPatient(repository.getPatientById(patientId));
    }

    @Override public List<Consent> loadConsentsFromPatient(Patient patient)
    {
        return new LinkedList<>();
    }

    @Override public JSON toJson(List<Consent> consents)
    {
        JSONArray json = new JSONArray();
        for (Consent consent : consents) {
            json.add(consent.toJson());
        }
        return json;
    }

    @Override public List<Consent> fromJson(JSON json)
    {
        return null;
    }

    @Override public boolean updatePatient(String patientId, List<Consent> consents)
    {
        return false;
    }

    private boolean intToBool(int value)
    {
        return value == 1;
    }
}
