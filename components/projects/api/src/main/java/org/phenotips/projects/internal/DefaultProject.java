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
import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.internal.AbstractPrimaryEntity;
import org.phenotips.projects.data.Project;
import org.phenotips.projects.groupManagers.CollaboratorsInProjectManager;
import org.phenotips.projects.groupManagers.PatientsInProjectManager;
import org.phenotips.projects.groupManagers.TemplatesInProjectManager;
import org.phenotips.templates.data.Template;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
public class DefaultProject extends AbstractPrimaryEntity implements Project
{
    private static final String OPEN_FOR_CONTRIBUTION_KEY = "openProjectForContribution";

    private XWikiDocument projectObject;

    /** Logging helper object. */
    private Logger logger = LoggerFactory.getLogger(DefaultProject.class);

    /**
     * Basic constructor.
     *
     * @param projectObject xwiki object of project
     */
    public DefaultProject(XWikiDocument projectObject)
    {
        super(projectObject);

        this.projectObject = projectObject;
    }

    @Override
    public String getImage()
    {
        String avatarURL = "";
        try {
            DocumentAccessBridge documentAccessBridge =
                ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
            List<AttachmentReference> attachmentRefs =
                documentAccessBridge.getAttachmentReferences(this.getDocument());
            if (attachmentRefs.size() > 0) {
                avatarURL = documentAccessBridge.getAttachmentURL(attachmentRefs.get(0), true);
            } else {
                Provider<XWikiContext> xcontextProvider =
                    ComponentManagerRegistry.getContextComponentManager().getInstance(XWikiContext.TYPE_PROVIDER);
                XWikiContext context = xcontextProvider.get();
                XWiki xwiki = context.getWiki();
                avatarURL = xwiki.getSkinFile("icons/xwiki/noavatargroup.png", context);
            }
        } catch (Exception ex) {
            this.logger.error("Failed to access project data for ({})", this.getName(), ex.getMessage());
        }
        return avatarURL;
    }

    @Override
    public boolean addPatient(Patient patient)
    {
        return getPatientsInProjectManager().addMember(this, patient);
    }

    @Override
    public Collection<Patient> getPatients()
    {
        return getPatientsInProjectManager().getMembers(this);
    }

    @Override
    public int getNumberOfPatients()
    {
        return this.getPatients().size();
    }

    @Override
    public int getNumberOfCollaboratorsUsers()
    {
        Set<String> usersList = new HashSet<String>();
        for (Collaborator collaborator : this.getCollaborators()) {
            usersList.addAll(collaborator.getAllUserNames());
        }
        return usersList.size();
    }

    @Override
    public Collection<Collaborator> getCollaborators()
    {
        return this.getCollaboratorsInProjectManager().getMembers(this);
    }

    @Override
    public boolean setCollaborators(Collection<Collaborator> collaborators)
    {
        boolean success = this.getCollaboratorsInProjectManager().removeAllMembers(this);
        success = this.getCollaboratorsInProjectManager().addAllMembers(this, collaborators) && success;
        return success;
    }

    @Override
    public Collection<Template> getTemplates()
    {
        return this.getTemplatesInProjectManager().getMembers(this);
    }

    @Override
    public boolean setTemplates(Collection<String> templateIds)
    {
        boolean success = this.getTemplatesInProjectManager().removeAllMembers(this);
        return this.getTemplatesInProjectManager().addAllMembersById(this, templateIds) && success;
    }

    @Override
    public AccessLevel getCurrentUserAccessLevel()
    {
        AccessLevel highestAccessLevel = null;
        User currentUser = this.getUserManager().getCurrentUser();
        Collection<Collaborator> collaborators = this.getCollaborators();
        for (Collaborator c : collaborators) {
            if (c.isUserIncluded(currentUser)) {
                AccessLevel accessLevel = c.getAccessLevel();
                if (highestAccessLevel == null || accessLevel.compareTo(highestAccessLevel) >= 0) {
                    highestAccessLevel = accessLevel;
                }
            }
        }
        return highestAccessLevel;
    }

    @Override
    public boolean isProjectOpenForContribution()
    {
        BaseObject xObject = this.projectObject.getXObject(Project.CLASS_REFERENCE);
        int openIntValue = xObject.getIntValue(DefaultProject.OPEN_FOR_CONTRIBUTION_KEY);
        return openIntValue == 1;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DefaultProject)) {
            return false;
        }

        DefaultProject otherProject = (DefaultProject) obj;
        return this.getId().equals(otherProject.getId());
    }

    @Override
    public int hashCode()
    {
        return this.getId().hashCode();
    }

    @Override
    public String toString()
    {
        return getFullName();
    }

    @Override
    public int compareTo(Project other)
    {
        return this.getId().compareTo(other.getId());
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

    private PrimaryEntityGroupManager<Project, Patient> getPatientsInProjectManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(
                    PatientsInProjectManager.TYPE, "Project:Patient");
        } catch (ComponentLookupException e) {
            this.logger.error("Unexpected exception while getting patientsInProjectManager: {}", e.getMessage());
        }
        return null;
    }

    private PrimaryEntityGroupManager<Project, Template> getTemplatesInProjectManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(
                    TemplatesInProjectManager.TYPE, "Project:Template");
        } catch (ComponentLookupException e) {
            this.logger.error("Unexpected exception while getting templatesInProjectManager: {}", e.getMessage());
        }
        return null;
    }

    private PrimaryEntityGroupManager<Project, Collaborator> getCollaboratorsInProjectManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(
                    CollaboratorsInProjectManager.TYPE, "Project:Collaborator");
        } catch (ComponentLookupException e) {
            this.logger.error("Unexpected exception while getting collaboratorsInProjectManager: {}", e.getMessage());
        }
        return null;
    }

    private UserManager getUserManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(UserManager.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }
}
