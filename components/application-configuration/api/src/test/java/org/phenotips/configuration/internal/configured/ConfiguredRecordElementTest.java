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
package org.phenotips.configuration.internal.configured;

import org.phenotips.configuration.RecordElement;

import org.xwiki.uiextension.UIExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the configurable {@link RecordElement} implementation, {@link ConfiguredRecordElement}.
 *
 * @version $Id$
 */
public class ConfiguredRecordElementTest
{
    /** {@link RecordElement#isEnabled()} returns false when not listed in the group active fields. */
    @Test
    public void isEnabled()
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtension extension = mock(UIExtension.class);
        Map<String, String> params = new HashMap<>();
        when(extension.getId()).thenReturn("age");
        when(extension.getParameters()).thenReturn(params);
        RecordElement s = new ConfiguredRecordElement(cc, extension, null);

        when(cc.getFieldsOverride()).thenReturn(Collections.singletonList("name"));
        params.put("enabled", "true");
        Assert.assertFalse(s.isEnabled());

        when(cc.getFieldsOverride()).thenReturn(Collections.singletonList("age"));
        params.put("enabled", "false");
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordElement#isEnabled()} returns the global value when no element is listed in the configuration. */
    @Test
    public void isEnabledWithNoOverrides()
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtension extension = mock(UIExtension.class);
        Map<String, String> params = new HashMap<>();
        when(extension.getId()).thenReturn("age");
        when(extension.getParameters()).thenReturn(params);
        RecordElement s = new ConfiguredRecordElement(cc, extension, null);

        when(cc.getFieldsOverride()).thenReturn(Collections.<String>emptyList());
        params.put("enabled", "true");
        Assert.assertTrue(s.isEnabled());

        when(cc.getFieldsOverride()).thenReturn(null);
        params.put("enabled", "false");
        Assert.assertFalse(s.isEnabled());
    }

    /** {@link RecordElement#toString()} returns the title set in the properties. */
    @Test
    public void toStringTest()
    {
        UIExtension extension = mock(UIExtension.class);
        Map<String, String> params = new HashMap<>();
        params.put("title", "Age of onset");
        when(extension.getParameters()).thenReturn(params);
        RecordElement s = new ConfiguredRecordElement(null, extension, null);
        Assert.assertEquals("Age of onset", s.toString());
    }
}
