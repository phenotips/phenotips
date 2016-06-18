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

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class FormConditionalSubsectionTest
{
    String title = "title";

    String type = "type";

    @Test
    public void emptyUnselectedDisplay()
    {
        FormElement testYesNoPicker = mock(FormField.class);
        FormConditionalSubsection testConditionalSubsection = new FormConditionalSubsection(
            this.title, this.type, testYesNoPicker, false, false);
        Assert.assertEquals("", testConditionalSubsection.display(
            DisplayMode.Edit, new String[] { "phenotypes", "negative_phenotypes" }));
        Assert.assertEquals("", testConditionalSubsection.display(
            DisplayMode.View, new String[] { "phenotypes", "negative_phenotypes" }));
    }

    @Test
    public void selectedDisplay()
    {
        FormElement testYesNoPicker = mock(FormField.class);
        FormConditionalSubsection testConditionalSubsection = new FormConditionalSubsection(
            this.title, this.type, testYesNoPicker, true, false);
        FormField testFormField = mock(FormField.class);
        testConditionalSubsection.addElement(testFormField);

        Assert.assertEquals("<div class='section'>null<div class='dropdown invisible " + this.type
            + "'><div>null</div></div></div>", testConditionalSubsection.display(
                DisplayMode.Edit, new String[] { "phenotypes", "negative_phenotypes" }));
        Assert.assertEquals(
            "<div class='section value-checked'>title</div><div class='subsection " + this.type + "'>null</div>",
            testConditionalSubsection.display(DisplayMode.View, new String[] { "phenotypes", "negative_phenotypes" }));
    }

    @Test
    public void noSelectedDisplay()
    {
        FormElement testYesNoPicker = mock(FormField.class);
        FormConditionalSubsection testConditionalSubsection = new FormConditionalSubsection(
            this.title, this.type, testYesNoPicker, false, true);
        FormField testFormField = mock(FormField.class);
        testConditionalSubsection.addElement(testFormField);

        Assert.assertEquals("<div class='section'>null<div class='dropdown invisible " + this.type
            + "'><div>null</div></div></div>", testConditionalSubsection.display(
                DisplayMode.Edit, new String[] { "phenotypes", "negative_phenotypes" }));
        Assert.assertEquals(
            "<div class='section value-checked'>title</div><div class='subsection " + this.type + "'>null</div>",
            testConditionalSubsection.display(DisplayMode.View, new String[] { "phenotypes", "negative_phenotypes" }));
    }
}
