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

public class FormSubsectionTest {

    private FormField testFormField;
    private String[] fieldnames;
    String title = "title";
    String type = "type";

    @Before
    public void setUp(){
        testFormField = mock(FormField.class);
        fieldnames = new String[]{ "phenotype", "negative_phenotype" };
    }

    @Test
    public void testSubsectionDisplayTitleAndType(){
        FormSubsection testSubsection = new FormSubsection(title, type);
        Assert.assertEquals("", testSubsection.display(DisplayMode.Edit, fieldnames));
        testSubsection.addElement(testFormField);
        Assert.assertEquals("<label class='section'>" + title + "</label><div class='subsection "
            + type + "'>null</div>", testSubsection.display(DisplayMode.Edit, fieldnames));
    }

    @Test
    public void subsectionDisplayTitleOnly(){
        FormSubsection testSubsection = new FormSubsection(title);
        Assert.assertEquals("", testSubsection.display(DisplayMode.Edit, fieldnames));
        testSubsection.addElement(testFormField);
        Assert.assertEquals("<label class='section'>" + title + "</label><div class='subsection '>null</div>",
            testSubsection.display(DisplayMode.Edit, fieldnames));
    }
}
