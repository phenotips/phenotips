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
import org.phenotips.entities.internal.AbstractContainerPrimaryEntityGroup;
import org.phenotips.projects.data.Project;
import org.phenotips.projects.data.ProjectRepository;
import org.phenotips.templates.data.Template;
import org.phenotips.templates.data.TemplateRepository;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;

import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * This is a representation of the property of a project as a container for groups. Since a project implementation can
 * only extend one class, it contains a member of type {@link TemplateInProjectGroup} to manage containing templates.
 *
 * See also {@link CollaboratorInProjectGroup}.
 *
 * @version $Id $
 */
public class TemplateInProjectGroup extends AbstractContainerPrimaryEntityGroup<Template>
{
    /**
     * public constructor.
     *
     * @param projectDocument project's document
     */
    public TemplateInProjectGroup(XWikiDocument projectDocument)
    {
        super(projectDocument);
    }

    /**
     * Replaces group of templates with new templates with ids in {@code templateIds}.
     *
     * @param templateIds ids of templates
     * @return true if successful
     */
    public boolean setMembers(Collection<String> templateIds)
    {
        Collection<Template> existingTemplates = this.getMembers();
        for (Template template : existingTemplates) {
            this.removeMember(template);
        }

        for (String id : templateIds) {
            Template template = this.getTemplateRepository().get(id);
            this.addMember(template);
        }

        return true;
    }

    @Override
    public EntityReference getMemberType()
    {
        return Template.CLASS_REFERENCE;
    }

    @Override
    public EntityReference getType()
    {
        return Project.CLASS_REFERENCE;
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the project that this class represents
     */
    public Project getProject()
    {
        return this.getProjectRepository().get(this.document.getDocumentReference());
    }

    private TemplateRepository getTemplateRepository()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(TemplateRepository.class,
                    "Template");
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private ProjectRepository getProjectRepository()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(ProjectRepository.class,
                    "Project");
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }
}
