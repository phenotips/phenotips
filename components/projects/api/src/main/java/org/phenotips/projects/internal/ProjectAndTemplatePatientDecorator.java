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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.entities.PrimaryEntityProperty;
import org.phenotips.entities.internal.AbstractPrimaryEntity;
import org.phenotips.entities.internal.AbstractPrimaryEntityProperty;
import org.phenotips.projects.data.Project;
import org.phenotips.projects.data.ProjectRepository;
import org.phenotips.templates.data.Template;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds setters for projects and template. This is done here and not in Patient so that patient-data module is
 * not dependent on projects/templates modules.
 *
 * @version $Id$
 *
 */
public class ProjectAndTemplatePatientDecorator extends AbstractPrimaryEntity implements Patient
{
    protected Patient patient;

    private TemplateProperty templateProperty;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private class TemplateProperty extends AbstractPrimaryEntityProperty<Template>
        implements PrimaryEntityProperty<Template>
    {
        protected TemplateProperty(Patient patient)
        {
            super(patient.getDocument());
        }

        @Override
        public EntityReference getType()
        {
            return Patient.CLASS_REFERENCE;
        }

        @Override
        public void updateFromJSON(JSONObject json)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public EntityReference getMemberType()
        {
            return Template.CLASS_REFERENCE;
        }
    }

    /**
     * Basic constructor.
     *
     * @param patient to decorate
     */
    public ProjectAndTemplatePatientDecorator(Patient patient)
    {
        super(patient.getDocument());

        this.patient = patient;
        templateProperty = new TemplateProperty(patient);
    }

    /**
     * @return a collection of projects the patient is assigned to.
     */
    public Collection<Project> getProjects()
    {
        ProjectRepository projectRepository = this.getProjectRepository();
        return projectRepository.getGroupsForEntity(patient);
    }

    /**
     * Assign patient to projects.
     *
     * @param projects to assign the patient to
     */
    public void setProjects(Collection<Project> projects)
    {
        ProjectRepository projectRepository = this.getProjectRepository();
        for (Project p : projectRepository.getGroupsForEntity(patient)) {
            p.removeMember(this);
        }
        for (Project p : projects) {
            p.addMember(this);
        }
    }

    /**
     * Sets template for patient.
     *
     * @param template to set for patient
     */
    public void setTemplate(Template template)
    {
        if (template == null) {
            this.templateProperty.remove();
        } else {
            this.templateProperty.set(template);
        }
    }

    /**
     * @return Template that is assigned to the patient, null if not template is assigned.
     */
    public Template getTemplate()
    {
        return this.templateProperty.get();
    }

    private ProjectRepository getProjectRepository()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(ProjectRepository.class, "Project");
        } catch (Exception ex) {
            this.logger.error("Unexpected exception while getting projectRepository: {}", ex.getMessage());
        }
        return null;

    }

    // Methods delegated to inner patient

    @Override
    public EntityReference getType()
    {
        return this.patient.getType();
    }

    @Override
    public DocumentReference getDocument()
    {
        return this.patient.getDocument();
    }

    @Override
    public String getId()
    {
        return this.patient.getId();
    }

    @Override
    public String getName()
    {
        return this.patient.getName();
    }

    @Override
    public String getDescription()
    {
        return this.patient.getDescription();
    }

    @Override
    public String getExternalId()
    {
        return this.patient.getExternalId();
    }

    @Override
    public DocumentReference getReporter()
    {
        return this.patient.getReporter();
    }

    @Override
    public Set<? extends Feature> getFeatures()
    {
        return this.patient.getFeatures();
    }

    @Override
    public Set<? extends Disorder> getDisorders()
    {
        return this.patient.getDisorders();
    }

    @Override
    public <T> PatientData<T> getData(String name)
    {
        return this.patient.getData(name);
    }

    @Override
    public JSONObject toJSON()
    {
        return this.patient.toJSON();
    }

    @Override
    public JSONObject toJSON(Collection<String> selectedFields)
    {
        return this.patient.toJSON(selectedFields);
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        this.patient.updateFromJSON(json);
    }
}
