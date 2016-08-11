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
package org.phenotips.templates.data;

import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.component.annotation.Role;

/**
 * @version $Id$
 */
@Role
public interface TemplateRepository extends PrimaryEntityManager<Template>
{
    /**
     * Returns a JSON object with a list of templates, all with ids that fit a search criterion.
     *
     * @param input the beginning of the template id, must not be null. To get all templates use {@code getAll()}.
     * @param resultsLimit maximal length of list, non-negative number
     * @return JSON object with a list of templates
     */
    String searchTemplates(String input, int resultsLimit);
}
