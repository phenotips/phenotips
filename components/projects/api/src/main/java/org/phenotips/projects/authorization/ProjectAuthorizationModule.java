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
package org.phenotips.projects.authorization;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.projects.access.ContributorAccessLevel;
import org.phenotips.projects.access.LeaderAccessLevel;
import org.phenotips.projects.data.Project;
import org.phenotips.projects.internal.ProjectAndTemplateBinder;
import org.phenotips.projects.internal.ProjectsRepository;
import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */
@Component
@Named("project")
@Singleton
public class ProjectAuthorizationModule implements AuthorizationModule
{
    @Inject
    private ProjectAndTemplateBinder ptBinder;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private ProjectsRepository projectsRepository;

    @Inject
    @Named("leader")
    private AccessLevel leaderAccessLevel;

    @Override
    public int getPriority()
    {
        return 105;
    }

    @Override
    public Boolean hasAccess(User user, Right access, EntityReference document)
    {
        String documentName = document.getName();

        Patient patient = this.patientRepository.get(documentName);
        if (patient != null) {
            return this.hasAccess(user, access, patient);
        }

        Project project = this.projectsRepository.getProjectById(documentName);
        if (project != null) {
            return this.hasAccess(user, access, project);
        }

        return null;
    }

    private Boolean hasAccess(User user, Right access, Patient patient)
    {
        Collection<Project> projects = this.ptBinder.getProjectsForPatient(patient);
        for (Project project : projects) {

            Collection<Collaborator> collaborators = project.getCollaborators();
            for (Collaborator collaborator : collaborators) {
                if (collaborator.isUserIncluded(user)
                    && this.leaderAccessLevel.equals(collaborator.getAccessLevel())) {
                    return true;
                }
            }
        }
        return null;
    }

    private Boolean hasAccess(User user, Right access, Project project)
    {

        if (project.isProjectOpenForContribution() && access.isReadOnly()) {
            return true;
        }

        Collection<Collaborator> collaborators = project.getCollaborators();
        for (Collaborator collaborator : collaborators) {
            if (!collaborator.isUserIncluded(user)) {
                continue;
            }

            AccessLevel accessLevel = collaborator.getAccessLevel();
            if (accessLevel instanceof LeaderAccessLevel) {
                return true;
            }

            if (accessLevel instanceof ContributorAccessLevel && access.isReadOnly()) {
                return true;
            }
        }

        return null;
    }
}
