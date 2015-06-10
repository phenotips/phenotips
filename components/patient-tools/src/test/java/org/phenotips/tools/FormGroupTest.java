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

import org.junit.Test;
import org.junit.Assert;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;

 /** Created by tarang on 15-06-09.
 */
public class FormGroupTest {

    @Test
    public void testAddElement(){
        FormGroup addTestGroup = new FormGroup("Add Test Group");
        Assert.assertEquals(false, addTestGroup.addElement(null));
        FormField testField = mock(FormField.class);
        Assert.assertEquals(true, addTestGroup.addElement(testField));
    }

    @Test
    public void testEmptyGroupDisplay(){
        FormGroup emptyGroup = new FormGroup("Empty FormGroup");
        Assert.assertEquals(emptyGroup.display(DisplayMode.Edit, new String[] { "phenotype", "negative_phenotype" }), "");
        Assert.assertEquals(emptyGroup.display(DisplayMode.View, new String[] { "phenotype", "negative_phenotype" }), "");
    }

    @Test
    public void testEditModeDisplay(){
        FormGroup testGroup = new FormGroup("Display Test Group");
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
