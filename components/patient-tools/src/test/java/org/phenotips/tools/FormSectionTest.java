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
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;


public class FormSectionTest {

    private FormSection testFormSection;

    @Before
    public void setUp(){
        List<String> categories = new LinkedList<String>();
        testFormSection = new FormSection("title", "phenotype", categories);
    }

    @Test
    public void testAddCustomElement(){
        FormField testField = mock(FormField.class);
        FormGroup testGroup = mock(FormGroup.class);
        Assert.assertNotNull(testFormSection.addCustomElement(testField));
        Assert.assertNotNull(testFormSection.addCustomElement(testGroup));
    }

    @Test
    public void testSectionDisplay(){
        String[] fieldNames = new String[]{"phenotype", "negative_phenotype"};
        Assert.assertEquals(testFormSection.display(DisplayMode.View, fieldNames), "");
        Assert.assertNotEquals(testFormSection.display(DisplayMode.Edit, fieldNames), "");
        FormField testField = mock(FormField.class);
        testFormSection.addCustomElement(testField);
        Assert.assertNotEquals(testFormSection.display(DisplayMode.Edit, fieldNames), "");
    }
}
