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

import org.phenotips.projects.data.Project;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * @version $Id$
 */
@Component(roles = { ProjectsRepository.class })
@Singleton
public class ProjectsRepository
{
    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    /**
     * Returns a collection of EntityReferences of all projects.
     *
     * @return collection of EntityReferences to projects
     */
    public Collection<Project> getAllProjects()
    {
        StringBuilder querySb = new StringBuilder();
        querySb.append("from doc.object(PhenoTips.ProjectClass) as prj ");
        querySb.append("where doc.fullName <> 'PhenoTips.ProjectTemplate'");

        Query query = null;
        List<String> queryResults = null;
        try {
            query = this.qm.createQuery(querySb.toString(), Query.XWQL);
            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing projects query: [{}] ", e.getMessage());
        }
        Collections.sort(queryResults, String.CASE_INSENSITIVE_ORDER);

        List<Project> projects = new ArrayList<Project>(queryResults.size());
        for (String projectId : queryResults) {
            Project p = new DefaultProject(projectId);
            projects.add(p);
        }
        return projects;
    }
}
