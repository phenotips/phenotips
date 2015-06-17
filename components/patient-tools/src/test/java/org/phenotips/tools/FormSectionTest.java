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
    private String[] fieldNames;

    String title = "title";
    String propertyName = "phenotype";

    @Before
    public void setUp(){
        List<String> categories = new LinkedList<String>();
        testFormSection = new FormSection(title, propertyName, categories);
        testFormField = mock(FormField.class);
        testFormGroup = mock(FormGroup.class);
        fieldNames = new String[]{"phenotype", "negative_phenotype"};
    }

    @Test
    public void addAndGetCustomElements(){
        Assert.assertNotNull(testFormSection.addCustomElement(testFormField));
        Assert.assertNotNull(testFormSection.addCustomElement(testFormGroup));
        Assert.assertNotNull(testFormSection.getCustomElements());
    }

    @Test
    public void setCustomElements(){
        FormGroup customElementFormGroup = mock(FormGroup.class);
        testFormSection.setCustomElements(customElementFormGroup);
        Assert.assertEquals(customElementFormGroup, testFormSection.getCustomElements());
    }

    @Test
    public void displaySectionInEditMode() {
        Assert.assertNotEquals("", testFormSection.display(DisplayMode.Edit, fieldNames));
        Assert.assertEquals("", testFormSection.display(DisplayMode.View, fieldNames));

        testFormSection.addCustomElement(testFormField);
        String expectedCustomDisplay = "<div class='phenotype-group' style='" + "display:none"
            + "'><h3 id='Htitle'><span>" + title + "</span></h3><div class='phenotype-main predefined-entries'></div>"
            + "<div class='phenotype-other custom-entries'><div class=\"custom-display-data\">null</div>"
            + "<label for='phenotype_0." + "\\d+" + "' class='label-other label-other-phenotype'>Other</label>"
            + "<input type='text' name='phenotype' class='suggested multi suggest-hpo generateYesNo accept-value'"
            + " value='' size='16' id='phenotype_0." + "\\d+" + "' placeholder='enter free text and choose among "
            + "suggested ontology terms'/><input type='hidden' value='' name='_category'/></div></div>";

        String customDisplayResult = testFormSection.display(DisplayMode.Edit, fieldNames);
        Assert.assertTrue(customDisplayResult.matches(expectedCustomDisplay));

        testFormSection.addElement(testFormGroup);
        String expectedDisplay = "<div class='phenotype-group' style=''><h3 id='Htitle'><span>" + title + "</span></h3>"
            + "<div class='phenotype-main predefined-entries'>null</div><div class='phenotype-other custom-entries'>"
            + "<div class=\"custom-display-data\">null</div><label for='phenotype_0." + "\\d+" + "' class='label-other "
            + "label-other-phenotype'>Other</label><input type='text' name='phenotype' class='suggested multi suggest-hpo "
            + "generateYesNo accept-value' value='' size='16' id='phenotype_0." + "\\d+" + "' placeholder='enter free "
            + "text and choose among suggested ontology terms'/><input type='hidden' value='' name='_category'/></div>"
            + "</div>";
        String displayResult = testFormSection.display(DisplayMode.Edit, fieldNames);
        Assert.assertTrue(displayResult.matches(expectedDisplay));
    }

    @Test
    public void displaySectionInViewMode(){
        testFormSection.addElement(testFormField);
        String expectedElementDisplay = "<div class='phenotype-group' style=''><h3 id='Htitle'><span>" + title + "</span></h3>"
            + "<div class='phenotype-main predefined-entries'>null</div><div class='phenotype-other custom-entries'>"
            + "</div></div>";
        Assert.assertEquals(expectedElementDisplay, testFormSection.display(DisplayMode.View, fieldNames));

        testFormSection.addCustomElement(testFormGroup);
        String expectedCustomElementDisplay = "<div class='phenotype-group' style=''><h3 id='Htitle'><span>" + title
            + "</span></h3><div class='phenotype-main predefined-entries'>null</div><div class='phenotype-other "
            + "custom-entries'><div class=\"custom-display-data\">null</div></div></div>";
        Assert.assertEquals(expectedCustomElementDisplay, testFormSection.display(DisplayMode.View, fieldNames));
    }
}
