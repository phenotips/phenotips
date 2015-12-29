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

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.projects.data.Project;
import org.phenotips.studies.data.Study;
import org.phenotips.studies.internal.DefaultStudy;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Binds template and projects to a patient and retrieves them.
 *
 * @version $Id$
 */
@Component(roles = { ProjectAndTemplateBinder.class })
@Singleton
public class ProjectAndTemplateBinder
{
    private static final String PROJECT_BINDING_FIELD = "projectReference";

    private static final String TEMPLATE_BINDING_FIELD = "templateReference";

    private static final String PROJECTS_SEPARATOR = ";";

    /** The XClass used to store collaborators in the patient record. */
    private EntityReference projectBindingReference = new EntityReference("ProjectBindingClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The XClass used to store collaborators in the patient record. */
    private EntityReference templateBindingReference = new EntityReference("TemplateBindingClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private Logger logger;

    /**
     * Assigns project(s) to a patient.
     *
     * @param projectsSelected project(s) to assign
     * @param patient patient to assign the template to
     */
    public void setProjectsForPatient(String projectsSelected, Patient patient)
    {
        if (StringUtils.isEmpty(projectsSelected)) {
            return;
        }

        XWikiDocument patientXDoc = this.getPatientXWikiDocument(patient);

        List<String> projectsList = new ArrayList<String>();
        for (String projectId : projectsSelected.split(",")) {
            Project p = new DefaultProject(projectId);
            projectsList.add(p.getFullName());
        }
        try {
            XWikiContext xContext = this.contextProvider.get();
            String projects = StringUtils.join(projectsList, PROJECTS_SEPARATOR);
            BaseObject projectBindingObject = patientXDoc.newXObject(projectBindingReference, xContext);
            projectBindingObject.setStringValue(PROJECT_BINDING_FIELD, projects);
        } catch (XWikiException e) {
            this.logger.error("Failed to bind projects to patient. Patient: {}",
                patientXDoc.getDocumentReference().getName(), e.getMessage());
        }
    }

    /**
     * Returns a collection of projects assigned to a patient.
     *
     * @param patient to get a collection of projects from
     * @return a collection of Projects
     */
    public Collection<Project> getProjectsForPatient(Patient patient)
    {
        List<Project> projects = new ArrayList<Project>();
        XWikiDocument patientXDoc = this.getPatientXWikiDocument(patient);

        BaseObject projectBindingObject = patientXDoc.getXObject(projectBindingReference);
        if (projectBindingObject != null) {
            String projectsString = projectBindingObject.getStringValue(PROJECT_BINDING_FIELD);
            if (projectsString != null) {
                for (String projectId : projectsString.split(PROJECTS_SEPARATOR)) {
                    projects.add(new DefaultProject(projectId));
                }
            }
        }
        return projects;
    }

    /**
     * Assigns a template to a patient.
     *
     * @param templateSelected template to assign
     * @param patient patient to assign the template to
     */
    public void setTemplateForPatient(String templateSelected, Patient patient)
    {
        if (StringUtils.isEmpty(templateSelected)) {
            return;
        }

        XWikiDocument patientXDoc = this.getPatientXWikiDocument(patient);

        try {
            XWikiContext xContext = this.contextProvider.get();
            BaseObject templateBindingObject = patientXDoc.newXObject(templateBindingReference, xContext);
            templateBindingObject.setStringValue(TEMPLATE_BINDING_FIELD, templateSelected);
        } catch (XWikiException e) {
            this.logger.error("Failed to bind a template to patient. Patient: {}",
                patientXDoc.getDocumentReference().getName(), e.getMessage());
        }
    }

    /**
     * Returns the template assigned to a patient.
     *
     * @param patient to get the template from
     * @return Study
     */
    public Study getTempalteForPatient(Patient patient)
    {
        Study study = null;
        XWikiDocument patientXDoc = this.getPatientXWikiDocument(patient);

        BaseObject templateBindingObject = patientXDoc.getXObject(templateBindingReference);
        if (templateBindingObject != null) {
            String studyId = templateBindingObject.getStringValue(TEMPLATE_BINDING_FIELD);
            if (studyId != null) {
                study = new DefaultStudy(studyId);
            }
        }
        return study;

    }

    private XWikiDocument getPatientXWikiDocument(Patient patient)
    {
        DocumentReference patientRef = patient.getDocument();
        XWikiDocument patientXDoc;
        try {
            patientXDoc = (XWikiDocument) this.bridge.getDocument(patientRef);
        } catch (Exception e) {
            this.logger.error("Could not read patient document for patient {}", patientRef.getName(), e.getMessage());
            return null;
        }
        return patientXDoc;
    }
}
