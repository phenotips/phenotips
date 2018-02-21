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
package org.phenotips.entities.configuration.spi;

import org.phenotips.entities.configuration.RecordElement;
import org.phenotips.entities.configuration.RecordSection;
import org.phenotips.entities.configuration.RecordSectionOption;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link RecordSection} implementation, {@link StaticRecordSection}.
 *
 * @version $Id$
 */
public class StaticRecordSectionTest
{
    private static final String NAME = "Section name";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private UIExtension uiExtension;

    @Mock
    private UIExtensionManager uixManager;

    @Mock
    private UIExtensionFilter orderFilter;

    private List<RecordElement> elements;

    @Mock
    private RecordElement element1;

    @Mock
    private RecordElement element2;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.elements = Arrays.asList(this.element1, this.element2);
        when(this.element1.getName()).thenReturn("Element 1");
        when(this.element1.toString()).thenReturn("Element 1");
        when(this.element1.isEnabled()).thenReturn(false);
        when(this.element2.getName()).thenReturn("Element 2");
        when(this.element2.toString()).thenReturn("Element 2");
        when(this.element2.isEnabled()).thenReturn(true);
    }

    /** {@link RecordSection#getName()} returns the specified title. */
    @Test
    public void getName()
    {
        RecordSection s = new StaticRecordSection(NAME, true, null, null, this.elements);
        Assert.assertEquals(NAME, s.getName());
    }

    /** {@link RecordSection#getName()} returns null if title set to null. */
    @Test
    public void getNameWithNullName()
    {
        RecordSection s = new StaticRecordSection(null, true, null, null, this.elements);
        Assert.assertNull(s.getName());
    }

    /** {@link RecordSection#isEnabled()} returns specified value. */
    @Test
    public void isEnabledReturnsSpecifiedValue()
    {
        RecordSection s = new StaticRecordSection(NAME, true, null, null, this.elements);
        Assert.assertTrue(s.isEnabled());

        s = new StaticRecordSection(NAME, false, null, null, this.elements);
        Assert.assertFalse(s.isEnabled());
    }

    /** {@link RecordSection#getEnabledElements()} returns only enabled fields. */
    @Test
    public void getEnabledElements() throws ComponentLookupException
    {
        RecordSection s = new StaticRecordSection(NAME, true, null, null, this.elements);

        List<RecordElement> result = s.getEnabledElements();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("Element 2", result.get(0).getName());
    }

    /** {@link RecordSection#getAllElements()} returns all the extensions registered under this section. */
    @Test
    public void getAllElements() throws ComponentLookupException
    {
        RecordSection s = new StaticRecordSection(NAME, true, null, null, this.elements);
        List<RecordElement> result = s.getAllElements();
        Assert.assertEquals(2, result.size());
        for (int i = 0; i < 2; ++i) {
            Assert.assertEquals("Element " + (i + 1), result.get(i).getName());
        }
    }

    @Test
    public void getOptionsReturnsEmptyOptionsByDefault()
    {
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, null, null, null);
        Assert.assertNotNull(e.getOptions());
        Assert.assertTrue(e.getOptions().isEmpty());
    }

    @Test
    public void getOptionsReturnsSpecifiedOptions()
    {
        EnumSet<RecordSectionOption> options = EnumSet.of(RecordSectionOption.EXPANDED_BY_DEFAULT);
        RecordSection e = new StaticRecordSection(NAME, true, options, null, null);
        Assert.assertEquals(options, e.getOptions());
    }

    @Test
    public void getOptionsCopiesInput()
    {
        EnumSet<RecordSectionOption> options = EnumSet.of(RecordSectionOption.EXPANDED_BY_DEFAULT);
        RecordSection e = new StaticRecordSection(NAME, true, options, null, null);
        Assert.assertEquals(options, e.getOptions());
        options.remove(RecordSectionOption.EXPANDED_BY_DEFAULT);
        Assert.assertNotEquals(options, e.getOptions());
    }

    @Test
    public void getOptionsCopiesOutput()
    {
        EnumSet<RecordSectionOption> options = EnumSet.of(RecordSectionOption.EXPANDED_BY_DEFAULT);
        RecordSection e = new StaticRecordSection(NAME, true, options, null, null);
        EnumSet<RecordSectionOption> result = e.getOptions();
        Assert.assertEquals(result, e.getOptions());
        result.remove(RecordSectionOption.EXPANDED_BY_DEFAULT);
        Assert.assertNotEquals(result, e.getOptions());
        Assert.assertEquals(options, e.getOptions());
    }

    @Test
    public void getParametersReturnsEmptyParametersByDefault()
    {
        RecordSection e = new StaticRecordSection(NAME, true, null, null, null);
        Assert.assertNotNull(e.getParameters());
        Assert.assertTrue(e.getParameters().isEmpty());
    }

    @Test
    public void getParametersReturnsSpecifiedParameters()
    {
        Map<String, String> parameters = Collections.singletonMap("key", "value");
        RecordSection e = new StaticRecordSection(NAME, true, null, parameters, null);
        Assert.assertEquals(parameters, e.getParameters());
    }

    @Test
    public void getParametersCopiesInput()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", "value");
        RecordSection e = new StaticRecordSection(NAME, true, null, parameters, null);
        Assert.assertEquals(parameters, e.getParameters());
        parameters.put("newKey", "newValue");
        Assert.assertNotEquals(parameters, e.getParameters());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getParametersReturnsReadOnlyMap()
    {
        RecordSection e = new StaticRecordSection(NAME, true, null, Collections.singletonMap("key", "value"), null);
        Map<String, String> result = e.getParameters();
        result.put("newKey", "newValue");
    }

    /** {@link RecordSection#toString()} shows the section name and the list of enabled elements. */
    @Test
    public void toStringIsSpecifiedNameAndListOfEnabledElements() throws ComponentLookupException
    {
        RecordSection s = new StaticRecordSection(NAME, true, null, null, this.elements);

        Assert.assertEquals("Section name [Element 2]", s.toString());
    }
}
