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

import java.util.LinkedList;
import java.util.List;

public class FormGroup extends AbstractFormElement
{
    protected List<FormElement> elements = new LinkedList<FormElement>();

    FormGroup(String title)
    {
        super(title);
    }

    protected boolean addElement(FormElement e)
    {
        return e != null && this.elements.add(e);
    }

    @Override
    public String display(DisplayMode mode, String[] fieldNames)
    {
        StringBuilder str = new StringBuilder(this.elements.size() * 1024);
        for (FormElement e : this.elements) {
            str.append(e.display(mode, fieldNames));
        }
        return str.toString();
    }
}
