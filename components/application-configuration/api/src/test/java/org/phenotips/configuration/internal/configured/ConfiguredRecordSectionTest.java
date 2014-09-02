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
package org.phenotips.configuration.internal.configured;

import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.internal.global.DefaultRecordSection;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;
import org.xwiki.uiextension.internal.filter.SortByParameterFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link RecordSection} implementation, {@link DefaultRecordSection}.
 *
 * @version $Id$
 */
public class ConfiguredRecordSectionTest
{
    /** {@link RecordSection#isEnabled()} returns false only when explicitly disabled in the properties. */
    @Test
    public void isEnabled()
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtension extension = mock(UIExtension.class);
        Map<String, String> params = new HashMap<String, String>();
        when(extension.getId()).thenReturn("patient-info");
        when(extension.getParameters()).thenReturn(params);
        RecordSection s = new ConfiguredRecordSection(cc, extension, null, null);

        when(cc.getSectionsOverride()).thenReturn(Collections.singletonList("clinical-symptoms"));
        params.put("enabled", "true");
        Assert.assertFalse(s.isEnabled());

        when(cc.getSectionsOverride()).thenReturn(Collections.singletonList("patient-info"));
        Assert.assertTrue(s.isEnabled());

        params.put("enabled", "false");
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordSection#getEnabledElements()} returns only fields listed in the group configuration. */
    @Test
    public void getEnabledElements() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(ex.getId()).thenReturn("patient-info");
        RecordSection s = new ConfiguredRecordSection(cc, ex, m, filter);

        Map<String, String> params;
        List<UIExtension> fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "");
        params.put("title", "Name");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("name");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "true");
        params.put("title", "Identifier");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("identifier");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "false");
        params.put("title", "Global age of onset");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("onset");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Sex");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("gender");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("order", "4");
        params.put("title", "Date of birth");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("date_of_birth");
        fields.add(ex);

        when(m.get("patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        List<String> configuredFields = new LinkedList<String>();
        configuredFields.add("identifier");
        configuredFields.add("date_of_birth");
        configuredFields.add("gender");
        configuredFields.add("ethnicity");
        configuredFields.add("onset");
        when(cc.getFieldsOverride()).thenReturn(configuredFields);

        List<RecordElement> result = s.getEnabledElements();
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("identifier", result.get(0).getExtension().getId());
        Assert.assertEquals("date_of_birth", result.get(1).getExtension().getId());
        Assert.assertEquals("gender", result.get(2).getExtension().getId());
        Assert.assertEquals("onset", result.get(3).getExtension().getId());
    }

    /**
     * {@link RecordSection#getAllElements()} returns all the extensions registered under this section, in the
     * configured order.
     */
    @Test
    public void getAllElements() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(ex.getId()).thenReturn("patient-info");
        RecordSection s = new ConfiguredRecordSection(cc, ex, m, filter);

        Map<String, String> params;
        List<UIExtension> fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "");
        params.put("title", "Name");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("name");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "true");
        params.put("title", "Identifier");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("identifier");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "false");
        params.put("title", "Global age of onset");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("onset");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Sex");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("gender");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("order", "4");
        params.put("title", "Date of birth");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("date_of_birth");
        fields.add(ex);

        when(m.get("patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        List<String> configuredFields = new LinkedList<String>();
        configuredFields.add("identifier");
        configuredFields.add("date_of_birth");
        configuredFields.add("gender");
        configuredFields.add("ethnicity");
        configuredFields.add("onset");
        when(cc.getFieldsOverride()).thenReturn(configuredFields);

        List<RecordElement> result = s.getAllElements();
        Assert.assertEquals(5, result.size());
        Assert.assertEquals("identifier", result.get(0).getExtension().getId());
        Assert.assertEquals("date_of_birth", result.get(1).getExtension().getId());
        Assert.assertEquals("gender", result.get(2).getExtension().getId());
        Assert.assertEquals("onset", result.get(3).getExtension().getId());
        Assert.assertEquals("name", result.get(4).getExtension().getId());
    }

    /**
     * {@link RecordSection#getAllElements()} returns all the extensions registered under this section in the global
     * order when no override is configured.
     */
    @Test
    public void getAllElementsWithNoOverride() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(ex.getId()).thenReturn("patient-info");
        RecordSection s = new ConfiguredRecordSection(cc, ex, m, filter);

        Map<String, String> params;
        List<UIExtension> fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "");
        params.put("title", "Name");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("name");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "true");
        params.put("title", "Identifier");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("identifier");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "false");
        params.put("title", "Global age of onset");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("onset");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Sex");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("gender");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("order", "4");
        params.put("title", "Date of birth");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("date_of_birth");
        fields.add(ex);

        when(m.get("patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        when(cc.getFieldsOverride()).thenReturn(Collections.<String>emptyList());

        List<RecordElement> result = s.getAllElements();
        Assert.assertEquals(5, result.size());
        Assert.assertEquals("identifier", result.get(0).getExtension().getId());
        Assert.assertEquals("name", result.get(1).getExtension().getId());
        Assert.assertEquals("onset", result.get(2).getExtension().getId());
        Assert.assertEquals("date_of_birth", result.get(3).getExtension().getId());
        Assert.assertEquals("gender", result.get(4).getExtension().getId());
    }

    /**
     * {@link RecordSection#getAllElements()} returns all the extensions registered under this section in the global
     * order when the configuration is missing.
     */
    @Test
    public void getAllElementsWithMissingConfiguration() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(ex.getId()).thenReturn("patient-info");
        RecordSection s = new ConfiguredRecordSection(cc, ex, m, filter);

        Map<String, String> params;
        List<UIExtension> fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "");
        params.put("title", "Name");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("name");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "true");
        params.put("title", "Identifier");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("identifier");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "false");
        params.put("title", "Global age of onset");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("onset");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Sex");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("gender");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("order", "4");
        params.put("title", "Date of birth");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("date_of_birth");
        fields.add(ex);

        when(m.get("patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        when(cc.getFieldsOverride()).thenReturn(null);

        List<RecordElement> result = s.getAllElements();
        Assert.assertEquals(5, result.size());
        Assert.assertEquals("identifier", result.get(0).getExtension().getId());
        Assert.assertEquals("name", result.get(1).getExtension().getId());
        Assert.assertEquals("onset", result.get(2).getExtension().getId());
        Assert.assertEquals("date_of_birth", result.get(3).getExtension().getId());
        Assert.assertEquals("gender", result.get(4).getExtension().getId());
    }
}
