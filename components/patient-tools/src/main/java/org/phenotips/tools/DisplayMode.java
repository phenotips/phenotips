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

/**
 * Different ways of displaying form elements. Used by the {@link FormElement#display(DisplayMode, String[])} method.
 *
 * @version $Id$
 */
public enum DisplayMode
{
    View,
    Edit;

    public static DisplayMode get(String mode)
    {
        if ("edit".equalsIgnoreCase(mode)) {
            return Edit;
        }
        return View;
    }
}
