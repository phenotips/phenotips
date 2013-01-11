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
package edu.toronto.cs.cidb.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.api.Document;

import edu.toronto.cs.cidb.solr.HPOScriptService;

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
    private static final String DOCUMENT_KEY = "pdt.document";

    private static final String YES_SELECTION_MARKER = "pdt.yes_";

    private static final String NO_SELECTION_MARKER = "pdt.no_";

    private static final String FIELD_NAME_KEY = "pdt.fieldName";

    private static final String PROPERTY_NAME_KEY = "pdt.propertyName";

    private static final String MODE_KEY = "pdt.mode";

    private static final String SELECTED_VALUES_KEY = "pdt.selectedValues";

    private static final String MESSAGES_KEY = "pdt.messages";

    @Inject
    private Execution execution;

    @Inject
    @Named("hpo")
    private ScriptService ontologyService;

    public void use(String prefix, String name)
    {
        useProperty(YES_SELECTION_MARKER, prefix, name);
    }

    public void use(String prefix, String yName, String nName)
    {
        useProperty(YES_SELECTION_MARKER, prefix, yName);
        useProperty(NO_SELECTION_MARKER, prefix, nName);
    }

    protected void useProperty(String type, String prefix, String name)
    {
        this.execution.getContext().setProperty(type + FIELD_NAME_KEY, prefix + name);
        this.execution.getContext().setProperty(type + PROPERTY_NAME_KEY, name);
    }

    public void setDocument(Document document)
    {
        this.execution.getContext().setProperty(DOCUMENT_KEY, document);
    }

    public void setSelectedValues(Collection<String> values)
    {
        setSelectedValues(YES_SELECTION_MARKER, values);
    }

    public void setSelectedValues(Collection<String> yValues, Collection<String> nValues)
    {
        setSelectedValues(YES_SELECTION_MARKER, yValues);
        setSelectedValues(NO_SELECTION_MARKER, nValues);
    }

    protected void setSelectedValues(String type, Collection<String> values)
    {
        Set<String> selectedValues = new HashSet<String>();
        if (values != null) {
            selectedValues.addAll(values);
        }
        this.execution.getContext().setProperty(type + SELECTED_VALUES_KEY, selectedValues);
    }

    public void setMode(String mode)
    {
        this.execution.getContext().setProperty(MODE_KEY, DisplayMode.get(mode));
    }

    public void setMessageMap(Map<String, String> messages)
    {
        Map<String, String> messageMap = new LinkedHashMap<String, String>();
        messageMap.putAll(messages);
        this.execution.getContext().setProperty(MESSAGES_KEY, messageMap);
    }

    public String display(Collection<Map<String, ? >> template)
    {
        return new PropertyDisplayer(template, this.getPropertyName(), (HPOScriptService) this.ontologyService,
            getFieldName(YES_SELECTION_MARKER), getFieldName(NO_SELECTION_MARKER),
            getSelectedValues(YES_SELECTION_MARKER), getSelectedValues(NO_SELECTION_MARKER)).display(getMode());
    }

    public void clear()
    {
        this.execution.getContext().removeProperty(DOCUMENT_KEY);
        this.execution.getContext().removeProperty(MODE_KEY);
        this.execution.getContext().removeProperty(YES_SELECTION_MARKER + FIELD_NAME_KEY);
        this.execution.getContext().removeProperty(NO_SELECTION_MARKER + FIELD_NAME_KEY);
        this.execution.getContext().removeProperty(YES_SELECTION_MARKER + PROPERTY_NAME_KEY);
        this.execution.getContext().removeProperty(NO_SELECTION_MARKER + PROPERTY_NAME_KEY);
        this.execution.getContext().removeProperty(YES_SELECTION_MARKER + SELECTED_VALUES_KEY);
        this.execution.getContext().removeProperty(NO_SELECTION_MARKER + SELECTED_VALUES_KEY);
        if (this.execution.getContext().hasProperty(MESSAGES_KEY)) {
            this.execution.getContext().removeProperty(MESSAGES_KEY);
        }
    }

    private Set<String> getSelectedValues(String type)
    {
        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) this.execution.getContext().getProperty(type + SELECTED_VALUES_KEY);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    private String getFieldName(String type)
    {
        return (String) this.execution.getContext().getProperty(type + FIELD_NAME_KEY);
    }

    private DisplayMode getMode()
    {
        return (DisplayMode) this.execution.getContext().getProperty(MODE_KEY);
    }

    private String getPropertyName()
    {
        return getPropertyName(YES_SELECTION_MARKER);
    }

    private String getPropertyName(String type)
    {
        return (String) this.execution.getContext().getProperty(type + PROPERTY_NAME_KEY);
    }
}
