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
 * The service exposing {@link org.phenotips.configuration.internal.consent.DefaultConsentAuthorizer}.
 *
 * @version $Id$
 * @since 1.3M1
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

    /**
     * Finds a patient by `patientId`, and calls {@link ConsentAuthorizer#filterForm(List, Patient)}.
     * @param elements of a patient form to be filtered
     * @param patientId of a patient whose data will be displayed through the `elements`
     * @return same as {@link ConsentAuthorizer#filterForm(List, Patient)}
     */
    public List<RecordElement> filterEnabled(List<RecordElement> elements, String patientId)
    {
        Patient patient = this.repository.getPatientById(patientId);
        return this.authorizer.filterForm(elements, patient);
    }

    /**
     * Forwards the call to {@link ConsentAuthorizer}.
     * @return same as {@link ConsentAuthorizer#consentsGloballyEnabled()}
     */
    public boolean consentsGloballyEnabled()
    {
        return this.authorizer.consentsGloballyEnabled();
    }
}
