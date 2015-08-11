package org.phenotips.data.rest.internal;

import org.phenotips.data.Consent;
import org.phenotips.data.ConsentManager;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.PatientConsentResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import net.sf.json.JSON;

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

    @Inject
    private ConsentManager consentManager;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Override
    public Response getConsents(String patientId)
    {
        this.logger.debug("Retrieving patient record [{}] via REST", patientId);
        Patient patient = this.repository.getPatientById(patientId);
        if (patient == null) {
            this.logger.debug("No such patient record: [{}]", patientId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocument())) {
            this.logger.debug("View access denied to user [{}] on patient record [{}]", currentUser, patientId);
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<Consent> consents = consentManager.loadConsentsFromPatient(patient);
        JSON json = consentManager.toJson(consents);
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public Response grantConsent(String patientId, String id)
    {
        return null;
    }

    @Override
    public Response revokeConsent(String patientId, String id)
    {
        return null;
    }
}
