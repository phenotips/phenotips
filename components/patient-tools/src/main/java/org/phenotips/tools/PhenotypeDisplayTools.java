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
package org.phenotips.tools;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.api.Document;

/**
 * Display lists of phenotype properties.
 *
 * @version $Id$
 */
@Component
@Named("phenotype")
@Singleton
public class PhenotypeDisplayTools implements ScriptService
{
    private static final String CONTEXT_KEY = "pdt.data";

    private static final String MESSAGES_KEY = "pdt.messages";

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    @Named("hpo")
    private Vocabulary ontologyService;

    public void use(String prefix, String name)
    {
        getFormData().setPositivePropertyName(name);
        getFormData().setPositiveFieldName(prefix + name);
    }

    public void use(String prefix, String yName, String nName)
    {
        getFormData().setPositivePropertyName(yName);
        getFormData().setPositiveFieldName(prefix + yName);
        getFormData().setNegativePropertyName(nName);
        getFormData().setNegativeFieldName(prefix + nName);
    }

    public void setDocument(Document document)
    {
        getFormData().setDocument(document);
    }

    public void setSelectedValues(Collection<String> values)
    {
        getFormData().setSelectedValues(values);
    }

    public void setSelectedValues(Collection<String> yValues, Collection<String> nValues)
    {
        getFormData().setSelectedValues(yValues);
        getFormData().setSelectedNegativeValues(nValues);
    }

    public void setCustomCategories(Map<String, List<String>> customCategories)
    {
        getFormData().setCustomCategories(customCategories);
    }

    public void setCustomCategories(Map<String, List<String>> customCategories,
        Map<String, List<String>> customNCategories)
    {
        getFormData().setCustomCategories(customCategories);
        getFormData().setCustomNegativeCategories(customNCategories);
    }

    public void setMode(String mode)
    {
        getFormData().setMode(DisplayMode.get(mode));
    }

    public void setMessageMap(Map<String, String> messages)
    {
        Map<String, String> messageMap = new LinkedHashMap<String, String>();
        messageMap.putAll(messages);
        this.execution.getContext().setProperty(MESSAGES_KEY, messageMap);
    }

    public String display(Collection<Map<String, ?>> template)
    {
        try {
            FormData formData = this.replaceOldTerms(this.getFormData());
            return new PropertyDisplayer(template, formData, this.ontologyService).display();
        } catch (Exception ex) {
            logger.error("An error has occurred while trying to display phenotypes. {}", ex.getMessage(), ex);
            return "";
        }
    }

    public void clear()
    {
        this.execution.getContext().removeProperty(CONTEXT_KEY);
        if (this.execution.getContext().hasProperty(MESSAGES_KEY)) {
            this.execution.getContext().removeProperty(MESSAGES_KEY);
        }
    }

    private FormData getFormData()
    {
        FormData data = (FormData) this.execution.getContext().getProperty(CONTEXT_KEY);
        if (data == null) {
            data = new FormData();
            this.execution.getContext().setProperty(CONTEXT_KEY, data);
        }

        return data;
    }

    private FormData replaceOldTerms(FormData data)
    {
        if (data.getMode() == DisplayMode.Edit) {
            List<String> correctIds = new LinkedList<String>();
            List<String> correctNegativeIds = new LinkedList<String>();
            if (data.getSelectedValues() != null && !data.getSelectedValues().isEmpty()) {
                for (String id : data.getSelectedValues()) {
                    if (StringUtils.isNotBlank(id)) {
                        VocabularyTerm properTerm = this.ontologyService.getTerm(id);
                        if (properTerm != null) {
                            correctIds.add(properTerm.getId());
                        } else {
                            correctIds.add(id);
                        }
                    }
                }
            }
            data.setSelectedValues(correctIds);
            if (data.getSelectedNegativeValues() != null && !data.getSelectedNegativeValues().isEmpty()) {
                for (String id : data.getSelectedNegativeValues()) {
                    if (StringUtils.isNotBlank(id)) {
                        VocabularyTerm properTerm = this.ontologyService.getTerm(id);
                        if (properTerm != null) {
                            correctNegativeIds.add(properTerm.getId());
                        } else {
                            correctNegativeIds.add(id);
                        }
                    }
                }
            }
            data.setSelectedNegativeValues(correctNegativeIds);
            return data;
        } else {
            return data;
        }
    }
}
