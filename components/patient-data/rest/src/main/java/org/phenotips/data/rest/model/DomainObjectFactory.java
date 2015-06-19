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
package org.phenotips.data.rest.model;

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import javax.ws.rs.core.UriInfo;

/**
 * Factory class for generating REST representations of various types of entities.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Unstable
@Role
public interface DomainObjectFactory
{
    /**
     * Create the REST representation for a {@link Patient}'s summary.
     *
     * @param patient the patient to serialize
     * @param uriInfo the URI information for the rest system and the current request
     * @return a patient summary, or {@code null} if the current user doesn't have access to the patient or accessing
     *         the patient data fails
     */
    PatientSummary createPatientSummary(Patient patient, UriInfo uriInfo);
}
