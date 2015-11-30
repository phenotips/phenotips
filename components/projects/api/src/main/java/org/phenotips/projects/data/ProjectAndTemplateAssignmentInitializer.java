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
package org.phenotips.projects.data;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRecordInitializer;
import org.phenotips.projects.internal.DefaultProject;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
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
    private static final String PROJECT_BINDING_FIELD = "projectReference";

    private static final String TEMPLATE_BINDING_FIELD = "templateReference";

    private static final String PROJECTS_SELECTED_KEY = "projectsSelected";

    private static final String TEMPLATE_SELECTED_KEY = "templateSelected";

    /** The XClass used to store collaborators in the patient record. */
    private EntityReference projectBindingReference = new EntityReference("ProjectBindingClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The XClass used to store collaborators in the patient record. */
    private EntityReference templateBindingReference = new EntityReference("TemplateBindingClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    @Inject
    private Execution execution;

    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private Logger logger;

    @Override
    public void initialize(Patient patient)
    {
        DocumentReference patientRef = patient.getDocument();
        XWikiDocument patientDoc;
        try {
            patientDoc = (XWikiDocument) this.bridge.getDocument(patientRef);
        } catch (Exception e) {
            this.logger.error("Could not read patient document for patient {}", patientRef.getName(), e.getMessage());
            return;
        }

        XWikiContext xContext = getXContext();
        XWikiRequest request = xContext.getRequest();
        Map<String, String[]> parameterMap = request.getParameterMap();

        // Projects selected
        String[] projectsSelectedValue = parameterMap.get(PROJECTS_SELECTED_KEY);
        if (projectsSelectedValue != null && projectsSelectedValue.length == 1) {
            this.assignProjects(projectsSelectedValue[0], patientDoc);
        }

        // Template selected
        String[] templateSelected = parameterMap.get(TEMPLATE_SELECTED_KEY);
        if (templateSelected != null && templateSelected.length == 1) {
            this.assignTemplate(templateSelected[0], patientDoc);
        }
    }

    private void assignProjects(String projectsSelected, XWikiDocument patientDoc)
    {
        List<String> projectsList = new ArrayList<String>();
        for (String projectId : projectsSelected.split(",")) {
            Project p = new DefaultProject(projectId);
            projectsList.add(p.getFullName());
        }
        try {
            XWikiContext xContext = getXContext();
            String projects = StringUtils.join(projectsList, ";");
            BaseObject projectBindingObject = patientDoc.newXObject(projectBindingReference, xContext);
            projectBindingObject.setStringValue(PROJECT_BINDING_FIELD, projects);
        } catch (XWikiException e) {
            this.logger.error("Failed to bind projects to patient. Patient: {}",
                patientDoc.getDocumentReference().getName(), e.getMessage());
        }
    }

    private void assignTemplate(String templateSelected, XWikiDocument patientDoc)
    {
        try {
            XWikiContext xContext = getXContext();
            BaseObject templateBindingObject = patientDoc.newXObject(templateBindingReference, xContext);
            templateBindingObject.setStringValue(TEMPLATE_BINDING_FIELD, templateSelected);
        } catch (XWikiException e) {
            this.logger.error("Failed to bind a template to patient. Patient: {}",
                patientDoc.getDocumentReference().getName(), e.getMessage());
        }
    }

    private XWikiContext getXContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
