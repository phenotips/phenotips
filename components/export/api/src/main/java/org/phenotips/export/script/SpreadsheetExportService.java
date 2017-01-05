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
package org.phenotips.export.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.export.internal.SpreadsheetExporter;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.users.UserManager;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * Service for exporting a list of patients into an {@code .xlsx} Excel file.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Unstable
@Component
@Named("spreadsheetexport")
@Singleton
public class SpreadsheetExportService implements ScriptService
{
    @Inject
    private Logger logger;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private PatientRepository patientRepository;

    /** Used for obtaining the current user. */
    @Inject
    private UserManager userManager;

    /** Used for checking access rights. */
    @Inject
    private AuthorizationService access;

    /**
     * Export the provided list of patients into an Excel file, containing the specified columns. The resulting binary
     * filled will be sent through the provided output stream, usually the {@code $response}'s output stream.
     *
     * @param patientIds list of patient IDs of the the patients to export
     * @param enabledFields a list of field names to export; these are internal names, which will be turned into human
     *            readable labels
     * @param outputStream the output stream where the resulting binary {@code .xlsx} file will be sent
     */
    public void export(List<String> patientIds, String[] enabledFields, OutputStream outputStream)
    {
        SpreadsheetExporter exporter = new SpreadsheetExporter();
        try {
            // since scripts do not have access to a non-secure versionof the patient, need to
            // get the actual Patient objects here, and check access rights here
            //
            // FIXME: once new version of entities is in, need to refactor PrimaryEntityManager and incorporate
            //        security features into the entities framework to avoid doing permission checks in client code
            //        that requires non-secure versions of the Patient object
            List<Patient> patients = new LinkedList<>();
            for (String patientId : patientIds) {
                Patient patient = this.patientRepository.get(patientId);
                if (patient == null || !this.access.hasAccess(
                        this.userManager.getCurrentUser(), Right.VIEW, patient.getDocumentReference())) {
                    continue;
                }
                patients.add(patient);
            }

            exporter.export(enabledFields, patients, outputStream);
        } catch (Exception ex) {
            this.logger.error("Error caught while generating an export spreadsheet", ex);
        }
    }
}
