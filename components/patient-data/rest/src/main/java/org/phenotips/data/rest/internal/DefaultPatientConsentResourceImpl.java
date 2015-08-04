package org.phenotips.data.rest.internal;

import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.PatientConsentResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPatientConsentResourceImpl")
@Singleton
public class DefaultPatientConsentResourceImpl extends XWikiResource implements PatientConsentResource
{
    @Inject
    private Logger logger;

    @Inject
    private PatientRepository repository;

    @Inject
    private AuthorizationManager access;

    @Inject
    private UserManager users;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Override
    public Response getConsents(String patientId)
    {
        return null;
    }

    @Override
    public Response updateConsent(String status, String patientId, String id)
    {
        return null;
    }
}
