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
import org.phenotips.templates.data.Template;
import org.phenotips.templates.internal.DefaultTemplate;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
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

    @Inject
    private ProjectsRepository projectsRepository;

    /**
     * Assigns project(s) to a patient.
     *
     * @param projectsSelected project(s) to assign
     * @param patient patient to assign the template to
     */
    public void setProjectsForPatient(String projectsSelected, Patient patient)
    {
        List<String> projectsList = new ArrayList<String>();
        if (!StringUtils.isEmpty(projectsSelected)) {
            for (String projectId : projectsSelected.split(",")) {
                Project p = this.projectsRepository.getProjectById(projectId);
                projectsList.add(p.getFullName());
            }
        }
        String projects = StringUtils.join(projectsList, PROJECTS_SEPARATOR);

        setPropertyForPatient(patient, this.projectBindingReference, PROJECT_BINDING_FIELD, projects);
    }

    /**
     * Returns a list of projects assigned to a patient.
     *
     * @param patient to get a list of projects from
     * @return a list of Projects
     */
    public List<Project> getProjectsForPatient(Patient patient)
    {
        List<Project> projects = new ArrayList<Project>();
        XWikiDocument patientXDoc = this.getPatientXWikiDocument(patient);

        BaseObject projectBindingObject = patientXDoc.getXObject(projectBindingReference);
        if (projectBindingObject != null) {
            String projectsString = projectBindingObject.getStringValue(PROJECT_BINDING_FIELD);
            if (projectsString != null) {
                for (String projectId : projectsString.split(PROJECTS_SEPARATOR)) {
                    Project project = this.projectsRepository.getProjectById(projectId);
                    projects.add(project);
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
        setPropertyForPatient(patient, this.templateBindingReference, TEMPLATE_BINDING_FIELD, templateSelected);
    }

    /**
     * Returns the template assigned to a patient.
     *
     * @param patient to get the template from
     * @return template
     */
    public Template getTempalteForPatient(Patient patient)
    {
        Template template = null;
        XWikiDocument patientXDoc = this.getPatientXWikiDocument(patient);

        BaseObject templateBindingObject = patientXDoc.getXObject(templateBindingReference);
        if (templateBindingObject != null) {
            String templateId = templateBindingObject.getStringValue(TEMPLATE_BINDING_FIELD);
            if (templateId != null) {
                template = new DefaultTemplate(templateId);
            }
        }
        return template;

    }

    /*
     * For patient {@patient}, sets the field {@link bindingField} of xobject {@link bindingReference} to be {@link
     * value}. The functions handles either creation, update or removal of the xobject.
     */
    private void setPropertyForPatient(Patient patient,
        EntityReference bindingReference, String bindingField, String value)
    {
        XWikiContext xContext = this.contextProvider.get();
        XWikiDocument patientXDoc = this.getPatientXWikiDocument(patient);
        BaseObject bindingObject = patientXDoc.getXObject(bindingReference);

        if (StringUtils.isEmpty(value) && bindingObject != null) {
            patientXDoc.removeXObject(bindingObject);
        } else if (!StringUtils.isEmpty(value)) {
            if (bindingObject == null) {
                try {
                    bindingObject =
                        patientXDoc.newXObject(bindingReference, xContext);
                } catch (XWikiException e) {
                    this.logger.error("Failed to create a new xobject for binding {} for patient {}.",
                        patientXDoc.getDocumentReference().getName(), bindingReference.getName(), e.getMessage());
                }
            }
            bindingObject.setStringValue(bindingField, value);
        }

        try {
            String description = "Updated " + bindingReference.getName() + " binding";
            xContext.getWiki().saveDocument(patientXDoc, description, true, xContext);
        } catch (XWikiException e) {
            this.logger.error("Failed to save patient {}",
                patientXDoc.getDocumentReference().getName(), e.getMessage());
        }
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
