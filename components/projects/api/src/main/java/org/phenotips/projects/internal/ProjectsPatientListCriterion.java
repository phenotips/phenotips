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
import org.phenotips.data.PatientListCriterion;
import org.phenotips.data.internal.AbstractPatientListCriterion;
import org.phenotips.projects.data.Project;
import org.phenotips.projects.data.ProjectRepository;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

/**
 * Returns all patients who are assigned to projects where the current user is a leader.
 *
 * @version $Id$
 */
public class ProjectsPatientListCriterion extends AbstractPatientListCriterion implements PatientListCriterion
{
    @Inject
    private ProjectRepository projectRepository;

    @Override
    public List<Patient> getAddList()
    {
        List<Patient> patients = new LinkedList<>();

        Collection<Project> projects = this.projectRepository.getProjectsWithLeadingRights();
        for (Project p : projects) {
            patients.addAll(p.getPatients());
        }

        return patients;
    }

}
