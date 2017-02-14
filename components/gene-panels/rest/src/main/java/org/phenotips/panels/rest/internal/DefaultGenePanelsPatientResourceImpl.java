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
package org.phenotips.panels.rest.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.panels.GenePanelFactory;
import org.phenotips.panels.rest.GenePanelsPatientResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Default implementation of the {@link GenePanelsPatientResource}.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("org.phenotips.panels.rest.internal.DefaultGenePanelsPatientResourceImpl")
@Singleton
public class DefaultGenePanelsPatientResourceImpl extends XWikiResource implements GenePanelsPatientResource
{
    /** A factory for gene panels. */
    @Inject
    private GenePanelFactory genePanelFactory;

    /** The logging object. */
    @Inject
    private Logger logger;

    /** The secure patient repository. */
    @Inject
    @Named("secure")
    private PatientRepository repository;

    @Override
    public Response getPatientGeneCounts(final String patientId)
    {
        // Check if patient ID is provided.
        if (StringUtils.isBlank(patientId)) {
            this.logger.error("No patient ID was provided.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        // Patient ID is provided, get the gene panel for the patient if user has access and if the patient exists.
        try {
            // Try to get the patient object.
            final Patient patient = this.repository.get(patientId);
            // Check if the patient exists.
            if (patient == null) {
                this.logger.error("Could not find patient with ID {}", patientId);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            final JSONObject genePanel = this.genePanelFactory.build(patient).toJSON();
            return Response.ok(genePanel, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (final SecurityException ex) {
            // The user has no access rights for the requested patient.
            this.logger.error("View access denied on patient record [{}]: {}", patientId, ex.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
}
