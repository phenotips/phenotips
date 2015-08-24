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
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class FormSubsectionTest
{
    private FormField testFormField;

    private String[] fieldnames;

    String title = "title";

    String type = "type";

    @Before
    public void setUp()
    {
        this.testFormField = mock(FormField.class);
        this.fieldnames = new String[] { "phenotype", "negative_phenotype" };
    }

    @Test
    public void testSubsectionDisplayTitleAndType()
    {
        FormSubsection testSubsection = new FormSubsection(this.title, this.type);
        Assert.assertEquals("", testSubsection.display(DisplayMode.Edit, this.fieldnames));
        testSubsection.addElement(this.testFormField);
        Assert.assertEquals("<label class='section'>" + this.title + "</label><div class='subsection "
            + this.type + "'>null</div>", testSubsection.display(DisplayMode.Edit, this.fieldnames));
    }

    @Test
    public void subsectionDisplayTitleOnly()
    {
        FormSubsection testSubsection = new FormSubsection(this.title);
        Assert.assertEquals("", testSubsection.display(DisplayMode.Edit, this.fieldnames));
        testSubsection.addElement(this.testFormField);
        Assert.assertEquals("<label class='section'>" + this.title + "</label><div class='subsection '>null</div>",
            testSubsection.display(DisplayMode.Edit, this.fieldnames));
    }

    @Test
    public void getTitleReturnsSpecifiedTitle()
    {
        FormSubsection testSubsection = new FormSubsection(null);
        Assert.assertNull(testSubsection.getTitle());

        testSubsection = new FormSubsection("");
        Assert.assertEquals("", testSubsection.getTitle());

        testSubsection = new FormSubsection(this.title);
        Assert.assertEquals(this.title, testSubsection.getTitle());
    }
}
