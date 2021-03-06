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
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;
import org.xwiki.uiextension.internal.filter.SortByParameterFilter;

import java.util.Collections;
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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link RecordSection} implementation, {@link UIXRecordSection}.
 *
 * @version $Id$
 */
public class UIXRecordSectionTest
{
    private static final String ORDER = "order";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private UIExtension uiExtension;

    @Mock
    private UIExtensionManager uixManager;

    @Mock
    private UIExtensionFilter orderFilter;

    private Map<String, String> parameters = new HashMap<>();

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        when(this.uiExtension.getParameters()).thenReturn(this.parameters);
    }

    /** Basic tests for {@link RecordSection#getExtension()}. */
    @Test
    public void getExtension()
    {
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertSame(this.uiExtension, s.getExtension());
    }

    /** Basic test to affirm that passing in a null extension will result in an exception. */
    @Test
    public void throwExceptionIfExtensionNull()
    {
        this.thrown.expect(IllegalArgumentException.class);
        new UIXRecordSection(null, this.uixManager, this.orderFilter);
    }

    /** Basic test to affirm that passing in a null UIExtensionManager will result in an exception. */
    @Test
    public void throwExceptionIfUIXManagerNull()
    {
        this.thrown.expect(IllegalArgumentException.class);
        new UIXRecordSection(this.uiExtension, null, this.orderFilter);
    }

    /** Basic test to affirm that passing in a null UIExtensionFilter will result in an exception. */
    @Test
    public void throwExceptionIfOrderFilterNull()
    {
        this.thrown.expect(IllegalArgumentException.class);
        new UIXRecordSection(this.uiExtension, this.uixManager, null);
    }

    /** {@link RecordSection#getName()} returns the title set in the properties. */
    @Test
    public void getName()
    {
        this.parameters.put("title", "Patient Information");
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertEquals("Patient Information", s.getName());
    }

    /** {@link RecordSection#getName()} returns the last part of the ID. */
    @Test
    public void getNameWithMissingTitle()
    {
        when(this.uiExtension.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertEquals("Patient info", s.getName());
    }

    /** {@link RecordSection#isEnabled()} returns true when there's no setting in the properties. */
    @Test
    public void isEnabledReturnsTrueForNullSetting()
    {
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordSection#isEnabled()} returns true when there's no value set in the properties. */
    @Test
    public void isEnabledReturnsTrueForEmptySetting()
    {
        this.parameters.put("enabled", "");
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordSection#isEnabled()} returns true when set to "true" in the properties. */
    @Test
    public void isEnabledReturnsTrueForTrueSetting()
    {
        this.parameters.put("enabled", "true");
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordSection#isEnabled()} returns false only when explicitly disabled in the properties. */
    @Test
    public void isEnabledReturnsFalseForFalseSetting()
    {
        this.parameters.put("enabled", "false");
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertFalse(s.isEnabled());
    }

    @Test
    public void setEnabledOverridesDefaultSetting()
    {
        this.parameters.put("enabled", "false");
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertFalse(s.isEnabled());
        s.setEnabled(true);
        Assert.assertTrue(s.isEnabled());

        this.parameters.put("enabled", "true");
        s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertTrue(s.isEnabled());
        s.setEnabled(false);
        Assert.assertFalse(s.isEnabled());
    }

    /** {@link RecordSection#isEnabled()} returns true when there's no setting in the properties. */
    @Test
    public void isExpandedReturnsFalseForNullSetting()
    {
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertFalse(s.isExpandedByDefault());
    }

    @Test
    public void isExpandedReturnsFalseForEmptySetting()
    {
        this.parameters.put("expanded_by_default", "");
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertFalse(s.isExpandedByDefault());
    }

    @Test
    public void isExpandedReturnsTrueForTrueSetting()
    {
        this.parameters.put("expanded_by_default", "true");
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertTrue(s.isExpandedByDefault());
    }

    @Test
    public void isExpandedReturnsFalseForFalseSetting()
    {
        this.parameters.put("expanded_by_default", "false");
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertFalse(s.isExpandedByDefault());
    }

    @Test
    public void setExpandedOverridesDefaultSetting()
    {
        this.parameters.put("expanded_by_default", "false");
        RecordSection s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertFalse(s.isExpandedByDefault());
        s.setExpandedByDefault(true);
        Assert.assertTrue(s.isExpandedByDefault());

        this.parameters.put("expanded_by_default", "true");
        s = new UIXRecordSection(this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertTrue(s.isExpandedByDefault());
        s.setExpandedByDefault(false);
        Assert.assertFalse(s.isExpandedByDefault());
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
        RecordSection s = new UIXRecordSection(ex, m, filter);

        Map<String, String> params;
        List<UIExtension> fields = new LinkedList<>();

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("enabled", "");
        params.put(ORDER, "2");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("1");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("enabled", "true");
        params.put(ORDER, "1");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("0");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("enabled", "false");
        params.put(ORDER, "3");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("invalid");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("3");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put(ORDER, "4");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("2");
        fields.add(ex);

        when(m.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, ORDER);
        when(filter.filter(fields, ORDER)).thenReturn(sorted);

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
        RecordSection s = new UIXRecordSection(ex, m, filter);

        Map<String, String> params;
        List<UIExtension> fields = new LinkedList<>();

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("enabled", "");
        params.put(ORDER, "2");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("1");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("enabled", "true");
        params.put(ORDER, "1");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("0");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("enabled", "false");
        params.put(ORDER, "3");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("2");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("4");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put(ORDER, "4");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("3");
        fields.add(ex);

        when(m.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, ORDER);
        when(filter.filter(fields, ORDER)).thenReturn(sorted);

        List<RecordElement> result = s.getAllElements();
        Assert.assertEquals(5, result.size());
        for (int i = 0; i < 5; ++i) {
            Assert.assertEquals(String.valueOf(i), result.get(i).getExtension().getId());
        }
    }

    @Test
    public void setElementsOverridesDefaults() throws ComponentLookupException
    {
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(ex.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSection s = new UIXRecordSection(ex, m, filter);

        Map<String, String> params;
        List<UIExtension> fields = new LinkedList<>();

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("enabled", "");
        params.put(ORDER, "2");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("1");
        fields.add(ex);

        when(m.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, ORDER);
        when(filter.filter(fields, ORDER)).thenReturn(sorted);

        Assert.assertEquals(1, s.getAllElements().size());
        s.setElements(Collections.emptyList());
        Assert.assertEquals(0, s.getAllElements().size());
        RecordElement e = Mockito.mock(RecordElement.class);
        s.setElements(Collections.singletonList(e));
        Assert.assertSame(e, s.getAllElements().get(0));
    }

    /** {@link RecordSection#toString()} shows the section name and the list of enabled elements. */
    @Test
    public void toStringTest() throws ComponentLookupException
    {
        UIExtensionManager m = mock(UIExtensionManager.class);
        UIExtension ex = mock(UIExtension.class);
        UIExtensionFilter filter = mock(UIExtensionFilter.class, "sortByParameter");
        UIExtensionFilter realFilter = new SortByParameterFilter();
        Map<String, String> params = new HashMap<>();
        params.put("title", "Patient information");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSection s = new UIXRecordSection(ex, m, filter);

        List<UIExtension> fields = new LinkedList<>();

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("title", "Name");
        params.put("enabled", "");
        params.put(ORDER, "2");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("title", "Identifier");
        params.put("enabled", "true");
        params.put(ORDER, "1");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("title", "Age");
        params.put("enabled", "false");
        params.put(ORDER, "3");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        when(ex.getParameters()).thenReturn(params);
        params.put("title", "Indication for referral");
        fields.add(ex);

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("title", "Onset");
        params.put(ORDER, "4");
        when(ex.getParameters()).thenReturn(params);
        fields.add(ex);

        when(m.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, ORDER);
        when(filter.filter(fields, ORDER)).thenReturn(sorted);

        Assert.assertEquals("Patient information [Identifier, Name, Onset, Indication for referral]", s.toString());
    }
}
