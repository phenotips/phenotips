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
package org.phenotips.tools;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.xml.XMLUtils;

import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

public class FormField extends AbstractFormElement
{
    private static final String DEFAULT_CSS_CLASS = "term-entry";

    private static final String EXPANDABLE_CSS_CLASS = " dropdown-root";

    private final String value;

    private final String metaData;

    private final boolean[] selection;

    private final boolean expandable;

    private final String hint;

    private OntologyTerm term;

    FormField(String value, String title, String hint, String metaData, boolean expandable, boolean yesSelected,
        boolean noSelected)
    {
        super(title);
        this.value = value;
        this.hint = hint;
        this.metaData = metaData;
        this.expandable = expandable;
        this.selection = new boolean[2];
        this.selection[YES] = yesSelected;
        this.selection[NO] = noSelected;
        try {
            OntologyManager om =
                ComponentManagerRegistry.getContextComponentManager().getInstance(OntologyManager.class);
            this.term = om.resolveTerm(value);
        } catch (ComponentLookupException ex) {
            this.term = null;
        }
    }

    private boolean isSelected(int which)
    {
        return this.selection[which];
    }

    @Override
    public String display(DisplayMode mode, String[] fieldNames)
    {
        if (DisplayMode.Edit.equals(mode)) {
            return generateFormField(fieldNames);
        }
        return generateSelection(fieldNames);
    }

    protected String generateFormField(String[] fieldNames)
    {
        if (fieldNames[NO] != null) {
            return String
                .format(
                    "<div class='%s%s'><span class='yes-no-picker'>%s%s%s</span><span class='entry-tools'>"
                        + "<span class='entry-tool info-tool' title='Information about this term'>i</span></span>"
                        + " <span title='%s'>%s</span>%s</div>",
                    DEFAULT_CSS_CLASS,
                    this.expandable ? EXPANDABLE_CSS_CLASS : "",
                    generateCheckbox("none", this.value, "", (!isSelected(YES) && !isSelected(NO)), "na", "NA"),
                    generateCheckbox(fieldNames[YES], this.value, this.hint, isSelected(YES), "yes", "Y"),
                    generateCheckbox(fieldNames[NO], this.value, this.hint, isSelected(NO), "no", "N"),
                    this.term == null ? this.title + "\n(custom term)" : (this.term.getName() + (StringUtils
                        .isNotBlank(this.term.getDescription()) ? "\n"
                        + StringEscapeUtils.escapeXml10(this.term.getDescription()) : "")),
                    generateLabel(fieldNames[YES] + '_' + this.value, "yes-no-picker-label", this.title),
                    generateTooltip());

        } else {
            return generateCheckbox(fieldNames[YES], this.value, this.hint, isSelected(YES), DEFAULT_CSS_CLASS
                + (this.expandable ? EXPANDABLE_CSS_CLASS : ""), this.title);
        }
    }

    protected String generateSelection(final String[] fieldNames)
    {
        String selectionMarker = isSelected(YES) ? "yes-selected" : isSelected(NO) ? "no-selected" : null;
        String selectionPrefix = isSelected(NO) ? "NO " : "";
        return (selectionMarker != null) ? ("<div class='value-checked " + selectionMarker + "'>" + selectionPrefix
            + XMLUtils.escapeElementContent(this.title) + this.metaData + "</div>") : "";
    }

    private String generateCheckbox(String name, String value, String title, boolean selected, String labelClass,
        String labelText)
    {
        String id = name + '_' + value;
        return String.format(
            "<label class='%s' for='%s'><input type='checkbox' name='%s' value='%s' id='%s' title='%s'%s/>%s</label>",
            labelClass, id, name, value, id, XMLUtils.escapeAttributeValue(title), (selected ? " checked='checked'"
                : ""), XMLUtils.escapeElementContent(labelText));
    }

    private String generateLabel(String forId, String labelClass, String labelText)
    {
        return String.format("<label class='%s' for='%s'>%s</label>", labelClass, forId,
            XMLUtils.escapeElementContent(labelText));
    }

    private String generateTooltip()
    {
        if (this.term == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append("<div class='tooltip invisible'>");
        result.append("<span class='hide-tool' title='Hide'>×</span>");
        result.append("<span class='info'><span class='key'>[" + this.term.getId() + "]</span> <span class='value'>"
            + this.term.getName() + "</span></span>");
        result.append("<dl>");
        if (StringUtils.isNotBlank(this.term.getDescription())) {
            result.append("<dt class=''></dt>");
            result.append("<dd><div>" + this.term.getDescription() + "</div></dd>");
        }
        @SuppressWarnings("unchecked")
        List<String> synonyms = (List<String>) this.term.get("synonym");
        if (synonyms != null && !synonyms.isEmpty()) {
            result.append("<dt class='also-known-as'>Also known as</dt><dd>");
            for (String s : synonyms) {
                result.append("<div>" + s + "</div>");
            }
            result.append("</dd>");
        }
        result.append("<dt class='is-a-type-of'>Is a type of</dt><dd>");
        for (OntologyTerm parent : this.term.getParents()) {
            result.append("<div>" + parent.getName() + "</div>");
        }
        result.append("</dd></dl></div>");

        return result.toString();
    }

    @Override
    public String toString()
    {
        return this.title;
    }
}
