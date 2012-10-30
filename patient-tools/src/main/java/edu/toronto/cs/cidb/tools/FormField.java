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

import org.xwiki.xml.XMLUtils;

public class FormField extends AbstractFormElement
{
    protected static final String DEFAULT_CSS_CLASS = "term-entry";

    protected static final String EXPANDIBLE_CSS_CLASS = " dropdown-root";

    protected final String value;

    protected final boolean selection[];

    private final boolean expandable;

    private final String hint;

    FormField(String value, String title, String hint, boolean expandable,
        String name, boolean selected)
    {
        this(value, title, hint, expandable, selected, false);
    }

    FormField(String value, String title, String hint, boolean expandable,
        boolean yesSelected, boolean noSelected)
    {
        super(title);
        this.value = value;
        this.hint = hint;
        this.expandable = expandable;
        this.selection = new boolean[2];
        this.selection[YES] = yesSelected;
        this.selection[NO] = noSelected;
    }

    @Override
    public String getTitle()
    {
        return this.title;
    }

    private boolean isSelected(int which)
    {
        return this.selection[which];
    }

    @Override
    public String display(DisplayMode mode, String fieldNames[])
    {
        if (DisplayMode.Edit.equals(mode)) {
            return generateFormField(fieldNames);
        }
        return generateSelection(fieldNames);
    }

    protected String generateFormField(String fieldNames[])
    {
        if (fieldNames[NO] != null) {
            return "<span class='"
                + DEFAULT_CSS_CLASS
                + (this.isExpandable() ? EXPANDIBLE_CSS_CLASS : "")
                + "'><span class='yes-no-picker'>"
                + generateCheckbox("NA", this.value, "",
                    (!isSelected(YES) && !isSelected(NO)), "na", "NA")
                + generateCheckbox(fieldNames[YES], this.value, this.hint,
                    isSelected(YES), "yes", "Y")
                + generateCheckbox(fieldNames[NO], this.value, this.hint,
                    isSelected(NO), "no", "N")
                + "</span>"
                + generateLabel(fieldNames[YES] + "_" + this.value,
                    "yes-no-picker-label", this.title) + "</span>";
        } else {
            return generateCheckbox(
                fieldNames[YES],
                this.value,
                this.hint,
                isSelected(YES),
                DEFAULT_CSS_CLASS
                    + (this.isExpandable() ? EXPANDIBLE_CSS_CLASS : ""),
                this.title);
        }
    }

    private boolean isExpandable()
    {
        return this.expandable;
    }

    protected String generateSelection(final String fieldNames[])
    {
        String selectionMarker = isSelected(YES) ? "yes-selected"
            : isSelected(NO) ? "no-selected" : null;
        return (selectionMarker != null) ? ("<div class='value-checked "
            + selectionMarker + "'>"
            + XMLUtils.escapeElementContent(this.title) + "</div>") : "";
    }

    private String generateCheckbox(String name, String value, String title,
        boolean selected, String labelClass, String labelText)
    {
        String id = name + "_" + value;
        return "<label class='" + labelClass + "' for='" + id
            + "'><input type='checkbox' name='" + name + "' value='"
            + value + "' id='" + id + "' title='" + title + "'"
            + (selected ? " checked='checked'" : "") + "/>"
            + XMLUtils.escapeElementContent(labelText) + "</label>";
    }

    private String generateLabel(String forId, String labelClass,
        String labelText)
    {
        return "<label class='" + labelClass + "' for='" + forId + "'>"
            + XMLUtils.escapeElementContent(labelText) + "</label>";
    }

    @Override
    public String toString()
    {
        return this.title;
    }
}
