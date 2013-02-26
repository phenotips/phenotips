/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package edu.toronto.cs.phenotips.tools;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.api.Document;

import edu.toronto.cs.phenotips.solr.HPOScriptService;

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

    private static final String DOCUMENT_KEY = "pdt.document";

    private static final String MESSAGES_KEY = "pdt.messages";

    @Inject
    private Execution execution;

    @Inject
    @Named("hpo")
    private ScriptService ontologyService;

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
        this.execution.getContext().setProperty(DOCUMENT_KEY, document);
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

    public String display(Collection<Map<String, ? >> template)
    {
        return new PropertyDisplayer(template, getFormData(), (HPOScriptService) this.ontologyService).display();
    }

    public void clear()
    {
        this.execution.getContext().removeProperty(CONTEXT_KEY);
        this.execution.getContext().removeProperty(DOCUMENT_KEY);
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
}
