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
package org.phenotips.projects.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.projects.data.Project;
import org.phenotips.projects.internal.ProjectAndTemplateBinder;
import org.phenotips.projects.internal.ProjectsRepository;
import org.phenotips.templates.data.Template;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */

@Component
@Named("projects")
@Singleton
public class ProjectsScriptService implements ScriptService
{
    @Inject
    private ProjectsRepository projectsRepository;

    @Inject
    private PatientRepository patientsRepository;

    @Inject
    private ProjectAndTemplateBinder ptBinder;

    /**
     * Returns a project by an id.
     * @param projectId id of the project to return
     * @return a project object
     */
    public Project getProjectById(String projectId)
    {
        return this.projectsRepository.getProjectById(projectId);
    }

    /**
     * @return a collection of all projects that the current user can contribute to.
     */
    public Collection<Project> getAllProjectsWithContributionRights()
    {
        return this.projectsRepository.getAllProjectsWithContributionRights();
    }

    /**
     * @return a collection of all projects that the current user can view.
     */
    public Collection<Project> getAllProjectsWithViewingRights()
    {
        return this.projectsRepository.getAllProjectsWithViewingRights();
    }

    /**
     * Receives a comma separated list of projects ids and returns a collection of ids of all templates associated with
     * them. For example, if t1,t2 are associated with p1 and t2,t3 are associated with p2, the collection returned for
     * the input "p1,p2" would contain t1,t2,t3.
     *
     * @param projects command separated project ids
     * @return collection of templates ids.
     */
    public Collection<Template> getTemplatesForProjects(String projects)
    {
        Set<Template> templates = new HashSet<Template>();
        for (String projectId : projects.split(",")) {
            Project project = this.getProjectById(projectId);
            if (project == null) {
                continue;
            }
            for (Template s : project.getTemplates()) {
                templates.add(s);
            }
        }
        return templates;
    }

    /**
     * Returns a collection of projects assigned to a patient.
     *
     * @param patientId id of patient to get a collection of projects from
     * @return a collection of Projects
     */
    public Collection<Project> getProjectsForPatient(String patientId)
    {
        Patient patient = this.patientsRepository.getPatientById(patientId);
        if (patient == null) {
            return null;
        }
        return this.ptBinder.getProjectsForPatient(patient);
    }

    /**
     * Assigns projects to a patient.
     *
     * @param projects colon-separated list of projects to assign
     * @param patientId id of patient
     */
    public void setProjectsForPatient(String projects, String patientId)
    {
        Patient patient = this.patientsRepository.getPatientById(patientId);
        this.ptBinder.setProjectsForPatient(projects, patient);
    }

    /**
     * Returns the template assigned to a patient.
     *
     * @param patientId id of patient to get the template from
     * @return template
     */
    public Template getTemplateForPatient(String patientId)
    {
        Patient patient = this.patientsRepository.getPatientById(patientId);
        if (patient == null) {
            return null;
        }
        return this.ptBinder.getTempalteForPatient(patient);
    }

    /**
     * Assigns a template to a patient.
     *
     * @param templateId id of template to assign
     * @param patientId id of patient
     */
    public void setTemplateForPatient(String templateId, String patientId)
    {
        Patient patient = this.patientsRepository.getPatientById(patientId);
        this.ptBinder.setTemplateForPatient(templateId, patient);
    }
}
