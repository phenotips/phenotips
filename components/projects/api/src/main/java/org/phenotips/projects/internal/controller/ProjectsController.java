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
package org.phenotips.projects.internal.controller;

import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.projects.data.Project;
import org.phenotips.projects.data.ProjectRepository;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Handles the patient's projects.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("projects")
@Singleton
public class ProjectsController implements PatientDataController<Project>
{
    private static final String DATA_NAME = "projects";

    @Inject
    private Logger logger;

    @Inject
    @Named("Project")
    private ProjectRepository projectRepository;

    @Inject
    @Named("Project:Patient")
    private PrimaryEntityGroupManager<Project, Patient> patientsInProject;

    @Override
    public PatientData<Project> load(Patient patient)
    {
        Collection<Project> projectsCollection = this.patientsInProject.getGroupsForMember(patient);
        List<Project> projects = new LinkedList<>(projectsCollection);
        return new IndexedPatientData<>(DATA_NAME, projects);
    }

    @Override
    public void save(Patient patient, DocumentModelBridge document)
    {
        // No need to do anything. Entities implementation saves projects.
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(DATA_NAME)) {
            return;
        }

        PatientData<Project> data = patient.getData(DATA_NAME);
        if (data != null && data.size() > 0) {
            JSONArray projectsArray = new JSONArray();
            for (Project project : data) {
                String name = project.getName();
                projectsArray.put(name);
            }

            json.put(DATA_NAME, projectsArray);
        }
    }

    @Override
    public PatientData<Project> readJSON(JSONObject json)
    {
        if (!json.has(DATA_NAME)) {
            // no supported data in provided JSON
            return null;
        }

        JSONArray projectsArray = json.getJSONArray(DATA_NAME);

        // Translating project names to full names (i.e. Project1 -> xwiki:Projects.project1)
        List<Project> projects = new ArrayList<>(projectsArray.length());
        for (Object o : projectsArray) {
            String projectName = (String) o;
            Project p = this.projectRepository.get(projectName);
            if (p == null) {
                this.logger.error("Cannot read project {} from JSON.", projectName);
            } else {
                projects.add(p);
            }
        }

        return new IndexedPatientData<>(DATA_NAME, projects);
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }
}
