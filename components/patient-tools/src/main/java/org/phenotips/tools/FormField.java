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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.translation.TranslationManager;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.xml.XMLUtils;

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

    private VocabularyTerm term;

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
            VocabularyManager vm =
                ComponentManagerRegistry.getContextComponentManager().getInstance(VocabularyManager.class);
            this.term = vm.resolveTerm(value);
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
            return String.format(
                "<div class='%s%s'><span class='yes-no-picker'>%s%s%s</span> <span title='%s'>%s</span></div>",
                DEFAULT_CSS_CLASS,
                this.expandable ? EXPANDABLE_CSS_CLASS : "",
                generateCheckbox("none", this.value, "", (!isSelected(YES) && !isSelected(NO)), "na", "NA"),
                generateCheckbox(fieldNames[YES], this.value, this.hint, isSelected(YES), "yes", "Y"),
                generateCheckbox(fieldNames[NO], this.value, this.hint, isSelected(NO), "no", "N"),
                this.term == null ? this.title + "\n(custom term)"
                    : (this.term.getName() + (StringUtils.isNotBlank(this.term.getDescription())
                        ? "\n" + StringEscapeUtils.escapeXml10(this.term.getDescription()) : "")),
                generateLabel(fieldNames[YES] + '_' + this.value, "yes-no-picker-label", this.title));
        } else {
            return generateCheckbox(fieldNames[YES], this.value, this.hint, isSelected(YES), DEFAULT_CSS_CLASS
                + (this.expandable ? EXPANDABLE_CSS_CLASS : ""), this.title);
        }
    }

    protected String generateSelection(final String[] fieldNames)
    {
        String selectionMarker = isSelected(YES) ? "yes-selected" : isSelected(NO) ? "no-selected" : null;
        String termSourceMarker = "";
        if (this.term == null || StringUtils.isEmpty(this.term.getId())) {
            String customTitle = "";
            try {
                TranslationManager tm =
                    ComponentManagerRegistry.getContextComponentManager().getInstance(TranslationManager.class);
                customTitle = tm.translate("phenotips.patientSheetCode.termSuggest.nonStandardPhenotype");
            } catch (ComponentLookupException ex) {
                // Will not happen, and if it does, it doesn't matter, the tooltip is not that critical
            }
            termSourceMarker = "<span class='fa fa-exclamation-triangle fa-fw' title='" + customTitle + "'> </span>";
        }
        String selectionPrefix = isSelected(NO) ? "NO " : "";
        return (selectionMarker != null) ? ("<div class='value-checked " + selectionMarker + "'>" + termSourceMarker
            + selectionPrefix + XMLUtils.escapeElementContent(this.title) + this.metaData + "</div>") : "";
    }

    private String generateCheckbox(String name, String value, String title, boolean selected, String labelClass,
        String labelText)
    {
        String id = name + '_' + value;
        return String.format(
            "<label class='%s' for='%s'><input type='checkbox' name='%s' value='%s' id='%s' title='%s'%s/>%s</label>",
            labelClass, id, name, value, id, XMLUtils.escapeAttributeValue(title),
            (selected ? " checked='checked'" : ""), XMLUtils.escapeElementContent(labelText));
    }

    private String generateLabel(String forId, String labelClass, String labelText)
    {
        return String.format("<label class='%s' for='%s'>%s</label>", labelClass, forId,
            XMLUtils.escapeElementContent(labelText));
    }

    @Override
    public String toString()
    {
        return this.title;
    }
}
