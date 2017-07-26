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
package org.phenotips.configuration.spi;

import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.uiextension.UIExtension;

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
 * Tests for the default {@link RecordElement} implementation, {@link UIXRecordElement}.
 *
 * @version $Id$
 */
public class UIXRecordElementTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private RecordSection recordSection;

    @Mock
    private UIExtension uiExtension;

    private Map<String, String> parameters = new HashMap<>();

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        when(this.uiExtension.getParameters()).thenReturn(this.parameters);
    }

    /** Basic tests for {@link RecordElement#getExtension()}. */
    @Test
    public void getExtension()
    {
        RecordElement s = new UIXRecordElement(this.uiExtension, this.recordSection);
        Assert.assertSame(this.uiExtension, s.getExtension());
    }

    /** Basic test to affirm that passing in a null extension will result in an exception. */
    @Test
    public void nullExtensionThrowsException()
    {
        this.thrown.expect(IllegalArgumentException.class);
        new UIXRecordElement(null, this.recordSection);
    }

    /** Basic test to affirm that passing in a null record section will result in an exception. */
    @Test
    public void nullSectionThrowsException()
    {
        this.thrown.expect(IllegalArgumentException.class);
        new UIXRecordElement(this.uiExtension, null);
    }

    /** {@link RecordElement#getName()} returns the title set in the properties. */
    @Test
    public void getName()
    {
        this.parameters.put("title", "Age of onset");
        RecordElement s = new UIXRecordElement(this.uiExtension, this.recordSection);
        Assert.assertEquals("Age of onset", s.getName());
    }

    /** {@link RecordElement#getName()} returns the last part of the ID. */
    @Test
    public void getNameWithMissingTitle()
    {
        when(this.uiExtension.getId()).thenReturn("org.phenotips.patientSheet.field.exam_date");

        RecordElement s = new UIXRecordElement(this.uiExtension, this.recordSection);
        Assert.assertEquals("Exam date", s.getName());
    }

    /** {@link RecordElement#isEnabled()} returns true when there's no setting in the properties. */
    @Test
    public void isEnabledReturnsTrueForNullSetting()
    {
        RecordElement s = new UIXRecordElement(this.uiExtension, this.recordSection);
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordElement#isEnabled()} returns true when there's no value set in the properties. */
    @Test
    public void isEnabledReturnsTrueForEmptySetting()
    {
        this.parameters.put("enabled", "");
        RecordElement s = new UIXRecordElement(this.uiExtension, this.recordSection);
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordElement#isEnabled()} returns true when set to "true" in the properties. */
    @Test
    public void isEnabledReturnsTrueForTrueSetting()
    {
        this.parameters.put("enabled", "true");
        RecordElement s = new UIXRecordElement(this.uiExtension, this.recordSection);
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordElement#isEnabled()} returns false only when explicitly disabled in the properties. */
    @Test
    public void isEnabledReturnsFalseForFalseSetting()
    {
        this.parameters.put("enabled", "false");
        RecordElement s = new UIXRecordElement(this.uiExtension, this.recordSection);
        Assert.assertFalse(s.isEnabled());
    }

    /** {@link RecordElement#getDisplayedFields()} returns the fields listed in the extension "fields" property. */
    @Test
    public void getDisplayedFields()
    {
        this.parameters.put("fields", ",first_name ,, last_name,");
        final UIXRecordElement element = new UIXRecordElement(this.uiExtension, this.recordSection);

        final List<String> result = element.getDisplayedFields();

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("first_name", result.get(0));
        Assert.assertEquals("last_name", result.get(1));
    }

    /** {@link RecordElement#getDisplayedFields()} returns an empty list when there's no "fields" property. */
    @Test
    public void getDisplayedFieldsWithMissingProperty()
    {
        RecordElement s = new UIXRecordElement(this.uiExtension, this.recordSection);
        List<String> result = s.getDisplayedFields();
        Assert.assertTrue(result.isEmpty());
    }

    /** {@link RecordElement#getContainingSection()} returns the passed section. */
    @Test
    public void getContainingSection()
    {
        RecordElement s = new UIXRecordElement(this.uiExtension, this.recordSection);
        Assert.assertSame(this.recordSection, s.getContainingSection());
    }

    /** {@link RecordElement#toString()} returns the title set in the properties. */
    @Test
    public void toStringTest()
    {
        this.parameters.put("title", "Age of onset");
        RecordElement s = new UIXRecordElement(this.uiExtension, this.recordSection);
        Assert.assertEquals("Age of onset", s.toString());
    }
}
