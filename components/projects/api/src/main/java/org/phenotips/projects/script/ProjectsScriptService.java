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
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.internal.DefaultCollaborator;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.PrimaryEntityProperty;
import org.phenotips.projects.data.Project;
import org.phenotips.projects.data.ProjectRepository;
import org.phenotips.templates.data.Template;
import org.phenotips.templates.data.TemplateRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @version $Id$
 */
@Component
@Named("projects")
@Singleton
public class ProjectsScriptService implements ScriptService
{
    @Inject
    @Named("Project")
    private ProjectRepository projectRepository;

    @Inject
    @Named("Template")
    private TemplateRepository templateRepository;

    @Inject
    private PatientRepository patientsRepository;

    @Inject
    @Named("Project:Template")
    private PrimaryEntityGroupManager<Project, Template> templatesInProject;

    @Inject
    @Named("Project:Patient")
    private PrimaryEntityGroupManager<Project, Patient> patientsInProject;

    @Inject
    @Named("Patient:Template")
    private PrimaryEntityProperty<Patient, Template> templateInPatient;

    @Inject
    @Named("secure")
    private PermissionsManager manager;

    @Inject
    private DocumentReferenceResolver<String> stringResolver;

    /**
     * Returns a project by an id.
     *
     * @param projectId id of the project to return
     * @return a project object
     */
    public Project getProjectById(String projectId)
    {
        return this.projectRepository.get(projectId);
    }

    /**
     * @return a list of all projects that the current user can contribute to.
     */
    public List<Project> getProjectsCurrentUserCanContributeTo()
    {
        List<Project> projects = new ArrayList<Project>();
        projects.addAll(this.projectRepository.getProjectsCurrentUserCanContributeTo());
        Collections.sort(projects);
        return projects;
    }

    /**
     * @return a list of all projects that the current user is a leader in.
     */
    public List<Project> getProjectsWithLeadingRights()
    {
        List<Project> projects = new ArrayList<Project>();
        projects.addAll(this.projectRepository.getProjectsWithLeadingRights());
        Collections.sort(projects);
        return projects;
    }

    /**
     * Receives a comma separated list of projects ids and returns a collection of ids of all templates associated with
     * them. For example, if t1,t2 are associated with p1 and t2,t3 are associated with p2, the collection returned for
     * the input "p1,p2" would contain t1,t2,t3.
     *
     * @param projectsString command separated project ids
     * @return collection of templates ids.
     */
    public Collection<Template> getTemplatesForProjects(String projectsString)
    {
        Collection<Project> projects = this.projectRepository.getFromString(projectsString);
        Set<Template> templates = new HashSet<>();
        for (Project project : projects) {
            templates.addAll(project.getTemplates());
        }

        List<Template> templatesList = new ArrayList<>(templates);
        Collections.sort(templatesList);
        return templatesList;
    }

    /**
     * Returns a list of projects assigned to a patient.
     *
     * @param patientId id of patient to get a collection of projects from
     * @return a list of Projects
     */
    public List<Project> getProjectsForPatient(String patientId)
    {
        List<Project> projects = new ArrayList<Project>();
        Patient patient = this.patientsRepository.get(patientId);
        if (patient == null) {
            return projects;
        }

        Collection<Project> projectsCollection = this.patientsInProject.getGroupsForMember(patient);
        projects.addAll(projectsCollection);
        Collections.sort(projects);
        return projects;
    }

    /**
     * Assigns projects to a patient.
     *
     * @param projectString comma separated list of projects to assign
     * @param patientId id of patient
     */
    public void setProjectsForPatient(String projectString, String patientId)
    {
        Collection<Project> projects = this.projectRepository.getFromString(projectString);
        Patient patient = this.patientsRepository.get(patientId);

        this.patientsInProject.removeFromAllGroups(patient);
        this.patientsInProject.addToAllGroups(patient, projects);
    }

    /**
     * Returns the template assigned to a patient.
     *
     * @param patientId id of patient to get the template from
     * @return template
     */
    public Template getTemplateForPatient(String patientId)
    {
        Patient patient = this.patientsRepository.get(patientId);
        if (patient == null) {
            return null;
        }
        return this.templateInPatient.get(patient);
    }

    /**
     * Assigns a template to a patient.
     *
     * @param templateId id of template to assign
     * @param patientId id of patient
     */
    public void setTemplateForPatient(String templateId, String patientId)
    {
        Patient patient = this.patientsRepository.get(patientId);
        Template template = this.templateRepository.get(templateId);
        this.templateInPatient.set(patient, template);
    }

    /**
     * Returns a condition for an HQL patients query that selects all patients that belong to any project in
     * the projects current user is a leader in.
     *
     * @param objectTable name of BaseObject in query
     * @return HQL condition
     */
    public String getProjectConditionForCurrentUser(String objectTable)
    {
        Collection<Project> projects = this.getProjectsWithLeadingRights();
        Collection<Patient> patients = new LinkedList<>();
        for (Project p : projects) {
            Collection<Patient> members = this.patientsInProject.getMembers(p);
            patients.addAll(members);
        }
        if (patients.size() == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder()
            .append(" ").append(objectTable).append(".name in (");

        String apostrophe = "'";
        boolean firstPatient = true;
        for (Patient p : patients) {
            if (firstPatient) {
                firstPatient = false;
            } else {
                sb.append(",");
            }
            sb.append(apostrophe).append(p.getId()).append(apostrophe);
        }
        sb.append(") ");
        return sb.toString();
    }

    /**
     * Receives a JSON object with collaborators and sets them in the given project.
     *
     * @param collaboratorJSONString JSON object containing information about collaborators. For example:
     *        {'collaborators':[{'userOrGroup':'admin', 'accessLevel':'leader'}]}
     * @param project to set collaborators for
     */
    public void setCollaboratorsInProject(String collaboratorJSONString, Project project)
    {
        JSONObject collaboratorsJSON = new JSONObject(collaboratorJSONString);
        JSONArray collaboratorArray = collaboratorsJSON.getJSONArray("collaborators");

        Collection<Collaborator> collaborators = new HashSet<>();
        for (Object o : collaboratorArray) {
            JSONObject collaboratorItem = (JSONObject) o;

            String userOrGroupString = collaboratorItem.getString("userOrGroup");
            EntityReference userOrGroup = this.stringResolver.resolve(userOrGroupString);

            String accessLevelName = collaboratorItem.getString("accessLevel");
            AccessLevel accessLevel = this.manager.resolveAccessLevel(accessLevelName);

            Collaborator c = new DefaultCollaborator(userOrGroup, accessLevel);
            collaborators.add(c);
        }

        project.setCollaborators(collaborators);
    }

    /**
     * Returns number of projects that a given template is associated with.
     *
     * @param template template
     * @return number of projects
     */
    public int getNumberOfProjectsForTemplate(Template template)
    {
        return this.getProjectsForTemplate(template).size();
    }

    /**
     * Returns a collection of projects that a given template is associated with.
     *
     * @param template template
     * @return a collection of projects
     */
    public Collection<Project> getProjectsForTemplate(Template template)
    {
        List<Project> projects = new ArrayList<>(this.templatesInProject.getGroupsForMember(template));
        Collections.sort(projects);
        return projects;
    }

    /**
     * Returns the number of patients who have a given template assigned to them.
     *
     * @param template template
     * @return number of patient who have a given template assigned to them
     */
    public int getNumberOfPatientsForTemplate(Template template)
    {
        return this.templateInPatient.getGroupsForProperty(template).size();
    }
}
