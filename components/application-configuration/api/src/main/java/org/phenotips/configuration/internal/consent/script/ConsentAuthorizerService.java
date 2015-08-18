package org.phenotips.configuration.internal.consent.script;

import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.internal.consent.ConsentAuthorizer;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 */
@Unstable
@Component
@Named("consentauthorizer")
@Singleton
public class ConsentAuthorizerService implements ScriptService
{
    @Inject
    private ConsentAuthorizer authorizer;

    @Inject
    private PatientRepository repository;

    public List<RecordElement> filterEnabled(List<RecordElement> elements, String patientId)
    {
        Patient patient = this.repository.getPatientById(patientId);
        return this.authorizer.filterForm(elements, patient);
    }

    public boolean consentsGloballyEnabled()
    {
        return this.authorizer.consentsGloballyEnabled();
    }
}
