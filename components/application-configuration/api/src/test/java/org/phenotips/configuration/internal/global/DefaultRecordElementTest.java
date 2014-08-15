/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.configuration.internal.global;

import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;

import org.xwiki.uiextension.UIExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link RecordElement} implementation, {@link DefaultRecordElement}.
 *
 * @version $Id$
 */
public class DefaultRecordElementTest
{
    /** Basic tests for {@link RecordElement#getExtension()}. */
    @Test
    public void getExtension()
    {
        UIExtension extension = mock(UIExtension.class);
        RecordElement s = new DefaultRecordElement(extension, null);
        Assert.assertSame(extension, s.getExtension());

        s = new DefaultRecordElement(null, null);
        Assert.assertNull(s.getExtension());
    }

    /** {@link RecordElement#getName()} returns the title set in the properties. */
    @Test
    public void getName()
    {
        UIExtension extension = mock(UIExtension.class);
        Map<String, String> params = new HashMap<String, String>();
        params.put("title", "Age of onset");
        when(extension.getParameters()).thenReturn(params);
        RecordElement s = new DefaultRecordElement(extension, null);
        Assert.assertEquals("Age of onset", s.getName());
    }

    /** {@link RecordElement#getName()} returns the last part of the ID. */
    @Test
    public void getNameWithMissingTitle()
    {
        UIExtension extension = mock(UIExtension.class);
        when(extension.getParameters()).thenReturn(Collections.<String, String>emptyMap());
        when(extension.getId()).thenReturn("org.phenotips.patientSheet.field.exam_date");
        RecordElement s = new DefaultRecordElement(extension, null);
        Assert.assertEquals("Exam date", s.getName());
    }

    /** {@link RecordElement#isEnabled()} returns false only when explicitly disabled in the properties. */
    @Test
    public void isEnabled()
    {
        UIExtension extension = mock(UIExtension.class);
        Map<String, String> params = new HashMap<String, String>();
        when(extension.getParameters()).thenReturn(params);
        RecordElement s = new DefaultRecordElement(extension, null);
        Assert.assertTrue(s.isEnabled());

        params.put("enabled", "");
        Assert.assertTrue(s.isEnabled());

        params.put("enabled", "true");
        Assert.assertTrue(s.isEnabled());

        params.put("enabled", "false");
        Assert.assertFalse(s.isEnabled());
    }

    /** {@link RecordElement#getDisplayedFields()} returns the fields listed in the extension "fields" property. */
    @Test
    public void getDisplayedFields()
    {
        UIExtension extension = mock(UIExtension.class);
        Map<String, String> params = new HashMap<String, String>();
        params.put("fields", ",first_name ,, last_name,");
        when(extension.getParameters()).thenReturn(params);
        RecordElement s = new DefaultRecordElement(extension, null);
        List<String> result = s.getDisplayedFields();
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("first_name", result.get(0));
        Assert.assertEquals("last_name", result.get(1));
    }

    /** {@link RecordElement#getDisplayedFields()} returns an empty list when there's no "fields" property. */
    @Test
    public void getDisplayedFieldsWithMissingProperty()
    {
        UIExtension extension = mock(UIExtension.class);
        when(extension.getParameters()).thenReturn(Collections.<String, String>emptyMap());
        RecordElement s = new DefaultRecordElement(extension, null);
        List<String> result = s.getDisplayedFields();
        Assert.assertTrue(result.isEmpty());
    }

    /** {@link RecordElement#getContainingSection()} returns the passed section. */
    @Test
    public void getContainingSection()
    {
        UIExtension extension = mock(UIExtension.class);
        RecordSection section = mock(RecordSection.class);
        RecordElement s = new DefaultRecordElement(extension, section);
        Assert.assertSame(section, s.getContainingSection());
    }

    /** {@link RecordElement#toString()} returns the title set in the properties. */
    @Test
    public void toStringTest()
    {
        UIExtension extension = mock(UIExtension.class);
        Map<String, String> params = new HashMap<String, String>();
        params.put("title", "Age of onset");
        when(extension.getParameters()).thenReturn(params);
        RecordElement s = new DefaultRecordElement(extension, null);
        Assert.assertEquals("Age of onset", s.toString());
    }
}
