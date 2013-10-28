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

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.internal.global.GlobalRecordConfiguration;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
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
 * Tests for the default {@link RecordConfiguration} implementation, {@link GlobalRecordConfiguration}.
 * 
 * @version $Id$
 */
public class ConfiguredRecordConfigurationTest
{
    /** {@link RecordConfiguration#getEnabledSections()} lists only the enabled sections. */
    @Test
    public void getEnabledSections() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        Execution e = mock(Execution.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, e, m, filter);

        Map<String, String> params;
        List<UIExtension> sections = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("patient-info");
        params = new HashMap<String, String>();
        params.put("title", "Patient information");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("family-info");
        params = new HashMap<String, String>();
        params.put("title", "Family history");
        params.put("enabled", "false");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("prenatal-info");
        params = new HashMap<String, String>();
        params.put("title", "Prenatal history");
        params.put("enabled", "");
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("phenotype-info");
        params = new HashMap<String, String>();
        params.put("title", "Clinical observations");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        when(m.get("org.phenotips.patientSheet.content")).thenReturn(sections);
        List<UIExtension> sorted = realFilter.filter(sections, "order");
        when(filter.filter(sections, "order")).thenReturn(sorted);

        List<String> configuredSections = new LinkedList<String>();
        configuredSections.add("patient-info");
        configuredSections.add("diagnosis-info");
        configuredSections.add("phenotype-info");
        configuredSections.add("family-info");
        when(cc.getSectionsOverride()).thenReturn(configuredSections);

        List<RecordSection> result = c.getEnabledSections();
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Patient information", result.get(0).getName());
        Assert.assertEquals("Clinical observations", result.get(1).getName());
        Assert.assertEquals("Family history", result.get(2).getName());
    }

    /** {@link RecordConfiguration#getEnabledSections()} lists only the enabled sections. */
    @Test
    public void getEnabledSectionsWithNoOverride() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        Execution e = mock(Execution.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, e, m, filter);

        Map<String, String> params;
        List<UIExtension> sections = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("patient-info");
        params = new HashMap<String, String>();
        params.put("title", "Patient information");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("family-info");
        params = new HashMap<String, String>();
        params.put("title", "Family history");
        params.put("enabled", "false");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("prenatal-info");
        params = new HashMap<String, String>();
        params.put("title", "Prenatal history");
        params.put("enabled", "");
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("phenotype-info");
        params = new HashMap<String, String>();
        params.put("title", "Clinical observations");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        when(m.get("org.phenotips.patientSheet.content")).thenReturn(sections);
        List<UIExtension> sorted = realFilter.filter(sections, "order");
        when(filter.filter(sections, "order")).thenReturn(sorted);

        when(cc.getSectionsOverride()).thenReturn(Collections.<String> emptyList());
        List<RecordSection> result = c.getEnabledSections();
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Patient information", result.get(0).getName());
        Assert.assertEquals("Clinical observations", result.get(1).getName());
        Assert.assertEquals("Prenatal history", result.get(2).getName());
    }

    /** {@link RecordConfiguration#getAllSections()} lists all the sections, in order. */
    @Test
    public void getAllSections() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        Execution e = mock(Execution.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, e, m, filter);

        Map<String, String> params;
        List<UIExtension> sections = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("patient-info");
        params = new HashMap<String, String>();
        params.put("title", "Patient information");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("family-info");
        params = new HashMap<String, String>();
        params.put("title", "Family history");
        params.put("enabled", "false");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("prenatal-info");
        params = new HashMap<String, String>();
        params.put("title", "Prenatal history");
        params.put("enabled", "");
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("phenotype-info");
        params = new HashMap<String, String>();
        params.put("title", "Clinical observations");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        when(m.get("org.phenotips.patientSheet.content")).thenReturn(sections);
        List<UIExtension> sorted = realFilter.filter(sections, "order");
        when(filter.filter(sections, "order")).thenReturn(sorted);

        List<String> configuredSections = new LinkedList<String>();
        configuredSections.add("patient-info");
        configuredSections.add("diagnosis-info");
        configuredSections.add("phenotype-info");
        configuredSections.add("family-info");
        when(cc.getSectionsOverride()).thenReturn(configuredSections);

        List<RecordSection> result = c.getAllSections();
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("Patient information", result.get(0).getName());
        Assert.assertEquals("Clinical observations", result.get(1).getName());
        Assert.assertEquals("Family history", result.get(2).getName());
        Assert.assertEquals("Prenatal history", result.get(3).getName());
    }

    /** Basic tests for {@link RecordConfiguration#getEnabledFieldNames()}. */
    @Test
    public void getEnabledFieldNames() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        Execution e = mock(Execution.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, e, m, filter);

        Map<String, String> params;
        List<UIExtension> sections = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("patient-info");
        params = new HashMap<String, String>();
        params.put("title", "Patient information");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("family-info");
        params = new HashMap<String, String>();
        params.put("title", "Family history");
        params.put("enabled", "false");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("prenatal-info");
        params = new HashMap<String, String>();
        params.put("title", "Prenatal history");
        params.put("enabled", "");
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("phenotype-info");
        params = new HashMap<String, String>();
        params.put("title", "Clinical observations");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        when(m.get("org.phenotips.patientSheet.content")).thenReturn(sections);
        List<UIExtension> sorted = realFilter.filter(sections, "order");
        when(filter.filter(sections, "order")).thenReturn(sorted);

        List<String> configuredSections = new LinkedList<String>();
        configuredSections.add("patient-info");
        configuredSections.add("diagnosis-info");
        configuredSections.add("phenotype-info");
        configuredSections.add("family-info");
        when(cc.getSectionsOverride()).thenReturn(configuredSections);

        List<UIExtension> fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("name");
        params = new HashMap<String, String>();
        params.put("fields", ",first_name ,, last_name,");
        params.put("enabled", "");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        when(ex.getId()).thenReturn("external_id");
        params.put("fields", "external_id");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("date_of_birth");
        params = new HashMap<String, String>();
        params.put("fields", "date_of_birth,");
        params.put("enabled", "false");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("gender");
        params = new HashMap<String, String>();
        params.put("fields", "gender");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("patient-info")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("ethnicity");
        params = new HashMap<String, String>();
        params.put("fields", "maternal_ethnicity,paternal_ethnicity");
        params.put("enabled", "true");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("family-info")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("gestation");
        params = new HashMap<String, String>();
        params.put("fields", "gestation");
        params.put("enabled", "true");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("prenatal-info")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("phenotype");
        params = new HashMap<String, String>();
        params.put("fields", "unaffected,phenotype,negative_phenotype");
        params.put("enabled", "true");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("phenotype-info")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        List<String> configuredFields = new LinkedList<String>();
        configuredFields.add("date_of_birth");
        configuredFields.add("ethnicity");
        configuredFields.add("external_id");
        configuredFields.add("phenotype");
        when(cc.getFieldsOverride()).thenReturn(configuredFields);

        List<String> expectedFields = new LinkedList<String>();
        expectedFields.add("date_of_birth");
        expectedFields.add("external_id");
        expectedFields.add("unaffected");
        expectedFields.add("phenotype");
        expectedFields.add("negative_phenotype");
        expectedFields.add("maternal_ethnicity");
        expectedFields.add("paternal_ethnicity");
        Assert.assertEquals(expectedFields, c.getEnabledFieldNames());
    }
}
