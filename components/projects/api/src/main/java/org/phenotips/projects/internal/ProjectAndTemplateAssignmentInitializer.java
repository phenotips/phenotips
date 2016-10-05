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
package org.phenotips.projects.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRecordInitializer;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.PrimaryEntityProperty;
import org.phenotips.projects.data.Project;
import org.phenotips.projects.data.ProjectRepository;
import org.phenotips.templates.data.Template;
import org.phenotips.templates.data.TemplateRepository;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Initializes the patient with selected project(s) and template.
 *
 * @version $Id$
 */
@Component(roles = { PatientRecordInitializer.class })
@Named("projectAndTemplate")
@Singleton
public class ProjectAndTemplateAssignmentInitializer implements PatientRecordInitializer
{
    private static final String PROJECTS_SELECTED_KEY = "projectsSelected";

    private static final String TEMPLATE_SELECTED_KEY = "templateSelected";

    @Inject
    @Named("Project")
    private ProjectRepository projectRepository;

    @Inject
    @Named("Template")
    private TemplateRepository templateRepository;

    @Inject
    @Named("Project:Patient")
    private PrimaryEntityGroupManager<Project, Patient> patientsInProject;

    @Inject
    @Named("Patient:Template")
    private PrimaryEntityProperty<Patient, Template> templateInPatient;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public void initialize(Patient patient)
    {
        XWikiContext xContext = this.contextProvider.get();
        XWikiRequest request = xContext.getRequest();
        Map<String, String[]> parameterMap = request.getParameterMap();

        // Projects selected
        String[] projectsSelectedValue = parameterMap.get(PROJECTS_SELECTED_KEY);
        if (projectsSelectedValue != null && projectsSelectedValue.length == 1) {
            Collection<Project> projects = this.projectRepository.getFromString(projectsSelectedValue[0]);

            this.patientsInProject.removeFromAllGroups(patient);
            this.patientsInProject.addToAllGroups(patient, projects);
        }

        // Template selected
        String[] templateSelected = parameterMap.get(TEMPLATE_SELECTED_KEY);
        if (templateSelected != null && templateSelected.length == 1) {
            Template template = this.templateRepository.get(templateSelected[0]);
            if (template != null) {
                this.templateInPatient.set(patient, template);
            }
        }
    }
}
