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
package org.phenotips.configuration.internal.consent;

import org.phenotips.configuration.RecordElement;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;

import java.util.List;

/**
 * Grants authorization to keep a {@link RecordElement} enabled.
 *
 * @version $Id$
 * @since 1.2RC2
 */
@Role
public interface ConsentAuthorizer
{
    /**
     * The system might have no consents configured, or purposefully turned them off, in which case the authorizer
     * should not be doing checks.
     *
     * @return true if this {@link ConsentAuthorizer} is on, false otherwise
     */
    boolean consentsGloballyEnabled();

    /**
     * The patient form (the one displayed to the user) consists of blocks, some of which should not be presented to
     * the user based on consents granted.
     * @param elements the form elements which will be filtered
     * @param patient the patient record that will be presented to the user
     * @return list of form elements that should be presented to the user
     */
    List<RecordElement> filterForm(List<RecordElement> elements, Patient patient);

    /**
     * Determines if any interactions with a patient record given the consents that have been granted should be
     * allowed.
     *
     * @param patient record in question
     * @return {@link true} if the patient record can be modified or viewed, {@link false} otherwise
     */
    boolean authorizeInteraction(Patient patient);

    /**
     * Determines if any interactions with a patient record given the consents that have been granted should be
     * allowed.
     *
     * @param grantedConsents list of consent ids that have been granted; can be {@link null}
     * @return true if interactions with the patient record can occur, false otherwise.
     */
    boolean authorizeInteraction(Iterable<String> grantedConsents);

    /**
     * This {@link ConsentAuthorizer} takes into account several factors in deciding whether a {@link RecordElement} is
     * enabled. Sometimes, however, it is necessary to know if an {@link RecordElement} is consented, rather than if it
     * is fully enabled.
     * @param element in question
     * @param patient record which does or does not consent to collection of data with the `element`
     * @return {@link true} if the element has necessary consents granted, {@link false} otherwise
     */
    boolean isElementConsented(RecordElement element, Patient patient);
}
