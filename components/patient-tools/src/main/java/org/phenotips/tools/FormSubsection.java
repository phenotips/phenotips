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

import org.apache.commons.lang3.StringUtils;

public class FormSubsection extends FormGroup
{
    private final String type;

    FormSubsection(String title)
    {
        this(title, "");
    }

    FormSubsection(String title, String type)
    {
        super(title);
        this.type = type;
    }

    @Override
    public String getTitle()
    {
        return this.title;
    }

    @Override
    public String display(DisplayMode mode, String[] fieldNames)
    {
        String displayedElements = super.display(mode, fieldNames);
        if (StringUtils.isBlank(displayedElements)) {
            return "";
        }
        return "<label class='section'>"
            + XMLUtils.escapeElementContent(this.title)
            + "</label><div class='subsection " + this.type + "'>"
            + displayedElements + "</div>";
    }
}
