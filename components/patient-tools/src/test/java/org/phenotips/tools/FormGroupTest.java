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
import static org.mockito.Mockito.verify;

public class FormGroupTest
{
    private FormGroup testGroup;

    private String[] fieldnames;

    @Before
    public void setUp()
    {
        this.testGroup = new FormGroup("Test Group");
        this.fieldnames = new String[] { "phenotype", "negative_phenotype" };
    }

    @Test
    public void checkElementsAreAdded()
    {
        Assert.assertFalse(this.testGroup.addElement(null));
        FormField testField = mock(FormField.class);
        Assert.assertTrue(this.testGroup.addElement(testField));
        Assert.assertTrue(this.testGroup.elements.contains(testField));
    }

    @Test
    public void checkEmptyFormGroupDisplay()
    {
        Assert.assertEquals("",
            this.testGroup.display(DisplayMode.Edit, new String[] { "phenotype", "negative_phenotype" }));
        Assert.assertEquals("",
            this.testGroup.display(DisplayMode.View, new String[] { "phenotype", "negative_phenotype" }));
    }

    @Test
    public void checkBehaviorOnEditAndViewMode()
    {
        FormField testField1 = mock(FormField.class);
        FormField testField2 = mock(FormField.class);
        this.testGroup.addElement(testField1);
        this.testGroup.addElement(testField2);

        this.testGroup.display(DisplayMode.Edit, this.fieldnames);
        verify(testField1).display(DisplayMode.Edit, this.fieldnames);
        verify(testField2).display(DisplayMode.Edit, this.fieldnames);

        this.testGroup.display(DisplayMode.View, this.fieldnames);
        verify(testField1).display(DisplayMode.View, this.fieldnames);
        verify(testField2).display(DisplayMode.View, this.fieldnames);
    }
}
