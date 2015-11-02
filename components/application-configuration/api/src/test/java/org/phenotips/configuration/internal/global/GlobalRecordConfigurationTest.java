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
package org.phenotips.configuration.internal.global;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordSection;
import org.phenotips.data.Patient;

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
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link RecordConfiguration} implementation, {@link GlobalRecordConfiguration}.
 *
 * @version $Id$
 */
public class GlobalRecordConfigurationTest
{
    @Mock
    private Provider<XWikiContext> xcp;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
    }

    /** {@link GlobalRecordConfiguration#getEnabledSections()} lists only the enabled sections. */
    @Test
    public void getEnabledSections() throws ComponentLookupException
    {
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new GlobalRecordConfiguration(this.xcp, m, filter);

        Map<String, String> params;
        List<UIExtension> sections = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section1");
        params = new HashMap<String, String>();
        params.put("title", "Patient information");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("disabled_section");
        params = new HashMap<String, String>();
        params.put("title", "Family history and pedigree");
        params.put("enabled", "false");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section4");
        params = new HashMap<String, String>();
        params.put("title", "Prenatal history");
        params.put("enabled", "");
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section3");
        params = new HashMap<String, String>();
        params.put("title", "Clinical observations");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        when(m.get("org.phenotips.patientSheet.content")).thenReturn(sections);
        List<UIExtension> sorted = realFilter.filter(sections, "order");
        when(filter.filter(sections, "order")).thenReturn(sorted);

        List<RecordSection> result = c.getEnabledSections();
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Patient information", result.get(0).getName());
        Assert.assertEquals("Clinical observations", result.get(1).getName());
        Assert.assertEquals("Prenatal history", result.get(2).getName());
    }

    /** {@link GlobalRecordConfiguration#getAllSections()} lists all the sections, in order. */
    @Test
    public void getAllSections() throws ComponentLookupException
    {
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new GlobalRecordConfiguration(this.xcp, m, filter);

        Map<String, String> params;
        List<UIExtension> sections = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section1");
        params = new HashMap<String, String>();
        params.put("title", "Patient information");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("disabled_section");
        params = new HashMap<String, String>();
        params.put("title", "Family history and pedigree");
        params.put("enabled", "false");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section4");
        params = new HashMap<String, String>();
        params.put("title", "Prenatal history");
        params.put("enabled", "");
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section3");
        params = new HashMap<String, String>();
        params.put("title", "Clinical observations");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        when(m.get("org.phenotips.patientSheet.content")).thenReturn(sections);
        List<UIExtension> sorted = realFilter.filter(sections, "order");
        when(filter.filter(sections, "order")).thenReturn(sorted);

        List<RecordSection> result = c.getAllSections();
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("Patient information", result.get(0).getName());
        Assert.assertEquals("Family history and pedigree", result.get(1).getName());
        Assert.assertEquals("Clinical observations", result.get(2).getName());
        Assert.assertEquals("Prenatal history", result.get(3).getName());
    }

    /** Basic tests for {@link GlobalRecordConfiguration#getEnabledFieldNames()}. */
    @Test
    public void getEnabledFieldNames() throws ComponentLookupException
    {
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new GlobalRecordConfiguration(this.xcp, m, filter);

        Map<String, String> params;
        List<UIExtension> sections = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class, "section1");
        when(ex.getId()).thenReturn("section1");
        params = new HashMap<String, String>();
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class, "disabled_section");
        when(ex.getId()).thenReturn("disabled_section");
        params = new HashMap<String, String>();
        params.put("enabled", "false");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class, "section3");
        when(ex.getId()).thenReturn("section3");
        params = new HashMap<String, String>();
        params.put("enabled", "");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class, "section4");
        when(ex.getId()).thenReturn("section4");
        params = new HashMap<String, String>();
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        when(m.get("org.phenotips.patientSheet.content")).thenReturn(sections);
        List<UIExtension> sorted = realFilter.filter(sections, "order");
        when(filter.filter(sections, "order")).thenReturn(sorted);

        List<UIExtension> fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("fields", ",first_name ,, last_name,");
        params.put("enabled", "");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("fields", "external_id");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("fields", "date_of_birth,");
        params.put("enabled", "false");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("fields", "gender");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("fields", "");
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("section1")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("fields", "maternal_ethnicity,paternal_ethnicity");
        params.put("enabled", "true");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("disabled_section")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("fields", "gestation");
        params.put("enabled", "true");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("section3")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("fields", "unaffected,phenotype,negative_phenotype");
        params.put("enabled", "true");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("section4")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        List<String> expectedFields = new LinkedList<String>();
        expectedFields.add("external_id");
        expectedFields.add("first_name");
        expectedFields.add("last_name");
        expectedFields.add("gender");
        expectedFields.add("gestation");
        expectedFields.add("unaffected");
        expectedFields.add("phenotype");
        expectedFields.add("negative_phenotype");
        Assert.assertEquals(expectedFields, c.getEnabledFieldNames());
    }

    /** Basic tests for {@link GlobalRecordConfiguration#getAllFieldNames()}. */
    @Test
    public void getAllFieldNames() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(x.getDocument(Patient.CLASS_REFERENCE, context)).thenReturn(doc);
        BaseClass c = mock(BaseClass.class);
        when(doc.getXClass()).thenReturn(c);
        String[] props = new String[] { "external_id", "first_name", "last_name", "gender" };
        when(c.getPropertyNames()).thenReturn(props);

        List<String> expectedFields = new LinkedList<String>();
        expectedFields.add("external_id");
        expectedFields.add("first_name");
        expectedFields.add("last_name");
        expectedFields.add("gender");

        RecordConfiguration config =
            new GlobalRecordConfiguration(this.xcp, mock(UIExtensionManager.class), mock(UIExtensionFilter.class));
        Assert.assertEquals(expectedFields, config.getAllFieldNames());
    }

    /** {@link GlobalRecordConfiguration#getAllFieldNames()} catches exceptions. */
    @Test
    public void getAllFieldNamesWithException() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        when(x.getDocument(Patient.CLASS_REFERENCE, context)).thenThrow(new XWikiException());

        RecordConfiguration config =
            new GlobalRecordConfiguration(this.xcp, mock(UIExtensionManager.class), mock(UIExtensionFilter.class));
        Assert.assertTrue(config.getAllFieldNames().isEmpty());
    }

    /** Basic tests for {@link GlobalRecordConfiguration#getPhenotypeMapping()}. */
    @Test
    public void getPhenotypeMapping() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Matchers.any(EntityReference.class), Matchers.same(context))).thenReturn(wh);
        BaseObject o = mock(BaseObject.class);
        when(wh.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getStringValue("phenotypeMapping")).thenReturn("PhenoTips.XPhenotypeMapping");
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

        RecordConfiguration config =
            new GlobalRecordConfiguration(this.xcp, mock(UIExtensionManager.class), mock(UIExtensionFilter.class));
        Assert.assertEquals(expectedMapping, config.getPhenotypeMapping());
    }

    /** {@link GlobalRecordConfiguration#getPhenotypeMapping()} returns a default mapping with no configuration. */
    @Test
    public void getPhenotypeMappingWithNoConfiguration() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Matchers.any(EntityReference.class), Matchers.same(context))).thenReturn(wh);
        BaseObject o = mock(BaseObject.class);
        when(wh.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getStringValue("phenotypeMapping")).thenReturn(null);
        ComponentManager cm = mock(ComponentManager.class);
        @SuppressWarnings("unchecked")
        Provider<ComponentManager> mockProvider = mock(Provider.class);
        // This is a bit fragile, let's hope the field name doesn't change
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", mockProvider);
        when(mockProvider.get()).thenReturn(cm);
        @SuppressWarnings("unchecked")
        DocumentReferenceResolver<String> resolver = mock(DocumentReferenceResolver.class);
        when(cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current")).thenReturn(resolver);
        DocumentReference expectedMapping = new DocumentReference("xwiki", "PhenoTips", "PhenotypeMapping");
        when(resolver.resolve("PhenoTips.PhenotypeMapping")).thenReturn(expectedMapping);

        RecordConfiguration config =
            new GlobalRecordConfiguration(this.xcp, mock(UIExtensionManager.class), mock(UIExtensionFilter.class));
        Assert.assertEquals(expectedMapping, config.getPhenotypeMapping());
    }

    /** {@link GlobalRecordConfiguration#getPhenotypeMapping()} returns null when getting the actual mapping fails. */
    @Test
    public void getPhenotypeMappingWithExceptions() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Matchers.any(EntityReference.class), Matchers.same(context))).thenReturn(wh);
        when(wh.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS)).thenReturn(null);
        RecordConfiguration config =
            new GlobalRecordConfiguration(this.xcp, mock(UIExtensionManager.class), mock(UIExtensionFilter.class));
        Assert.assertNull(config.getPhenotypeMapping());

        BaseObject o = mock(BaseObject.class);
        when(o.getStringValue("phenotypeMapping")).thenReturn("PhenoTips.XPhenotypeMapping");
        when(wh.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS)).thenReturn(o);
        ComponentManager cm = mock(ComponentManager.class);
        @SuppressWarnings("unchecked")
        Provider<ComponentManager> mockProvider = mock(Provider.class);
        // This is a bit fragile, let's hope the field name doesn't change
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", mockProvider);
        when(mockProvider.get()).thenReturn(cm);
        when(cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current")).thenThrow(
            new ComponentLookupException("No such component"));

        config = new GlobalRecordConfiguration(this.xcp, mock(UIExtensionManager.class), mock(UIExtensionFilter.class));
        Assert.assertNull(config.getPhenotypeMapping());
    }

    /** Basic tests for {@link GlobalRecordConfiguration#getDateOfBirthFormat()}. */
    @Test
    public void getDateOfBirthFormat() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Matchers.any(EntityReference.class), Matchers.same(context))).thenReturn(wh);
        BaseObject o = mock(BaseObject.class);
        when(wh.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getStringValue("dateOfBirthFormat")).thenReturn("MMMM yyyy");

        RecordConfiguration config =
            new GlobalRecordConfiguration(this.xcp, mock(UIExtensionManager.class), mock(UIExtensionFilter.class));
        Assert.assertEquals("MMMM yyyy", config.getDateOfBirthFormat());
    }

    /** {@link GlobalRecordConfiguration#getDateOfBirthFormat()} has a default format. */
    @Test
    public void getDateOfBirthFormatDefaultValue() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Matchers.any(EntityReference.class), Matchers.same(context))).thenReturn(wh);
        BaseObject o = mock(BaseObject.class);
        when(wh.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getStringValue("dateOfBirthFormat")).thenReturn("");

        RecordConfiguration config =
            new GlobalRecordConfiguration(this.xcp, mock(UIExtensionManager.class), mock(UIExtensionFilter.class));
        Assert.assertEquals("yyyy-MM-dd", config.getDateOfBirthFormat());
    }

    /** {@link GlobalRecordConfiguration#getDateOfBirthFormat()} catches exceptions. */
    @Test
    public void getDateOfBirthFormatWithException() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        when(x.getDocument(Matchers.any(EntityReference.class), Matchers.same(context)))
            .thenThrow(new XWikiException());

        RecordConfiguration config =
            new GlobalRecordConfiguration(this.xcp, mock(UIExtensionManager.class), mock(UIExtensionFilter.class));
        Assert.assertEquals("yyyy-MM-dd", config.getDateOfBirthFormat());
    }

    /** {@link GlobalRecordConfiguration#getDateOfBirthFormat()} has a default format when the config is missing. */
    @Test
    public void getDateOfBirthFormatWithMissingConfiguration() throws ComponentLookupException, XWikiException
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.xcp.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Matchers.any(EntityReference.class), Matchers.same(context))).thenReturn(wh);
        when(wh.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS)).thenReturn(null);

        RecordConfiguration config =
            new GlobalRecordConfiguration(this.xcp, mock(UIExtensionManager.class), mock(UIExtensionFilter.class));
        Assert.assertEquals("yyyy-MM-dd", config.getDateOfBirthFormat());
    }

    /** {@link GlobalRecordConfiguration#toString()} lists all the enabled sections. */
    @Test
    public void toStringTest() throws ComponentLookupException
    {
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        RecordConfiguration c = new GlobalRecordConfiguration(this.xcp, m, filter);

        Map<String, String> params;
        List<UIExtension> sections = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section1");
        params = new HashMap<String, String>();
        params.put("title", "Patient information");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("disabled_section");
        params = new HashMap<String, String>();
        params.put("title", "Family history and pedigree");
        params.put("enabled", "false");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section4");
        params = new HashMap<String, String>();
        params.put("title", "Prenatal history");
        params.put("enabled", "");
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section3");
        params = new HashMap<String, String>();
        params.put("title", "Clinical observations");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        when(m.get("org.phenotips.patientSheet.content")).thenReturn(sections);
        List<UIExtension> sorted = realFilter.filter(sections, "order");
        when(filter.filter(sections, "order")).thenReturn(sorted);

        List<UIExtension> fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Patient name");
        params.put("fields", ",first_name ,, last_name,");
        params.put("enabled", "");
        params.put("order", "2");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("fields", "external_id");
        params.put("title", "Identifier");
        params.put("enabled", "true");
        params.put("order", "1");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Date of birth");
        params.put("fields", "date_of_birth,");
        params.put("enabled", "false");
        params.put("order", "3");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Sex");
        params.put("fields", "gender");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Pedigree");
        params.put("fields", "");
        params.put("order", "4");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("section1")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Ethnicity");
        params.put("fields", "maternal_ethnicity,paternal_ethnicity");
        params.put("enabled", "true");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("disabled_section")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Gestation at delivery");
        params.put("fields", "gestation");
        params.put("enabled", "true");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("section4")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        fields = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        params = new HashMap<String, String>();
        params.put("title", "Clinical symptoms");
        params.put("fields", "unaffected,phenotype,negative_phenotype");
        params.put("enabled", "true");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("section3")).thenReturn(fields);
        sorted = realFilter.filter(fields, "order");
        when(filter.filter(fields, "order")).thenReturn(sorted);

        Assert.assertEquals("Patient information [Identifier, Patient name, Pedigree, Sex], "
            + "Clinical observations [Clinical symptoms], Prenatal history [Gestation at delivery]", c.toString());
    }
}
