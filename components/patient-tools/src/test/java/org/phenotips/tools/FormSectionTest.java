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
    private FormField testFormField;
    private FormGroup testFormGroup;

    @Before
    public void setUp(){
        List<String> categories = new LinkedList<String>();
        testFormSection = new FormSection("title", "phenotype", categories);
        testFormField = mock(FormField.class);
        testFormGroup = mock(FormGroup.class);
    }

    @Test
    public void testCustomElement(){
        Assert.assertNotNull(testFormSection.addCustomElement(testFormField));
        Assert.assertNotNull(testFormSection.addCustomElement(testFormGroup));
        Assert.assertNotNull(testFormSection.getCustomElements());
    }

    @Test
    public void testSectionDisplay(){
        String[] fieldNames = new String[]{"phenotype", "negative_phenotype"};
        Assert.assertEquals(testFormSection.display(DisplayMode.View, fieldNames), "");
        Assert.assertNotEquals(testFormSection.display(DisplayMode.Edit, fieldNames), "");
        testFormSection.addCustomElement(testFormField);
        Assert.assertNotEquals(testFormSection.display(DisplayMode.Edit, fieldNames), "");
        testFormSection.addCustomElement(testFormGroup);
        Assert.assertNotEquals(testFormSection.display(DisplayMode.View, fieldNames), "");
    }
}
