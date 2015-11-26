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

@Component(roles = { PatientRecordInitializer.class })
@Named("projectAndTemplate")
@Singleton
public class ProjectAndTemplateAssignmentInitializer implements PatientRecordInitializer
{
    /** The XClass used to store collaborators in the patient record. */
    private EntityReference PROJECT_BINDING_REFERENCE = new EntityReference("ProjectBindingClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    private String PROJECT_BINDING_FIELD = "projectReference";

    private static final String PROJECT_PREFIX = "PhenoTips.ProjectBindingClass_";

    private static final String STUDY_PREFIX = "PhenoTips.StudyBindingClass_";

    private static final String PROJECT_SELECTED = "on";

    @Inject
    private Execution execution;

    @Inject
    DocumentAccessBridge bridge;

    @Inject
    Logger logger;

    @Override
    public void initialize(Patient patient)
    {
        List<String> projectsToAssign = new ArrayList<String>();

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
        for (String key : parameterMap.keySet())
        {
            String[] values = parameterMap.get(key);

            if (key.startsWith(PROJECT_PREFIX)) {
                if (PROJECT_SELECTED.equals(values[0])) {
                    String projectId = key.substring(key.indexOf(PROJECT_PREFIX) + PROJECT_PREFIX.length());
                    Project p = new DefaultProject(projectId);
                    projectsToAssign.add(p.getFullName());
                }
            }

            if (key.startsWith(STUDY_PREFIX)) {
                if (PROJECT_SELECTED.equals(values[0])) {
                    String studyId = key.substring(key.indexOf(STUDY_PREFIX) + STUDY_PREFIX.length());
                    // TODO
                }
            }
        }

        try {
            String projects = StringUtils.join(projectsToAssign, ";");
            BaseObject projectBindingObject = patientDoc.newXObject(PROJECT_BINDING_REFERENCE, xContext);
            projectBindingObject.setStringValue(PROJECT_BINDING_FIELD, projects);
        } catch (XWikiException e) {
            this.logger.error("Failed to bind a project to patient. Patient: {}", patientRef.getName(), e.getMessage());
        }
    }

    private XWikiContext getXContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
