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
package org.phenotips.studies.family.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.spi.RecordConfigurationModule;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.CapturingMatcher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link GlobalFamilyConfigurationModule}.
 *
 * @version $Id$
 */
public class GlobalFamilyConfigurationModuleTest
{
    private static final String TITLE_LABEL = "title";

    private static final String SORT_PARAMETER_NAME = "order";

    private static final String SECTION_A_LABEL = "sectionA";

    private static final String SECTION_B_LABEL = "sectionB";

    private static final String SECTION_C_LABEL = "sectionC";

    @Rule
    public final MockitoComponentMockingRule<RecordConfigurationModule> mocker =
        new MockitoComponentMockingRule<>(GlobalFamilyConfigurationModule.class);

    @Mock
    private RecordConfiguration config;

    private CapturingMatcher<List<RecordSection>> sectionsCapture = new CapturingMatcher<>();

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        Mockito.doNothing().when(this.config).setSections(org.mockito.Matchers.argThat(this.sectionsCapture));
    }

    @Test
    public void processAddsAllSectionsToConfig() throws ComponentLookupException
    {
        final UIExtensionManager uixManager = this.mocker.getInstance(UIExtensionManager.class);
        final UIExtensionFilter orderFilter = this.mocker.getInstance(UIExtensionFilter.class, "sortByParameter");

        final UIExtension extensionA = mock(UIExtension.class);
        final UIExtension extensionB = mock(UIExtension.class);
        final UIExtension extensionC = mock(UIExtension.class);

        final List<UIExtension> extensions = Arrays.asList(extensionA, extensionB, extensionC);

        when(uixManager.get("phenotips.familyRecord.content")).thenReturn(extensions);
        when(orderFilter.filter(extensions, SORT_PARAMETER_NAME)).thenReturn(extensions);

        final Map<String, String> paramA = new HashMap<>();
        paramA.put(TITLE_LABEL, SECTION_A_LABEL);
        when(extensionA.getParameters()).thenReturn(paramA);

        final Map<String, String> paramB = new HashMap<>();
        paramB.put(TITLE_LABEL, SECTION_B_LABEL);
        when(extensionB.getParameters()).thenReturn(paramB);

        final Map<String, String> paramC = new HashMap<>();
        paramC.put(TITLE_LABEL, SECTION_C_LABEL);
        when(extensionC.getParameters()).thenReturn(paramC);

        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        final List<String> sectionNames = Arrays.asList(SECTION_A_LABEL, SECTION_B_LABEL, SECTION_C_LABEL);
        final List<RecordSection> sections = this.sectionsCapture.getLastValue();
        Assert.assertEquals(3, sections.size());
        Assert.assertTrue(sectionNames.contains(sections.get(0).getName()));
        Assert.assertTrue(sectionNames.contains(sections.get(1).getName()));
        Assert.assertTrue(sectionNames.contains(sections.get(2).getName()));
    }

    @Test
    public void processReturnsEmptyConfigWhenNoExtensionsAreFound() throws ComponentLookupException
    {
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        Assert.assertTrue(this.sectionsCapture.getLastValue().isEmpty());
    }

    @Test
    public void processReturnsNullForNullInputConfig() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().process(null));
    }

    @Test
    public void priorityIs0() throws ComponentLookupException
    {
        final int priority = this.mocker.getComponentUnderTest().getPriority();
        Assert.assertEquals(0, priority);
    }

    @Test
    public void supportsFamilyRecordType() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().supportsRecordType("family"));

    }

    @Test
    public void doesNotSupportOtherRecordTypes() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType("patient"));
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType("asdfasdf"));
    }

    @Test
    public void supportsRecordTypeReturnsFalseIfNullOrEmpty() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType(null));
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType(""));
    }
}
