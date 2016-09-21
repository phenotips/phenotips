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
import org.phenotips.templates.data.TemplateRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
    @Named("Template")
    private TemplateRepository templateRepository;

    /**
     * Returns a JSON object with a list of templates, all with ids that fit a search criterion.
     *
     * @param input the beginning of the template id, must not be null. To get all templates use {@code getAll()}.
     * @param resultsLimit maximal length of list, non-negative number
     * @return JSON object with a list of templates
     */
    public String searchTemplates(String input, int resultsLimit)
    {
        return this.templateRepository.searchTemplates(input, resultsLimit);
    }

    /**
     * @return number of templates
     */
    public int getNumberOfTemplates()
    {
        // TODO: will change in AbstractPrimaryEntityManager

        int count = 0;
        Iterator<Template> iterator = this.templateRepository.getAll();
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    /**
     * Returns a sorted collection of templates. If {@link numberOfResults>0} the collection will contain not more than
     * that number of results. Otherwise, it will contain all templates.
     *
     * @param numberOfResults maximal number of results
     * @return a collection of templates
     */
    public Collection<Template> getAll(int numberOfResults)
    {
        Iterator<Template> iterator = this.templateRepository.getAll();
        List<Template> templates = new LinkedList<>();

        boolean moreTemplates = iterator.hasNext();
        int size = 0;
        while (moreTemplates) {
            templates.add(iterator.next());
            size++;

            if (size >= numberOfResults && numberOfResults > 0) {
                moreTemplates = false;
            }
            if (!iterator.hasNext()) {
                moreTemplates = false;
            }
        }

        Collections.sort(templates);
        return templates;
    }
}
