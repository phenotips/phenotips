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

import org.xwiki.xml.XMLUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class FormSection extends FormGroup
{
    private final String propertyName;

    private final List<String> categories;

    private FormGroup customElements = new FormGroup("");

    FormSection(String title, String propertyName, Collection<String> categories)
    {
        super(title);
        this.propertyName = propertyName;
        this.categories = new LinkedList<String>();
        if (categories != null) {
            this.categories.addAll(categories);
        }
    }

    public String getPropertyName()
    {
        return this.propertyName;
    }

    @Override
    public String getTitle()
    {
        return this.title;
    }

    protected boolean addCustomElement(FormElement e)
    {
        return this.customElements.addElement(e);
    }

    @Override
    public String display(DisplayMode mode, String[] fieldNames)
    {
        String displayedElements = super.display(mode, fieldNames);
        String customValueDisplay = this.customElements.display(mode, fieldNames);
        if (!DisplayMode.Edit.equals(mode)
            && StringUtils.isBlank(displayedElements)
            && StringUtils.isBlank(customValueDisplay)) {
            return "";
        }
        String display = "";
        // Hide sections with no elements, or sections with only custom elements displayed in the main phenotype
        // section, which has the selection summary
        if (this.elements.isEmpty()
            && (this.customElements.elements.isEmpty()
                || ("phenotype".equals(this.propertyName) && DisplayMode.Edit.equals(mode)))) {
            display = "display:none";
        }
        return String.format("<div class='%s-group%s' style='" + display + "'><h3 id='H%s'><span>%s</span></h3>"
            + "<div class='%1$s-main predefined-entries'>%s</div>"
            + "<div class='%1$s-other custom-entries'>%s%s</div></div>",
            this.getPropertyName(), (this.categories.contains("HP:0000001") ? " catch-all" : ""),
            this.title.replaceAll("[^a-zA-Z0-9]+", "-"),
            XMLUtils.escapeElementContent(this.title), displayedElements,
            StringUtils.isNotBlank(customValueDisplay)
                ? ("<div class=\"custom-display-data\">" + customValueDisplay + "</div>") : "",
            generateSuggestionsField(mode, fieldNames));
    }

    private String generateSuggestionsField(DisplayMode mode, String[] fieldNames)
    {
        if (!DisplayMode.Edit.equals(mode)) {
            return "";
        }
        String result = "";
        String id = fieldNames[YES] + "_" + Math.random();
        String displayedLabel = "Other";
        result += String.format("<label for='%s' class='label-other label-other-%s'>%s</label>", id, fieldNames[YES],
            displayedLabel);

        result += String.format("<input type='text' name='%s' class='suggested multi suggest-hpo %s accept-value'"
            + " value='' size='16' id='%s' placeholder='%s'/>", fieldNames[YES],
            (fieldNames[NO] == null ? "generateCheckboxes" : "generateYesNo"), id,
            "enter free text and choose among suggested ontology terms");
        result += String.format("<input type='hidden' value='%s' name='_category'/>",
            this.categories.toString().replaceAll("[\\[\\]\\s]", ""));
        return result;
    }

    public FormGroup getCustomElements()
    {
        return this.customElements;
    }

    public void setCustomElements(FormGroup customElements)
    {
        this.customElements = customElements;
    }

    public List<String> getCategories()
    {
        return this.categories;
    }

}
