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
package org.phenotips.configuration.internal;

import org.phenotips.configuration.PatientRecordConfiguration;
import org.phenotips.data.Patient;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;
import org.xwiki.uiextension.internal.filter.SortByParameterFilter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link PatientRecordConfiguration} implementation, {@link DefaultPatientRecordConfiguration}.
 * 
 * @version $Id$
 */
public class DefaultPatientRecordConfigurationTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientRecordConfiguration> mocker =
        new MockitoComponentMockingRule<PatientRecordConfiguration>(DefaultPatientRecordConfiguration.class);

    /** Basic tests for {@link PatientRecordConfiguration#getEnabledFieldNames()}. */
    @Test
    public void listAccessLevels() throws ComponentLookupException
    {
        UIExtensionManager m = this.mocker.getInstance(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = this.mocker.getInstance(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        Map<String, String> params;

        List<UIExtension> sections = new LinkedList<UIExtension>();

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section1");
        params = new HashMap<String, String>();
        params.put("enabled", "true");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("disabled_section");
        params = new HashMap<String, String>();
        params.put("enabled", "false");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section3");
        params = new HashMap<String, String>();
        params.put("enabled", "");
        when(ex.getParameters()).thenReturn(params);
        sections.add(ex);

        ex = mock(UIExtension.class);
        when(ex.getId()).thenReturn("section4");
        params = new HashMap<String, String>();
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
        Assert.assertEquals(expectedFields, this.mocker.getComponentUnderTest().getEnabledFieldNames());
    }

    /** Basic tests for {@link PatientRecordConfiguration#getAllFieldNames()}. */
    @Test
    public void getAllFieldNames() throws ComponentLookupException, XWikiException
    {
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(x.getDocument(Patient.CLASS_REFERENCE, context)).thenReturn(doc);
        BaseClass c = mock(BaseClass.class);
        when(doc.getXClass()).thenReturn(c);
        String[] props = new String[] {"external_id", "first_name", "last_name", "gender"};
        when(c.getPropertyNames()).thenReturn(props);

        List<String> expectedFields = new LinkedList<String>();
        expectedFields.add("external_id");
        expectedFields.add("first_name");
        expectedFields.add("last_name");
        expectedFields.add("gender");
        Assert.assertEquals(expectedFields, this.mocker.getComponentUnderTest().getAllFieldNames());
    }

    /** {@link PatientRecordConfiguration#getAllFieldNames()} catches exceptions. */
    @Test
    public void getAllFieldNamesWithException() throws ComponentLookupException, XWikiException
    {
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        when(x.getDocument(Patient.CLASS_REFERENCE, context)).thenThrow(new XWikiException());

        Assert.assertTrue(this.mocker.getComponentUnderTest().getAllFieldNames().isEmpty());
    }

    /** Basic tests for {@link PatientRecordConfiguration#getDateOfBirthFormat()}. */
    @Test
    public void getDateOfBirthFormat() throws ComponentLookupException, XWikiException
    {
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Mockito.any(EntityReference.class), Mockito.same(context))).thenReturn(wh);
        BaseObject o = mock(BaseObject.class);
        when(wh.getXObject(PatientRecordConfiguration.PREFERENCES_CLASS)).thenReturn(o);
        when(o.getStringValue("dateOfBirthFormat")).thenReturn("MMMM yyyy");

        Assert.assertEquals("MMMM yyyy", this.mocker.getComponentUnderTest().getDateOfBirthFormat());
    }

    /** {@link PatientRecordConfiguration#getDateOfBirthFormat()} has a default format. */
    @Test
    public void getDateOfBirthFormatDefaultValue() throws ComponentLookupException, XWikiException
    {
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Mockito.any(EntityReference.class), Mockito.same(context))).thenReturn(wh);
        BaseObject o = mock(BaseObject.class);
        when(wh.getXObject(PatientRecordConfiguration.PREFERENCES_CLASS)).thenReturn(o);
        when(o.getStringValue("dateOfBirthFormat")).thenReturn("");

        Assert.assertEquals("dd/MM/yyyy", this.mocker.getComponentUnderTest().getDateOfBirthFormat());
    }

    /** {@link PatientRecordConfiguration#getDateOfBirthFormat()} catches exceptions. */
    @Test
    public void getDateOfBirthFormatWithException() throws ComponentLookupException, XWikiException
    {
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        when(x.getDocument(Mockito.any(EntityReference.class), Mockito.same(context))).thenThrow(new XWikiException());

        Assert.assertEquals("dd/MM/yyyy", this.mocker.getComponentUnderTest().getDateOfBirthFormat());
    }

    /** {@link PatientRecordConfiguration#getDateOfBirthFormat()} has a default format when the config is missing. */
    @Test
    public void getDateOfBirthFormatWithMissingConfiguration() throws ComponentLookupException, XWikiException
    {
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument wh = mock(XWikiDocument.class);
        when(x.getDocument(Mockito.any(EntityReference.class), Mockito.same(context))).thenReturn(wh);
        when(wh.getXObject(PatientRecordConfiguration.PREFERENCES_CLASS)).thenReturn(null);

        Assert.assertEquals("dd/MM/yyyy", this.mocker.getComponentUnderTest().getDateOfBirthFormat());
    }
}
