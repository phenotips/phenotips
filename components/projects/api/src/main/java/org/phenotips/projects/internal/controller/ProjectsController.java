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
import org.phenotips.projects.data.Project;
import org.phenotips.projects.internal.ProjectAndTemplateBinder;
import org.phenotips.projects.internal.ProjectsRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

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
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Execution execution;

    @Inject
    private ProjectAndTemplateBinder ptBinder;

    @Inject
    private ProjectsRepository projectRepository;

    @Override
    public PatientData<Project> load(Patient patient)
    {
        List<Project> projects = this.ptBinder.getProjectsForPatient(patient);
        if (projects.size() == 0) {
            return null;
        }

        return new IndexedPatientData<>(DATA_NAME, projects);
    }

    @Override
    public void save(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            PatientData<Project> projectsData = patient.getData(DATA_NAME);

            if (projectsData == null || projectsData.size() == 0) {
                return;
            }

            // Formatting for PatientAndTemplateBinder
            List<String> projectNames = new ArrayList<String>(projectsData.size());
            for (Project p : projectsData) {
                String fullName = p.getFullName();
                projectNames.add(fullName);
            }
            String joinedProjects = StringUtils.join(projectNames, ",");

            this.ptBinder.setProjectsForPatient(joinedProjects, patient);

            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            context.getWiki().saveDocument(doc, "Updated projects from JSON", true, context);
        } catch (Exception e) {
            this.logger.error("Failed to save patient projects: [{}]", e.getMessage());
        }
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
            Project p = this.projectRepository.getProjectById(projectName);
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
