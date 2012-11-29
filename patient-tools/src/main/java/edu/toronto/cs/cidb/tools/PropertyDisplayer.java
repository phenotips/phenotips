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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.xwiki.script.service.ScriptService;

import edu.toronto.cs.cidb.solr.SolrScriptService;

public class PropertyDisplayer
{
    private static final String TYPE_KEY = "type";

    private static final String GROUP_TYPE_KEY = "group_type";

    private static final String ID_KEY = "id";

    private static final String TITLE_KEY = "title";

    private static final String CATEGORIES_KEY = "categories";

    private static final String DATA_KEY = "data";

    private static final String ITEM_TYPE_SECTION = "section";

    private static final String ITEM_TYPE_SUBSECTION = "subsection";

    private static final String ITEM_TYPE_FIELD = "field";

    private static final String INDEXED_NAME_KEY = "name";

    private static final String INDEXED_CATEGORY_KEY = "term_category";

    private static final String DEFAULT_SECTION_TITLE = "Other";

    private static final String INDEXED_PARENT_KEY = "is_a";

    protected ScriptService ontologyService;

    protected final String[] fieldNames;

    protected final String propertyName;

    private List<FormSection> sections = new LinkedList<FormSection>();

    PropertyDisplayer(Collection<Map<String, ? >> template, String propertyName, ScriptService ontologyService,
        String fieldName, Collection<String> selected)
    {
        this(template, propertyName, ontologyService, fieldName, null, selected, null);
    }

    PropertyDisplayer(Collection<Map<String, ? >> template, String propertyName, ScriptService ontologyService,
        String yesFieldName, String noFieldName, Collection<String> yesSelected, Collection<String> noSelected)
    {
        this.ontologyService = ontologyService;
        this.fieldNames = new String[2];
        this.fieldNames[0] = yesFieldName;
        this.fieldNames[1] = noFieldName;
        this.propertyName = propertyName;
        List<String> customYesSelected = new LinkedList<String>();
        customYesSelected.addAll(yesSelected);
        List<String> customNoSelected = new LinkedList<String>();
        if (noFieldName != null) {
            customNoSelected.addAll(noSelected);
        }
        for (Map<String, ? > sectionTemplate : template) {
            if (isSection(sectionTemplate)) {
                this.sections.add(generateSection(sectionTemplate, customYesSelected, customNoSelected));
            }
        }
        Map<String, List<String>> yCustomCategories = new HashMap<String, List<String>>();
        Map<String, List<String>> nCustomCategories = new HashMap<String, List<String>>();
        for (String value : customYesSelected) {
            yCustomCategories.put(value, this.getCategoriesFromOntology(value));
        }
        for (String value : customNoSelected) {
            nCustomCategories.put(value, this.getCategoriesFromOntology(value));
        }
        for (FormSection section : this.sections) {
            List<String> yCustomFieldIDs = this.assignCustomFields(section, yCustomCategories);
            List<String> nCustomFieldIDs = this.assignCustomFields(section, nCustomCategories);
            for (String val : yCustomFieldIDs) {
                section.addCustomElement(this.generateField(val, null, false, true, false));
                yCustomCategories.remove(val);
            }
            for (String val : nCustomFieldIDs) {
                section.addCustomElement(this.generateField(val, null, false, false, true));
                nCustomCategories.remove(val);
            }
        }
    }

    public String display(DisplayMode mode)
    {
        StringBuilder str = new StringBuilder();
        for (FormSection section : this.sections) {
            str.append(section.display(mode, this.fieldNames));
        }
        return str.toString();
    }

    private boolean isSection(Map<String, ? > item)
    {
        return ITEM_TYPE_SECTION.equals(item.get(TYPE_KEY))
            && Collection.class.isInstance(item.get(CATEGORIES_KEY))
            && String.class.isInstance(item.get(TITLE_KEY))
            && Collection.class.isInstance(item.get(DATA_KEY));
    }

    private boolean isSubsection(Map<String, ? > item)
    {
        return ITEM_TYPE_SUBSECTION.equals(item.get(TYPE_KEY))
            && String.class.isInstance(item.get(TITLE_KEY))
            && Collection.class.isInstance(item.get(DATA_KEY));
    }

