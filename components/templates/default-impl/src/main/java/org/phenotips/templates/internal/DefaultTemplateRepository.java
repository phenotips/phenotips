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
package org.phenotips.templates.internal;

import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.internal.AbstractPrimaryEntityManager;
import org.phenotips.templates.data.Template;
import org.phenotips.templates.data.TemplateRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Iterator;

import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @version $Id$
 */
@Component(roles = { PrimaryEntityManager.class, TemplateRepository.class })
@Named("Template")
@Singleton
public class DefaultTemplateRepository extends AbstractPrimaryEntityManager<Template> implements TemplateRepository
{
    private static final String MATCHED_TEMPLATES = "matchedTemplates";

    @Override
    public EntityReference getDataSpace()
    {
        return Template.DEFAULT_DATA_SPACE;
    }

    @Override
    public String searchTemplates(String input, int resultsLimit)
    {
        JSONArray templatesArray = new JSONArray();
        Iterator<Template> templatesIterator = this.getAll();

        while (templatesIterator.hasNext() && templatesArray.length() < resultsLimit) {
            Template template = templatesIterator.next();
            if (!template.getName().startsWith(input)) {
                continue;
            }

            JSONObject templateJSON = new JSONObject();
            templateJSON.put("id", template.getId());
            templateJSON.put("textSummary", template.getName());
            templatesArray.put(templateJSON);
        }

        JSONObject result = new JSONObject();
        result.put(MATCHED_TEMPLATES, templatesArray);
        return result.toString();
    }

    @Override
    protected Class<? extends Template> getEntityClass()
    {
        return DefaultTemplate.class;
    }

    @Override
    protected DocumentReference getEntityXClassReference()
    {
        return this.referenceResolver.resolve(Template.CLASS_REFERENCE);
    }
}
