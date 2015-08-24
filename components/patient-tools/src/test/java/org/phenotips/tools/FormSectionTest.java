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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class FormSectionTest
{
    private FormSection testFormSection;

    private FormField testFormField;

    private FormGroup testFormGroup;

    private String[] fieldNames;

    String title = "title";

    String propertyName = "phenotype";

    @Before
    public void setUp()
    {
        List<String> categories = new LinkedList<String>();
        this.testFormSection = new FormSection(this.title, this.propertyName, categories);
        this.testFormField = mock(FormField.class);
        this.testFormGroup = mock(FormGroup.class);
        this.fieldNames = new String[] { "phenotype", "negative_phenotype" };
    }

    @Test
    public void addAndGetCustomElements()
    {
        Assert.assertNotNull(this.testFormSection.addCustomElement(this.testFormField));
        Assert.assertNotNull(this.testFormSection.addCustomElement(this.testFormGroup));
        Assert.assertNotNull(this.testFormSection.getCustomElements());
    }

    @Test
    public void setCustomElements()
    {
        FormGroup customElementFormGroup = mock(FormGroup.class);
        this.testFormSection.setCustomElements(customElementFormGroup);
        Assert.assertEquals(customElementFormGroup, this.testFormSection.getCustomElements());
    }

    @Test
    public void displaySectionInEditMode()
    {
        Assert.assertNotEquals("", this.testFormSection.display(DisplayMode.Edit, this.fieldNames));
        Assert.assertEquals("", this.testFormSection.display(DisplayMode.View, this.fieldNames));

        this.testFormSection.addCustomElement(this.testFormField);
        String expectedCustomDisplay = "<div class='phenotype-group' style='" + "display:none"
            + "'><h3 id='Htitle'><span>" + this.title
            + "</span></h3><div class='phenotype-main predefined-entries'></div>"
            + "<div class='phenotype-other custom-entries'><div class=\"custom-display-data\">null</div>"
            + "<label for='phenotype_0." + "\\d+" + "' class='label-other label-other-phenotype'>Other</label>"
            + "<input type='text' name='phenotype' class='suggested multi suggest-hpo generateYesNo accept-value'"
            + " value='' size='16' id='phenotype_0." + "\\d+" + "' placeholder='enter free text and choose among "
            + "suggested ontology terms'/><input type='hidden' value='' name='_category'/></div></div>";

        String customDisplayResult = this.testFormSection.display(DisplayMode.Edit, this.fieldNames);
        Assert.assertTrue(customDisplayResult.matches(expectedCustomDisplay));

        this.testFormSection.addElement(this.testFormGroup);
        String expectedDisplay =
            "<div class='phenotype-group' style=''><h3 id='Htitle'><span>" + this.title + "</span></h3>"
                + "<div class='phenotype-main predefined-entries'>null</div><div class='phenotype-other custom-entries'>"
                + "<div class=\"custom-display-data\">null</div><label for='phenotype_0." + "\\d+"
                + "' class='label-other "
                + "label-other-phenotype'>Other</label><input type='text' name='phenotype' class='suggested multi suggest-hpo "
                + "generateYesNo accept-value' value='' size='16' id='phenotype_0." + "\\d+"
                + "' placeholder='enter free "
                + "text and choose among suggested ontology terms'/><input type='hidden' value='' name='_category'/></div>"
                + "</div>";
        String displayResult = this.testFormSection.display(DisplayMode.Edit, this.fieldNames);
        Assert.assertTrue(displayResult.matches(expectedDisplay));
    }

    @Test
    public void displaySectionInViewMode()
    {
        this.testFormSection.addElement(this.testFormField);
        String expectedElementDisplay =
            "<div class='phenotype-group' style=''><h3 id='Htitle'><span>" + this.title + "</span></h3>"
                + "<div class='phenotype-main predefined-entries'>null</div><div class='phenotype-other custom-entries'>"
                + "</div></div>";
        Assert.assertEquals(expectedElementDisplay, this.testFormSection.display(DisplayMode.View, this.fieldNames));

        this.testFormSection.addCustomElement(this.testFormGroup);
        String expectedCustomElementDisplay =
            "<div class='phenotype-group' style=''><h3 id='Htitle'><span>" + this.title
                + "</span></h3><div class='phenotype-main predefined-entries'>null</div><div class='phenotype-other "
                + "custom-entries'><div class=\"custom-display-data\">null</div></div></div>";
        Assert.assertEquals(expectedCustomElementDisplay,
            this.testFormSection.display(DisplayMode.View, this.fieldNames));
    }

    @Test
    public void getTitleReturnsSpecifiedTitle()
    {
        FormSection testSection = new FormSection(null, this.propertyName, null);
        Assert.assertNull(testSection.getTitle());

        testSection = new FormSection("", this.propertyName, null);
        Assert.assertEquals("", testSection.getTitle());

        testSection = new FormSection(this.title, this.propertyName, null);
        Assert.assertEquals(this.title, testSection.getTitle());
    }

    @Test
    public void getCategoriesReturnsSpecifiedCategories()
    {
        FormSection testSection = new FormSection(this.title, this.propertyName, null);
        Assert.assertTrue(testSection.getCategories().isEmpty());

        testSection = new FormSection(this.title, this.propertyName, Collections.<String>emptySet());
        Assert.assertTrue(testSection.getCategories().isEmpty());

        testSection = new FormSection(this.title, this.propertyName, Collections.singleton("category"));
        Assert.assertEquals(Collections.singletonList("category"), testSection.getCategories());
    }
}
