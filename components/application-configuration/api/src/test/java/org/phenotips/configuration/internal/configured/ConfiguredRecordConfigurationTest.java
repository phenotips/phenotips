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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.internal.global.GlobalRecordConfiguration;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;
import org.xwiki.uiextension.internal.filter.SortByParameterFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link RecordConfiguration} implementation, {@link GlobalRecordConfiguration}.
 *
 * @version $Id$
 */
public class ConfiguredRecordConfigurationTest
{
    @Mock
    private Provider<XWikiContext> xcp;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
    }

    /** {@link RecordConfiguration#getEnabledSections()} lists only the enabled sections. */
    @Test
    public void getEnabledSections() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, this.xcp, m, filter);

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
        params.put("title", "Family history and pedigree");
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
        Assert.assertEquals("Family history and pedigree", result.get(2).getName());
    }

    /** {@link RecordConfiguration#getEnabledSections()} lists only the enabled sections. */
    @Test
    public void getEnabledSectionsWithNoOverride() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, this.xcp, m, filter);

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
        params.put("title", "Family history and pedigree");
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

        when(cc.getSectionsOverride()).thenReturn(Collections.<String>emptyList());
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
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, this.xcp, m, filter);

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
        params.put("title", "Family history and pedigree");
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
        Assert.assertEquals("Family history and pedigree", result.get(2).getName());
        Assert.assertEquals("Prenatal history", result.get(3).getName());
    }

    /** Basic tests for {@link RecordConfiguration#getEnabledFieldNames()}. */
    @Test
    public void getEnabledFieldNames() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, this.xcp, m, filter);

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
        params.put("title", "Family history and pedigree");
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

    /** Basic tests for {@link ConfiguredRecordConfiguration#getPhenotypeMapping()}. */
    @Test
    public void getPhenotypeMapping() throws ComponentLookupException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, this.xcp, m, filter);
        when(cc.getPhenotypeMapping()).thenReturn("PhenoTips.XPhenotypeMapping");
        ComponentManager cm = mock(ComponentManager.class);
        @SuppressWarnings("unchecked")
        Provider<ComponentManager> mockProvider = mock(Provider.class);
        // This is a bit fragile, let's hope the field name doesn't change
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", mockProvider);
        when(mockProvider.get()).thenReturn(cm);
        @SuppressWarnings("unchecked")
        DocumentReferenceResolver<String> resolver = mock(DocumentReferenceResolver.class);
        when(cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current")).thenReturn(resolver);
        DocumentReference expectedMapping = new DocumentReference("xwiki", "PhenoTips", "XPhenotypeMapping");
        when(resolver.resolve("PhenoTips.XPhenotypeMapping")).thenReturn(expectedMapping);

        Assert.assertEquals(expectedMapping, c.getPhenotypeMapping());
    }

    /**
     * {@link ConfiguredRecordConfiguration#getPhenotypeMapping()} returns the global mapping when there's no custom
     * mapping.
     */
    @Test
    public void getPhenotypeMappingWithMissingConfiguration() throws ComponentLookupException, XWikiException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, this.xcp, m, filter);
        when(cc.getPhenotypeMapping()).thenReturn("");
        ComponentManager cm = mock(ComponentManager.class);
        @SuppressWarnings("unchecked")
        Provider<ComponentManager> mockProvider = mock(Provider.class);
        // This is a bit fragile, let's hope the field name doesn't change
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", mockProvider);
        when(mockProvider.get()).thenReturn(cm);
        @SuppressWarnings("unchecked")
        DocumentReferenceResolver<String> resolver = mock(DocumentReferenceResolver.class);
        when(cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current")).thenReturn(resolver);
        DocumentReference expectedMapping = new DocumentReference("xwiki", "PhenoTips", "XPhenotypeMapping");
        when(resolver.resolve("PhenoTips.XPhenotypeMapping")).thenReturn(expectedMapping);

        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Matchers.any(EntityReference.class), Matchers.same(context))).thenReturn(wh);
        BaseObject o = mock(BaseObject.class);
        when(wh.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getStringValue("phenotypeMapping")).thenReturn("PhenoTips.XPhenotypeMapping");

        Assert.assertEquals(expectedMapping, c.getPhenotypeMapping());
    }

    /**
     * {@link ConfiguredRecordConfiguration#getPhenotypeMapping()} returns the global mapping when getting the custom
     * mapping fails.
     */
    @Test
    public void getPhenotypeMappingWithExceptions() throws ComponentLookupException, XWikiException
    {
        CustomConfiguration cc = mock(CustomConfiguration.class);
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        RecordConfiguration c = new ConfiguredRecordConfiguration(cc, this.xcp, m, filter);
        when(cc.getPhenotypeMapping()).thenReturn("PhenoTips.YPhenotypeMapping");
        ComponentManager cm = mock(ComponentManager.class);
        @SuppressWarnings("unchecked")
        Provider<ComponentManager> mockProvider = mock(Provider.class);
        // This is a bit fragile, let's hope the field name doesn't change
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", mockProvider);
        when(mockProvider.get()).thenReturn(cm);
        @SuppressWarnings("unchecked")
        DocumentReferenceResolver<String> resolver = mock(DocumentReferenceResolver.class);
        when(cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current")).thenThrow(
            new ComponentLookupException("No such component")).thenReturn(resolver);
        DocumentReference expectedMapping = new DocumentReference("xwiki", "PhenoTips", "XPhenotypeMapping");
        when(resolver.resolve("PhenoTips.YPhenotypeMapping")).thenReturn(null);
        when(resolver.resolve("PhenoTips.XPhenotypeMapping")).thenReturn(expectedMapping);

        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Matchers.any(EntityReference.class), Matchers.same(context))).thenReturn(wh);
        BaseObject o = mock(BaseObject.class);
        when(wh.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getStringValue("phenotypeMapping")).thenReturn("PhenoTips.XPhenotypeMapping");

        Assert.assertEquals(expectedMapping, c.getPhenotypeMapping());
    }
}
