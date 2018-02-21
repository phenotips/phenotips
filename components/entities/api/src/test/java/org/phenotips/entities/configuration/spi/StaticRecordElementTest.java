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
import org.phenotips.entities.configuration.RecordElementOption;
import org.phenotips.entities.configuration.RecordSection;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.uiextension.UIExtension;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the default {@link RecordElement} implementation, {@link UIXRecordElement}.
 *
 * @version $Id$
 */
public class StaticRecordElementTest
{
    private static final String NAME = "name";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private RecordSection recordSection;

    @Mock
    private UIExtension uiExtension;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
    }

    /** Basic tests for {@link RecordElement#getExtension()}. */
    @Test
    public void getExtensionReturnsSpecifiedExtension()
    {
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, null, null, Collections.emptyList());
        Assert.assertSame(this.uiExtension, e.getExtension());

        e = new StaticRecordElement(null, NAME, true, null, null, Collections.emptyList());
        Assert.assertNull(e.getExtension());
    }

    @Test
    public void getNameReturnsSpecifiedName()
    {
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, null, null, Collections.emptyList());
        Assert.assertEquals(NAME, e.getName());

        e = new StaticRecordElement(this.uiExtension, null, true, null, null, Collections.emptyList());
        Assert.assertNull(e.getName());
    }

    @Test
    public void isEnabledReturnsSpecifiedSetting()
    {
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, null, null, Collections.emptyList());
        Assert.assertTrue(e.isEnabled());

        e = new StaticRecordElement(this.uiExtension, NAME, false, null, null, Collections.emptyList());
        Assert.assertFalse(e.isEnabled());
    }

    @Test
    public void getDisplayedFieldsReturnsSpecifiedFields()
    {
        List<ClassPropertyReference> fields = new LinkedList<>();
        fields.add(new ClassPropertyReference("propertyName", new DocumentReference("wiki", "space", "page")));

        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, null, null, fields);
        Assert.assertEquals(fields, e.getDisplayedFields());
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
        EnumSet<RecordElementOption> options =
            EnumSet.of(RecordElementOption.HARD_MANDATORY, RecordElementOption.SOFT_UNIQUE);
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, options, null, null);
        Assert.assertEquals(options, e.getOptions());
    }

    @Test
    public void getOptionsCopiesInput()
    {
        EnumSet<RecordElementOption> options =
            EnumSet.of(RecordElementOption.HARD_MANDATORY, RecordElementOption.SOFT_UNIQUE);
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, options, null, null);
        Assert.assertEquals(options, e.getOptions());
        options.add(RecordElementOption.READ_ONLY);
        Assert.assertNotEquals(options, e.getOptions());
    }

    @Test
    public void getOptionsCopiesOutput()
    {
        EnumSet<RecordElementOption> options =
            EnumSet.of(RecordElementOption.HARD_MANDATORY, RecordElementOption.SOFT_UNIQUE);
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, options, null, null);
        EnumSet<RecordElementOption> result = e.getOptions();
        Assert.assertEquals(result, e.getOptions());
        result.add(RecordElementOption.READ_ONLY);
        Assert.assertNotEquals(result, e.getOptions());
        Assert.assertEquals(options, e.getOptions());
    }

    @Test
    public void getParametersReturnsEmptyParametersByDefault()
    {
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, null, null, null);
        Assert.assertNotNull(e.getParameters());
        Assert.assertTrue(e.getParameters().isEmpty());
    }

    @Test
    public void getParametersReturnsSpecifiedParameters()
    {
        Map<String, String> parameters = Collections.singletonMap("key", "value");
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, null, parameters, null);
        Assert.assertEquals(parameters, e.getParameters());
    }

    @Test
    public void getParametersCopiesInput()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", "value");
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, null, parameters, null);
        Assert.assertEquals(parameters, e.getParameters());
        parameters.put("newKey", "newValue");
        Assert.assertNotEquals(parameters, e.getParameters());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getParametersReturnsReadOnlyMap()
    {
        RecordElement e =
            new StaticRecordElement(this.uiExtension, NAME, true, null, Collections.singletonMap("key", "value"), null);
        Map<String, String> result = e.getParameters();
        result.put("newKey", "newValue");
    }

    @Test
    public void toStringIsSpecifiedName()
    {
        RecordElement e = new StaticRecordElement(this.uiExtension, NAME, true, null, null, null);
        Assert.assertEquals(NAME, e.toString());
    }
}
