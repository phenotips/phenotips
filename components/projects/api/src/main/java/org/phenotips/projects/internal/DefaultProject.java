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
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.projects.data.Project;
import org.phenotips.templates.data.Template;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
public class DefaultProject implements Project
{
    private static final String OPEN_FOR_CONTRIBUTION_KEY = "openProjectForContribution";

    private static final String OPEN_FOR_VIEWING_KEY = "openProjectForViewing";

    private String projectId;

    private XWikiDocument projectObject;

    private DocumentReference projectReference;

    /**
     * Basic constructor.
     *
     * @param projectObject xwiki object of project
     */
    public DefaultProject(XWikiDocument projectObject) {
        this.projectObject = projectObject;
        this.projectReference = this.projectObject.getDocumentReference();
        this.projectId = this.projectReference.getName();
    }

    @Override
    public DocumentReference getReference()
    {
        return projectReference;
    }

    @Override
    public String getId() {
        return this.projectId;
    }

    @Override
    public String getName() {
        return this.projectReference.getName();
    }

    @Override
    public String getFullName() {
        return projectReference.toString();
    }

    @Override
    public String getDescription() {
        // TODO
        return "";
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
        return this.getDefaultProjectHelper().getCollaborators(projectObject);
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
    public boolean setCollaborators(Collection<EntityReference> contributors, Collection<EntityReference> leaders)
    {
        return this.getDefaultProjectHelper().setCollaborators(projectObject, contributors, leaders);
    }

    @Override
    public boolean setCollaborators(Collection<Collaborator> collaborators)
    {
        return this.getDefaultProjectHelper().setCollaborators(projectObject, collaborators);
    }

    @Override
    public Collection<Template> getTemplates()
    {
        return this.getDefaultProjectHelper().getTemplates(projectObject);
    }

    @Override
    public boolean setTemplates(Collection<EntityReference> templates)
    {
        return this.getDefaultProjectHelper().setTemplates(projectObject, templates);
    }

    @Override
    public boolean isProjectOpenForContribution() {
        BaseObject xObject = this.projectObject.getXObject(Project.CLASS_REFERENCE);
        int openIntValue = xObject.getIntValue(DefaultProject.OPEN_FOR_CONTRIBUTION_KEY);
        return openIntValue == 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DefaultProject)) {
            return false;
        }

        DefaultProject otherProject = (DefaultProject) obj;
        return this.projectId.equals(otherProject.projectId);
    }

    @Override
    public int hashCode() {
        return this.projectId.hashCode();
    }

    @Override
    public int getNumberOfPatients() {
        StringBuilder querySb = new StringBuilder();
        querySb.append(", BaseObject accessObj, StringProperty accessProp, BaseObject patientObj, LongProperty iid ");
        querySb.append("where patientObj.name = doc.fullName ");
        querySb.append("and doc.fullName <> 'PhenoTips.PatientTemplate' ");
        querySb.append("and patientObj.className = 'PhenoTips.PatientClass' ");
        querySb.append("and iid.id.id = patientObj.id and iid.id.name = 'identifier' and iid.value >= 0 ");
        querySb.append("and accessObj.name = doc.fullName and accessProp.id.id = accessObj.id ");

        Set<Project> projects = new HashSet<Project>();
        projects.add(this);
        querySb.append(" and ");
        querySb.append(this.getProjectsRepository().getProjectCondition("accessObj", "accessProp", projects));

        Query query = null;
        List<String> queryResults = null;
        try {
            query = this.getQueryManager().createQuery(querySb.toString(), Query.HQL);
            queryResults = query.execute();
        } catch (QueryException e) {
            this.getLogger().error("Error while performing projects query: [{}] ", e.getMessage());
        }

        return queryResults.size();
    }

    @Override
    public String toString()
    {
        return getFullName();
    }

    private Logger getLogger()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(Logger.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private QueryManager getQueryManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(QueryManager.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private UserManager getUserManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(UserManager.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private DefaultProjectHelper getDefaultProjectHelper()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(DefaultProjectHelper.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private ProjectsRepository getProjectsRepository()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(ProjectsRepository.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }
}
