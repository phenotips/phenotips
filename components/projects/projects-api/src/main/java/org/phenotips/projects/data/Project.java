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
import org.phenotips.data.permissions.Collaborator;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;

/**
 * @version $Id$
 */
public interface Project
{
    /** The XClass used for storing project data. */
    EntityReference CLASS_REFERENCE = new EntityReference("ProjectClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The default space where patient data is stored. */
    EntityReference DEFAULT_DATA_SPACE = new EntityReference("Projects", EntityType.SPACE);

    /**
     * Returns a collection project collaborators, both leaders and contributors.
     *
     * @return a collection of collaborators
     */
    Collection<Collaborator> getCollaborators();

    /**
     * Sets the list of project collaborators.
     *
     * @param contributors collection of contributors
     * @param leaders collection of contributors
     * @return true if successful
     */
    boolean setCollaborators(Collection<EntityReference> contributors, Collection<EntityReference> leaders);

    /**
     * Sets the list of project collaborators.
     *
     * @param collaborators collection of contributors
     * @return true if successful
     */
    boolean setCollaborators(Collection<Collaborator> collaborators);

    /**
     * Returns a collection templates available for the project.
     *
     * @return a collection of templates
     */
    Collection<EntityReference> getTemplates();

    /**
     * Sets the list of templates available for the project.
     *
     * @param templates collection of templates
     * @return true if successful
     */
    boolean setTemplates(Collection<EntityReference> templates);

}
