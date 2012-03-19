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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.api.Document;

import edu.toronto.cs.cidb.solr.SolrScriptService;

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
    public static final String OTHER_FIELD_MARKER = "_other";

    public static final String RESTRICTION_FIELD_MARKER = "_category";

    public static final String CUSTOM_MAPPING_FIELD_MARKER = "_mapping";

    public static final String IMAGE_FIELD_MARKER = "_image";

    public static final String CUSTOM_MAPPING_CLASS = "ClinicalInformationCode.CategoryMappingClass";

    public static final String CUSTOM_MAPPING_CATEGORY_ID_FIELD = "category_id";

    public static final String CUSTOM_MAPPING_VALUES_FIELD = "values";

    public static final String CUSTOM_MAPPING_IMAGES_FIELD = "images";

    public static final String EMPTY_SECTION_MESSAGE = "None specified";

    private static final String UNDERSCORE = "_";

    private static final String DOCUMENT_KEY = "pdt.document";

    private static final String NAME_KEY = "pdt.name";

    private static final String PROPERTY_NAME_KEY = "pdt.propertyName";

    private static final String MODE_KEY = "pdt.mode";

    private static final String SELECTED_VALUES_KEY = "pdt.selectedValues";

    private static final String MESSAGES_KEY = "pdt.messages";

    private static final String VALUES_WITH_SELECTED_SUBTERMS_KEY = "pdt.valuesWithSelectedSubterms";

    private static final String PREDEFINED_SELECTED_VALUES_KEY = "pdt.predefinedSelectedValues";

    private static final String CUSTOM_SELECTED_VALUES_KEY = "pdt.customSelectedValues";

    private static final String CUSTOM_VALUES_CATEGORIES_KEY = "pdt.customValuesCategoriesElt";

    @Inject
    private Execution execution;

    @Inject
    @Named("solr")
    private ScriptService ontologyService;

    @SuppressWarnings("unchecked")
    public String display(Map data)
    {
        if (Map.class.isAssignableFrom(data.values().iterator().next().getClass())) {
            return displayMultipleSections(data);
        } else {
            return displayOneSection(data);
        }
    }

    public String displayOneSection(Map<String, Object> data)
    {
        try {
            preDisplay(data);
            if (!"edit".equals(getMode())) {
                Map<String, String> fieldsToAdd = getSectionCustomValues(data);
                cleanSection(data);
                addSectionCustomValues(data, fieldsToAdd);
                if (data.isEmpty()) {
                    return "";
                }
            }
            return "{{html wiki=true clean=false}}\n" + handleSection(data) + "\n{{/html}}\n";
        } finally {
            postDisplay();
        }
    }

    public String displayMultipleSections(Map<String, Map<String, Object>> data)
    {
        try {
            preDisplay(data);
            if (!"edit".equals(getMode())) {
                cleanSections(data);
                if (data.isEmpty()) {
                    return "";
                }
            }
            Object[] sectionNames = data.keySet().toArray();
            String result = "";
            for (Object sectionName : sectionNames) {
                result += "(% class=\"" + getPropertyName() + "-group\" %)(((\n";
                result += "===" + sectionName + "===\n";
                result +=
                    "{{html wiki=true clean=false}}\n"
                    + handleSection(data.get(sectionName))
                    + "{{/html}}";
                result += ")))\n";
            }
            return result;
        } finally {
            postDisplay();
        }
    }

    public void use(String prefix, String name)
    {
        this.execution.getContext().setProperty(NAME_KEY, prefix + name);
        this.execution.getContext().setProperty(PROPERTY_NAME_KEY, name);
    }

    public void setDocument(Document document)
    {
        this.execution.getContext().setProperty(DOCUMENT_KEY, document);
    }

    public void setSelectedValues(Collection<String> values)
    {
        Set<String> selectedValues = new HashSet<String>();
        if (values != null) {
            selectedValues.addAll(values);
        }
        this.execution.getContext().setProperty(SELECTED_VALUES_KEY, selectedValues);
    }

    public void setMode(String mode)
    {
        this.execution.getContext().setProperty(MODE_KEY, mode);
    }

    public void setMessageMap(Map<String, String> messages)
    {
        Map<String, String> messageMap = new LinkedHashMap<String, String>();
        messageMap.putAll(messages);
        this.execution.getContext().setProperty(MESSAGES_KEY, messageMap);
    }

    private String getLabelFromOntology(String id)
    {
        SolrDocument phObj = ((SolrScriptService) this.ontologyService).get(id);
        if (phObj != null) {
            return (String) phObj.get("name");
        }
        return id;
    }

    private String getDisplayedImageProperty(String caption, com.xpn.xwiki.api.Object dataObj)
    {
        String result = "";
        if (StringUtils.isNotEmpty(caption)) {
            result = "<span class=\"caption\">" + caption + "</span>\n";
        }
        String objDisplay = (String) dataObj.display(CUSTOM_MAPPING_IMAGES_FIELD, getMode());
        if (StringUtils.isNotEmpty(objDisplay.trim())) {
            result += objDisplay + "\n";
            return "<div class=\"image-gallery" + ("edit".equals(getMode()) ? " emphasized-box" : "") + "\">" + result
                + "<div class=\"clear\"></div></div>";
        }
        return "";
    }

    private boolean isOntologyId(String id)
    {
        return id.matches("[A-Z]+:[0-9]{7}");
    }

    private boolean isOntologyOther(String id)
    {
        return id.equals(OTHER_FIELD_MARKER);
    }

    private boolean isNonOntologyCheckBox(String id)
    {
        return !isOntologyOther(id) && id.startsWith("_c_");
    }

    private boolean isNonOntologyInput(String id)
    {
        return !isOntologyOther(id) && id.startsWith("_i_");
    }

    private boolean isFreeText(String id)
    {
        return !isOntologyOther(id) && id.startsWith("_t_");
    }

    private boolean isSubsection(String id)
    {
        return !isOntologyId(id) && !id.startsWith(UNDERSCORE);
    }

    private String wrapDetailsRequestMessage(String message)
    {
        return hintMessage("(Please list specific " + message + " in the \"Other\" box)");
    }

    private String hintMessage(String message)
    {
        return "<div class='hint'>" + message + "</div>";
    }

    private boolean enableDropdown(String id)
    {
        Map<String, String> params = new HashMap<String, String>();
        params.put("is_a", id);
        return isOntologyId(id) && (((SolrScriptService) this.ontologyService).search(params, 1, 0).size() > 0);
    }

    public String handleSection(Map<String, ? > sectionData)
    {
        StringBuilder result = new StringBuilder();
        boolean hasOtherInput = false;
        boolean hasImages = false;
        String restriction;
        String mapping;
        String imagesLabel = "";

        if (sectionData.containsKey(OTHER_FIELD_MARKER)) {
            hasOtherInput = true;
            sectionData.remove(OTHER_FIELD_MARKER);
        }
        if (sectionData.containsKey(IMAGE_FIELD_MARKER)) {
            hasImages = true;
            imagesLabel = (String) sectionData.remove(IMAGE_FIELD_MARKER);
        }
        restriction = (String) sectionData.remove(RESTRICTION_FIELD_MARKER);
        mapping = (String) sectionData.remove(CUSTOM_MAPPING_FIELD_MARKER);
        com.xpn.xwiki.api.Object mappingObj =
            getDocument().getObject(CUSTOM_MAPPING_CLASS, CUSTOM_MAPPING_CATEGORY_ID_FIELD, mapping);

        String customValueDisplay = "";
        String customValueDisplayField = "";
        if (mappingObj != null) {
            customValueDisplay = "";
            @SuppressWarnings("unchecked")
            List<String> values = (List<String>) mappingObj.getProperty(CUSTOM_MAPPING_VALUES_FIELD).getValue();
            for (String val : values) {
                customValueDisplay += generateCheckBox(val, getLabelFromOntology(val));
                customValueDisplay += getCustomValuesCategoriesElt().get(val);
            }
            customValueDisplay = "<div class=\"custom-display-data\">" + customValueDisplay + "</div>";

            String fieldName =
                CUSTOM_MAPPING_CLASS + '_' + mappingObj.getNumber() + '_' + CUSTOM_MAPPING_VALUES_FIELD;
            customValueDisplayField =
                "<input class='mapping' type='hidden' name='" + fieldName + "' value='"
                + mappingObj.display(CUSTOM_MAPPING_VALUES_FIELD, "view") + "' />";
        }

        Object[] fieldIds = sectionData.keySet().toArray();
        for (int i = 0; i < fieldIds.length; ++i) {
            result.append(handleField((String) fieldIds[i], sectionData));
        }

        if (hasOtherInput) {
            if (StringUtils.isNotEmpty(result)) {
                result = new StringBuilder("<div class='phenotypes-main half-width'>" + result.toString() + "</div>");
                result.append("<div class='phenotypes-other half-width'>");
            } else {
                result.append("<div class='phenotypes-main'>");
            }
            result.append(customValueDisplay);

            result.append(generateInput(OTHER_FIELD_MARKER, true));
            if (restriction != null) {
                result.append(
                    "<input type='hidden' name='" + RESTRICTION_FIELD_MARKER + "' value='" + restriction + "' />");
            }

            result.append(customValueDisplayField);

            result.append("</div>");
            result.append("<div class='clear'></div>");

        } else {
            result = new StringBuilder("<div class='phenotypes-main'>" + result.toString() + customValueDisplay
                + customValueDisplayField + "</div>");
        }

        if (hasImages && mappingObj != null) {
            result.append(getDisplayedImageProperty(imagesLabel, mappingObj));
        }
        return result.toString();
    }

    private String handleField(String id, Map<String, ? > data)
    {
        Object label = (data.get(id) == null ? getLabelFromOntology(id) : data.get(id));
        if (isOntologyId(id)) {
            return generateCheckBox(id, (String) label);
        } else if (isOntologyOther(id)) {
            return generateInput((String) label, true);
        } else if (isNonOntologyCheckBox(id)) {
            return generateCheckBox(id.substring(3), (String) label);
        } else if (isNonOntologyInput(id)) {
            return generateInput((String) label, false);
        } else if (isSubsection(id)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> subsection = (Map<String, Object>) data.get(id);
            return "<label class='section'>" + id + "</label><div class='subsection'>" + handleSection(subsection)
                + "</div>";
        } else if (isFreeText(id)) {
            return generateFreeText((String) label);
        }
        return "";
    }

    private String generateCheckBox(String value, String label)
    {
        SolrDocument doc = ((SolrScriptService) this.ontologyService).get(value);
        if (!"edit".equals(getMode())) {
            return "<div class='value-checked'>" + label + "</div>";
        }
        String cssClass = "term-label" + (enableDropdown(value) ? " dropdown-root" : "")
            + (BooleanUtils.isTrue(getValuesWithSelectedSubterms().get(value)) ? " subterm-selected" : "");
        String id = getName() + (value.startsWith(UNDERSCORE) ? "" : UNDERSCORE) + value;
        String message = getMessageMap().get(value);
        message = (message == null) ? "" : this.wrapDetailsRequestMessage(message);
        String title = "";
        if (doc != null && StringUtils.isNotEmpty((String) doc.getFieldValue("name"))) {
            title = " title='" + doc.getFieldValue("name") + "'";
        }
        String checked = getSelectedValues().contains(value) ? " checked='checked'" : "";
        return "<label class='" + cssClass + "' for='" + id + "'><input type='checkbox'" + checked + title + " name='"
            + getName() + "' id='" + id + "' value='" + value + "'/>" + label + "</label><br/>" + message;
    }

    private String generateInput(String label, boolean suggested)
    {
        String result = "";
        String id = getName() + UNDERSCORE + Math.random();
        String displayedLabel = (suggested ? "Other" : label);
        if (displayedLabel.matches("^\\(.*\\)$")) {
            displayedLabel = "<span class='hint'>" + displayedLabel + "</span>";
        } else {
            displayedLabel += ":";
        }
        result =
            "<label for='" + id + '\'' + (suggested ? " class='label-other label-other-" + getName() + '\'' : "") + ">"
            + displayedLabel + "</label>";
        if (suggested) {
            result += "<p class='hint'>(enter a free text and choose among suggested ontology terms)</p>";
        }
        result +=
            "<input type='text' name='" + getName() + '\''
            + (suggested ? "' class='suggested multi suggest-hpo generateCheckboxes'" : "")
            + " value='' size='16' id='" + id + "'/>";
        return result;
    }

    private String generateFreeText(String label)
    {
        String result = "";
        String id = getName() + UNDERSCORE + Math.random();
        result = "<label for='" + id + '\'' + ">" + label + "</label>";
        result += "<textarea name='" + getName() + "' rows='8' cols='40' id='" + id + "'></textarea>";
        return result;
    }

    private void cleanSections(Map<String, Map<String, Object>> data)
    {
        Iterator<Entry<String, Map<String, Object>>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map<String, Object> section = it.next().getValue();
            cleanSection(section);
            if (section.isEmpty()) {
                it.remove();
            }
        }
    }

    private void cleanSection(Map<String, Object> section)
    {
        Map<String, String> fieldsToAdd = getSectionCustomValues(section);

        Iterator<Entry<String, Object>> it = section.entrySet().iterator();

        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();
            String id = entry.getKey();
            if (isSubsection(id)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subsection = (Map<String, Object>) entry.getValue();
                cleanSection(subsection);
                if (subsection.isEmpty()) {
                    it.remove();
                }
            } else {
                if (!getSelectedValues().contains(id)) {
                    it.remove();
                }
            }
        }

        addSectionCustomValues(section, fieldsToAdd);
    }

    private void addSectionCustomValues(Map<String, ? super Object> section, Map<String, String> customValues)
    {
        Object[] fieldIds = customValues.keySet().toArray();
        for (int i = 0; i < fieldIds.length; ++i) {
            // section.put(fieldIds[i], (Object)fieldsToAdd.get(fieldIds[i]));
            // just a trick to make it work without class cast errors...
            section.put(customValues.get(fieldIds[i]), new LinkedHashMap<String, Object>());
        }
    }

    private Map<String, String> getSectionCustomValues(Map<String, ? > section)
    {
        String mapping = (String) section.remove(CUSTOM_MAPPING_FIELD_MARKER);
        if (StringUtils.isNotEmpty(mapping)) {
            com.xpn.xwiki.api.Object mappingObj =
                getDocument().getObject(CUSTOM_MAPPING_CLASS, CUSTOM_MAPPING_CATEGORY_ID_FIELD, mapping);
            if (mappingObj != null) {
                @SuppressWarnings("unchecked")
                Collection<String> values =
                    (Collection<String>) mappingObj.getProperty(CUSTOM_MAPPING_VALUES_FIELD).getValue();
                Map<String, String> result = getSectionCustomValues(section, values);
                if (section.containsKey(IMAGE_FIELD_MARKER)) {
                    String displayedImages =
                        getDisplayedImageProperty((String) section.remove(IMAGE_FIELD_MARKER), mappingObj);
                    if (StringUtils.isNotEmpty(displayedImages)) {
                        result.put(IMAGE_FIELD_MARKER, displayedImages);
                    }
                }
                return result;
            }
        }
        return new LinkedHashMap<String, String>();
    }

    private Map<String, String> getSectionCustomValues(Map<String, ? > section, Collection<String> otherValues)
    {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (String val : otherValues) {
            if (!section.containsKey(val.substring(3))) {
                String label;
                SolrDocument phObj = ((SolrScriptService) this.ontologyService).get(val);
                if (phObj != null) {
                    label = (String) phObj.get("name");
                } else {
                    label = val;
                }
                result.put(val, label);
            }
        }
        return result;
    }

    private void preDisplay(Map<String, ? > data)
    {
        if (getSelectedValues() == null || getSelectedValues().size() == 0) {
            return;
        }
        for (String id : data.keySet()) {
            if (isOntologyId(id)) {
                getValuesWithSelectedSubterms().put(id, false);
            } else if (isSubsection(id)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subsection = (Map<String, Object>) data.get(id);
                preDisplay(subsection);
            }
        }
        for (String selectedValue : getSelectedValues()) {
            if (getValuesWithSelectedSubterms().containsKey(selectedValue)) {
                getPredefinedSelectedValues().add(selectedValue);
            } else {
                getCustomSelectedValues().add(selectedValue);
                String categoriesElt = "";
                SolrDocument termObj = ((SolrScriptService) this.ontologyService).get(selectedValue);
                if (termObj != null && termObj.get("term_category") != null
                    && List.class.isAssignableFrom(termObj.get("term_category").getClass())) {
                    @SuppressWarnings("unchecked")
                    List<String> categories = (List<String>) termObj.get("term_category");
                    for (int j = 0; j < categories.size(); ++j) {
                        if (getValuesWithSelectedSubterms().containsKey(categories.get(j))) {
                            getValuesWithSelectedSubterms().put(categories.get(j), true);
                        }
                        categoriesElt += "<input type=\"hidden\" value=\"" + categories.get(j) + "\"/>\n";
                    }
                }
                categoriesElt = "<span class=\"hidden term-category\">\n" + categoriesElt + "</span>\n";
                getCustomValuesCategoriesElt().put(selectedValue, categoriesElt);
            }
        }
    }

    private void postDisplay()
    {
        this.execution.getContext().removeProperty(DOCUMENT_KEY);
        this.execution.getContext().removeProperty(NAME_KEY);
        this.execution.getContext().removeProperty(PROPERTY_NAME_KEY);
        this.execution.getContext().removeProperty(MODE_KEY);
        this.execution.getContext().removeProperty(SELECTED_VALUES_KEY);
        this.execution.getContext().removeProperty(MESSAGES_KEY);
        getValuesWithSelectedSubterms().clear();
        getPredefinedSelectedValues().clear();
        getCustomSelectedValues().clear();
        getCustomValuesCategoriesElt().clear();
    }

    private Document getDocument()
    {
        return (Document) this.execution.getContext().getProperty(DOCUMENT_KEY);
    }

    private Set<String> getSelectedValues()
    {
        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) this.execution.getContext().getProperty(SELECTED_VALUES_KEY);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    private Map<String, String> getMessageMap()
    {
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) this.execution.getContext().getProperty(MESSAGES_KEY);
        return result;
    }

    private String getMode()
    {
        return (String) this.execution.getContext().getProperty(MODE_KEY);
    }

    private String getName()
    {
        return (String) this.execution.getContext().getProperty(NAME_KEY);
    }

    private String getPropertyName()
    {
        return (String) this.execution.getContext().getProperty(PROPERTY_NAME_KEY);
    }

    private Map<String, Boolean> getValuesWithSelectedSubterms()
    {
        @SuppressWarnings("unchecked")
        Map<String, Boolean> valuesWithSelectedSubterms =
            (Map<String, Boolean>) this.execution.getContext().getProperty(VALUES_WITH_SELECTED_SUBTERMS_KEY);
        if (valuesWithSelectedSubterms == null) {
            valuesWithSelectedSubterms = new HashMap<String, Boolean>();
            this.execution.getContext().setProperty(VALUES_WITH_SELECTED_SUBTERMS_KEY, valuesWithSelectedSubterms);
        }
        return valuesWithSelectedSubterms;
    }

    private Set<String> getPredefinedSelectedValues()
    {
        @SuppressWarnings("unchecked")
        Set<String> predefinedSelectedValues =
            (Set<String>) this.execution.getContext().getProperty(PREDEFINED_SELECTED_VALUES_KEY);
        if (predefinedSelectedValues == null) {
            predefinedSelectedValues = new HashSet<String>();
            this.execution.getContext().setProperty(PREDEFINED_SELECTED_VALUES_KEY, predefinedSelectedValues);
        }
        return predefinedSelectedValues;
    }

    private Set<String> getCustomSelectedValues()
    {
        @SuppressWarnings("unchecked")
        Set<String> customSelectedValues =
            (Set<String>) this.execution.getContext().getProperty(CUSTOM_SELECTED_VALUES_KEY);
        if (customSelectedValues == null) {
            customSelectedValues = new HashSet<String>();
            this.execution.getContext().setProperty(CUSTOM_SELECTED_VALUES_KEY, customSelectedValues);
        }
        return customSelectedValues;
    }

    private Map<String, String> getCustomValuesCategoriesElt()
    {
        @SuppressWarnings("unchecked")
        Map<String, String> customValuesCategoriesElt =
            (Map<String, String>) this.execution.getContext().getProperty(CUSTOM_VALUES_CATEGORIES_KEY);
        if (customValuesCategoriesElt == null) {
            customValuesCategoriesElt = new HashMap<String, String>();
            this.execution.getContext().setProperty(CUSTOM_VALUES_CATEGORIES_KEY, customValuesCategoriesElt);
        }
        return customValuesCategoriesElt;
    }
}
