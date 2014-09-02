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
public class DefaultRecordSectionTest
{
    /** Basic tests for {@link RecordSection#getExtension()}. */
    @Test
    public void getExtension()
    {
        UIExtension extension = mock(UIExtension.class);
        RecordSection s = new DefaultRecordSection(extension, null, null);
        Assert.assertSame(extension, s.getExtension());

        s = new DefaultRecordSection(null, null, null);
        Assert.assertNull(s.getExtension());
    }

    /** {@link RecordSection#getName()} returns the title set in the properties. */
    @Test
    public void getName()
    {
        UIExtension extension = mock(UIExtension.class);
        Map<String, String> params = new HashMap<String, String>();
        params.put("title", "Patient Information");
        when(extension.getParameters()).thenReturn(params);
        RecordSection s = new DefaultRecordSection(extension, null, null);
        Assert.assertEquals("Patient Information", s.getName());
    }

    /** {@link RecordSection#getName()} returns the last part of the ID. */
    @Test
    public void getNameWithMissingTitle()
    {
        UIExtension extension = mock(UIExtension.class);
        when(extension.getParameters()).thenReturn(Collections.<String, String>emptyMap());
        when(extension.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSection s = new DefaultRecordSection(extension, null, null);
        Assert.assertEquals("Patient info", s.getName());
    }

    /** {@link RecordSection#isEnabled()} returns false only when explicitly disabled in the properties. */
    @Test
    public void isEnabled()
    {
        UIExtension extension = mock(UIExtension.class);
        Map<String, String> params = new HashMap<String, String>();
        when(extension.getParameters()).thenReturn(params);
        RecordSection s = new DefaultRecordSection(extension, null, null);
        Assert.assertTrue(s.isEnabled());

        params.put("enabled", "");
        Assert.assertTrue(s.isEnabled());

        params.put("enabled", "true");
        Assert.assertTrue(s.isEnabled());

        params.put("enabled", "false");
        Assert.assertFalse(s.isEnabled());
    }

    /** {@link RecordSection#getEnabledElements()} returns only enabled fields. */
    @Test
    public void getEnabledElements() throws ComponentLookupException
    {
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(ex.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSection s = new DefaultRecordSection(ex, m, filter);

        Map<String, String> params;
        List<UIExtension> fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("1");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("0");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "false");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("invalid");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("3");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("2");
        fields.add(ex);

        when(m.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        List<RecordElement> result = s.getEnabledElements();
        Assert.assertEquals(4, result.size());
        for (int i = 0; i < 4; ++i) {
            Assert.assertEquals(String.valueOf(i), result.get(i).getExtension().getId());
        }
    }

    /** {@link RecordSection#getAllElements()} returns all the extensions registered under this section. */
    @Test
    public void getAllElements() throws ComponentLookupException
    {
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(ex.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSection s = new DefaultRecordSection(ex, m, filter);

        Map<String, String> params;
        List<UIExtension> fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("1");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("0");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("enabled", "false");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("2");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("4");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("3");
        fields.add(ex);

        when(m.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        List<RecordElement> result = s.getAllElements();
        Assert.assertEquals(5, result.size());
        for (int i = 0; i < 5; ++i) {
            Assert.assertEquals(String.valueOf(i), result.get(i).getExtension().getId());
        }
    }

    /** {@link RecordSection#toString()} shows the section name and the list of enabled elements. */
    @Test
    public void toStringTest() throws ComponentLookupException
    {
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        Map<String, String> params = new HashMap<String, String>();
        params.put("title", "Patient information");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSection s = new DefaultRecordSection(ex, m, filter);

        List<UIExtension> fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Name");
        params.put("enabled", "");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Identifier");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Age");
        params.put("enabled", "false");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        when(ex.getParameters()).thenReturn(params);
        params.put("title", "Indication for referral");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Onset");
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        Assert.assertEquals("Patient information [Identifier, Name, Onset, Indication for referral]", s.toString());
    }
}
