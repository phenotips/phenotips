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

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import static org.mockito.Mockito.mock;

public class FormGroupTest {

     private FormGroup testGroup;

     @Before
     public void setUp(){
         testGroup = new FormGroup("Test Group");
     }

    @Test
    public void testAddElement(){
        Assert.assertEquals(false, testGroup.addElement(null));
        FormField testField = mock(FormField.class);
        Assert.assertEquals(true, testGroup.addElement(testField));
    }

    @Test
    public void testEmptyGroupDisplay(){
        Assert.assertEquals(testGroup.display(DisplayMode.Edit, new String[] { "phenotype", "negative_phenotype" }), "");
        Assert.assertEquals(testGroup.display(DisplayMode.View, new String[] { "phenotype", "negative_phenotype" }), "");
    }

    @Test
    public void testEditModeDisplay(){
        FormField testField1 = mock(FormField.class);
        FormField testField2 = mock(FormField.class);
        testGroup.addElement(testField1);
        testGroup.addElement(testField2);

        String output = testGroup.display(DisplayMode.Edit, new String[] { "phenotype", "negative_phenotype" });
        Assert.assertNotNull(output);
        output = testGroup.display(DisplayMode.Edit, new String[] { "phenotype" });
        Assert.assertNotNull(output);
    }

}
