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
package org.phenotips.templates.script;

import org.phenotips.templates.data.Template;
import org.phenotips.templates.internal.DefaultTemplate;
import org.phenotips.templates.internal.TemplatesRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */

@Component
@Named("templates")
@Singleton
public class TemplatesScriptService implements ScriptService
{
    @Inject
    private TemplatesRepository templatesRepository;

    /**
     * Returns a JSON object with a list of templates, all with ids that fit a search criterion.
     *
     * @param input the beginning of the template id
     * @param resultsLimit maximal length of list
     * @return JSON object with a list of templates
     */
    public String searchTemplates(String input, int resultsLimit)
    {
        return templatesRepository.searchTemplates(input, resultsLimit);
    }

    /**
     * Returns a collection of templates that are available for the user. The
     * list is compiled based on the system property of templates visibility. If
     * templates are unrestricted, all templates will be returned. If the
     * templates are available based on group visibility, then only templates
     * for which the current user has permission will be returned.
     *
     * @return a collection of templates
     */
    public Collection<Template> getAllTemplatesForUser()
    {
        return templatesRepository.getAllTemplatesForUser();
    }

    /**
     * @param templateId id of template to get
     * @return a template
     */
    public Template getTemplateById(String templateId)
    {
        return new DefaultTemplate(templateId);
    }
}
