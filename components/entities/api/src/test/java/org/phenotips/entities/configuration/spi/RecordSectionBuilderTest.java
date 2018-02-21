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
package org.phenotips.entities.configuration.spi;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.configuration.RecordElement;
import org.phenotips.entities.configuration.RecordSection;
import org.phenotips.entities.configuration.spi.uix.UIXRecordElementBuilder;
import org.phenotips.entities.configuration.spi.uix.UIXRecordSectionBuilder;

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

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Provider;

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
 * Tests for {@link UIXRecordSectionBuilder}.
 *
 * @version $Id$
 */
@NotThreadSafe
public class RecordSectionBuilderTest
{
    private static final String ORDER = "order";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private UIExtension uiExtension;

    @Mock
    private UIExtensionManager uixManager;

    @Mock
    private PrimaryEntityConfigurationBuilder config;

    @Mock
    private PrimaryEntityManager<? extends PrimaryEntity> entityManager;

    @Mock
    private UIExtensionFilter orderFilter;

    private Map<String, String> parameters = new HashMap<>();

    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private DocumentReferenceResolver<String> stringResolver;

    @Mock
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    @Mock
    private EntityReference stubReference;

    @Mock
    private DocumentReference fullReference;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        when(this.uiExtension.getParameters()).thenReturn(this.parameters);

        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current"))
            .thenReturn(this.referenceResolver);
        when(this.cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current")).thenReturn(this.stringResolver);

        Mockito.doReturn(this.entityManager).when(this.config).getEntityManager();
        when(this.entityManager.getTypeReference()).thenReturn(this.stubReference);
        when(this.referenceResolver.resolve(this.stubReference)).thenReturn(this.fullReference);
    }

    /** Basic tests for {@link RecordSection#getExtension()}. */
    @Test
    public void getExtension()
    {
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertSame(this.uiExtension, s.getExtension());
    }

    /** Basic test to affirm that passing in a null configuration builder will result in an exception. */
    @Test
    public void throwExceptionIfConfigurationBuilderNull()
    {
        this.thrown.expect(IllegalArgumentException.class);
        new UIXRecordSectionBuilder(null, this.uiExtension, this.uixManager, this.orderFilter);
    }

    /** Basic test to affirm that passing in a null extension will result in an exception. */
    @Test
    public void throwExceptionIfExtensionNull()
    {
        this.thrown.expect(IllegalArgumentException.class);
        new UIXRecordSectionBuilder(this.config, null, this.uixManager, this.orderFilter);
    }

    /** Basic test to affirm that passing in a null UIExtensionManager will result in an exception. */
    @Test
    public void throwExceptionIfUIXManagerNull()
    {
        this.thrown.expect(IllegalArgumentException.class);
        new UIXRecordSectionBuilder(this.config, this.uiExtension, null, this.orderFilter);
    }