    private boolean isField(Map<String, ? > item)
    {
        return item.get(TYPE_KEY) == null
            || ITEM_TYPE_FIELD.equals(item.get(TYPE_KEY))
            && item.get(ID_KEY) != null
            && String.class.isAssignableFrom(item.get(ID_KEY).getClass());
    }

    @SuppressWarnings("unchecked")
    private FormSection generateSection(Map<String, ? > sectionTemplate,
        List<String> customYesSelected, List<String> customNoSelected)
    {
        String title = (String) sectionTemplate.get(TITLE_KEY);
        Collection<String> categories = (Collection<String>) sectionTemplate.get(CATEGORIES_KEY);
        FormSection section = new FormSection(title, this.propertyName, categories);
        generateData(section, sectionTemplate, customYesSelected, customNoSelected);
        return section;
    }

    private FormElement generateSubsection(Map<String, ? > subsectionTemplate,
        List<String> customYesSelected, List<String> customNoSelected)
    {
        String title = (String) subsectionTemplate.get(TITLE_KEY);
        String type = (String) subsectionTemplate.get(GROUP_TYPE_KEY);
        if (type == null) {
            type = "";
        }
        FormGroup subsection = new FormSubsection(title, type);
        generateData(subsection, subsectionTemplate, customYesSelected, customNoSelected);
        return subsection;
    }

    @SuppressWarnings("unchecked")
    private void generateData(FormGroup formGroup, Map<String, ? > groupTemplate, List<String> customYesSelected,
        List<String> customNoSelected)
    {
        Collection<Map<String, ? >> data = (Collection<Map<String, ? >>) groupTemplate.get(DATA_KEY);
        for (Map<String, ? > item : data) {
            if (isSubsection(item)) {
                formGroup.addElement(generateSubsection(item, customYesSelected, customNoSelected));
            } else if (isField(item)) {
                formGroup.addElement(generateField(item, customYesSelected, customNoSelected));
            }
        }
    }

    private FormElement generateField(Map<String, ? > fieldTemplate,
        List<String> customYesSelected, List<String> customNoSelected)
    {
        String id = (String) fieldTemplate.get(ID_KEY);
        boolean yesSelected = customYesSelected.remove(id);
        boolean noSelected = customNoSelected.remove(id);
        return this.generateField(id, (String) fieldTemplate.get(TITLE_KEY), yesSelected, noSelected);

    }

    private FormElement generateField(String id, String title,
        boolean expandable, boolean yesSelected, boolean noSelected)
    {
        String hint = getLabelFromOntology(id);
        return new FormField(id, StringUtils.defaultString(title, hint), hint, expandable, yesSelected, noSelected);

    }

    private FormElement generateField(String id, String title,
        boolean yesSelected, boolean noSelected)
    {
        return generateField(id, title, hasDescendantsInOntology(id), yesSelected, noSelected);
    }

    private List<String> assignCustomFields(FormSection section,
        Map<String, List<String>> customCategories)
    {
        List<String> assigned = new LinkedList<String>();
        if (section.getCategories().size() == 0) {
            assigned.addAll(customCategories.keySet());
        } else {
            for (String value : customCategories.keySet()) {
                List<String> categories = customCategories.get(value);
                for (String c : categories) {
                    if (section.getCategories().contains(c)) {
                        assigned.add(value);
                        break;
                    }
                }
            }
        }
        return assigned;
    }

    private String getLabelFromOntology(String id)
    {
        SolrDocument phObj = ((SolrScriptService) this.ontologyService).get(id);
        if (phObj != null) {
            return (String) phObj.get(INDEXED_NAME_KEY);
        }
        return id;
    }

    private boolean hasDescendantsInOntology(String id)
    {
        Map<String, String> params = new HashMap<String, String>();
        params.put(INDEXED_PARENT_KEY, id);
        return (((SolrScriptService) this.ontologyService).search(params, 1, 0).size() > 0);
    }

    @SuppressWarnings("unchecked")
    private List<String> getCategoriesFromOntology(String value)
    {
        SolrDocument termObj = ((SolrScriptService) this.ontologyService)
            .get(value);
        if (termObj != null
            && termObj.get(INDEXED_CATEGORY_KEY) != null
            && List.class.isAssignableFrom(termObj.get(INDEXED_CATEGORY_KEY).getClass())) {
            return (List<String>) termObj.get(INDEXED_CATEGORY_KEY);
        }
        return new LinkedList<String>();
    }
}