    /** Basic test to affirm that passing in a null UIExtensionFilter will result in an exception. */
    @Test
    public void throwExceptionIfOrderFilterNull()
    {
        this.thrown.expect(IllegalArgumentException.class);
        new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, null);
    }

    /** {@link RecordSection#getName()} returns the title set in the properties. */
    @Test
    public void getName()
    {
        this.parameters.put("title", "Patient Information");
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertEquals("Patient Information", s.getName());
    }

    /** {@link RecordSection#getName()} returns the last part of the ID. */
    @Test
    public void getNameWithMissingTitle()
    {
        when(this.uiExtension.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertEquals("Patient info", s.getName());
    }

    /** {@link RecordSection#isEnabled()} returns true when there's no setting in the properties. */
    @Test
    public void isEnabledReturnsTrueForNullSetting()
    {
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordSection#isEnabled()} returns true when there's no value set in the properties. */
    @Test
    public void isEnabledReturnsTrueForEmptySetting()
    {
        this.parameters.put("enabled", "");
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordSection#isEnabled()} returns true when set to "true" in the properties. */
    @Test
    public void isEnabledReturnsTrueForTrueSetting()
    {
        this.parameters.put("enabled", "true");
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertTrue(s.isEnabled());
    }

    /** {@link RecordSection#isEnabled()} returns false only when explicitly disabled in the properties. */
    @Test
    public void isEnabledReturnsFalseForFalseSetting()
    {
        this.parameters.put("enabled", "false");
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertFalse(s.isEnabled());
    }

    @Test
    public void setEnabledOverridesDefaultSetting()
    {
        this.parameters.put("enabled", "false");
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertFalse(s.isEnabled());
        s.setEnabled(true);
        Assert.assertTrue(s.isEnabled());

        this.parameters.put("enabled", "true");
        s = new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);
        Assert.assertTrue(s.isEnabled());
        s.setEnabled(false);
        Assert.assertFalse(s.isEnabled());
    }

    /** {@link RecordSection#getEnabledElements()} returns only enabled fields. */
    @Test
    public void getAllElementsReturnsOrderedElements() throws ComponentLookupException
    {
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(this.uiExtension.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);

        Map<String, String> params;
        UIExtension ex;
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

        when(this.uixManager.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, ORDER);
        when(this.orderFilter.filter(fields, ORDER)).thenReturn(sorted);

        List<RecordElementBuilder> result = s.getAllElements();
        Assert.assertEquals(5, result.size());
        for (int i = 0; i < 5; ++i) {
            Assert.assertEquals(String.valueOf(i), result.get(i).getExtension().getId());
        }
    }

    @Test
    public void setElementsOverridesDefaults() throws ComponentLookupException
    {
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(this.uiExtension.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);

        Map<String, String> params;
        UIExtension ex;
        List<UIExtension> fields = new LinkedList<>();

        ex = mock(UIExtension.class);
        params = new HashMap<>();
        params.put("enabled", "");
        params.put(ORDER, "2");
        when(ex.getParameters()).thenReturn(params);
        when(ex.getId()).thenReturn("1");
        fields.add(ex);

        when(this.uixManager.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, ORDER);
        when(this.orderFilter.filter(fields, ORDER)).thenReturn(sorted);

        Assert.assertEquals(1, s.getAllElements().size());
        s.setElements(Collections.emptyList());
        Assert.assertEquals(0, s.getAllElements().size());
        UIXRecordElementBuilder e = UIXRecordElementBuilder.with(this.config, ex);
        s.setElements(Collections.singletonList(e));
        Assert.assertSame(e, s.getAllElements().get(0));
    }

    /** {@link RecordSection#getEnabledElements()} returns only enabled fields. */
    @Test
    public void build() throws ComponentLookupException
    {
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(this.uiExtension.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);

        Map<String, String> params;
        UIExtension ex;
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

        when(this.uixManager.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, ORDER);
        when(this.orderFilter.filter(fields, ORDER)).thenReturn(sorted);

        RecordSection result = s.build();
        Assert.assertEquals("Patient info", result.getName());
        Assert.assertTrue(result.isEnabled());

        List<RecordElement> allElements = result.getAllElements();
        Assert.assertEquals(5, allElements.size());
        for (int i = 0; i < 5; ++i) {
            Assert.assertEquals(String.valueOf(i), allElements.get(i).getExtension().getId());
        }
        List<RecordElement> enabledElements = result.getEnabledElements();
        Assert.assertEquals(4, enabledElements.size());
        for (int i = 0; i < 4; ++i) {
            Assert.assertEquals(String.valueOf(i < 2 ? i : i + 1), enabledElements.get(i).getExtension().getId());
        }
    }

    /** {@link RecordSection#toString()} shows the section name and the list of enabled elements. */
    @Test
    public void toStringTest() throws ComponentLookupException
    {
        UIExtensionFilter realFilter = new SortByParameterFilter();
        when(this.uiExtension.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        this.parameters.put("title", "Patient information");
        when(this.uiExtension.getId()).thenReturn("org.phenotips.patientSheet.section.patient-info");
        RecordSectionBuilder s =
            new UIXRecordSectionBuilder(this.config, this.uiExtension, this.uixManager, this.orderFilter);

        Map<String, String> params;
        UIExtension ex;
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

        when(this.uixManager.get("org.phenotips.patientSheet.section.patient-info")).thenReturn(fields);
        List<UIExtension> sorted = realFilter.filter(fields, ORDER);
        when(this.orderFilter.filter(fields, ORDER)).thenReturn(sorted);

        Assert.assertEquals("Patient information [Identifier, Name, Age, Onset, Indication for referral]",
            s.toString());
    }
}
