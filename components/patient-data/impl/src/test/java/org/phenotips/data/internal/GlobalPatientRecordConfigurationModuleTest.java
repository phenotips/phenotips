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
package org.phenotips.data.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.spi.RecordConfigurationModule;
import org.phenotips.data.Patient;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.CapturingMatcher;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link GlobalPatientRecordConfigurationModule}.
 *
 * @version $Id$
 */
public class GlobalPatientRecordConfigurationModuleTest
{
    private static final EntityReference PREFERENCES_LOCATION = new EntityReference("WebHome", EntityType.DOCUMENT,
        Patient.DEFAULT_DATA_SPACE);

    private static final String PATIENT_LABEL = "patient";

    private static final String FAMILY_LABEL = "family";

    private static final String TITLE_LABEL = "title";

    private static final String SORT_PARAMETER_NAME = "order";

    private static final String ENABLED_LABEL = "enabled";

    private static final String TRUE_LABEL = "true";

    private static final String EXPANDED_BY_DEFAULT_LABEL = "expanded_by_default";

    private static final String SECTION_A_LABEL = "sectionA";

    private static final String SECTION_B_LABEL = "sectionB";

    private static final String SECTION_C_LABEL = "sectionC";

    @Rule
    public final MockitoComponentMockingRule<RecordConfigurationModule> mocker =
        new MockitoComponentMockingRule<>(GlobalPatientRecordConfigurationModule.class);

    @Mock
    private RecordConfiguration config;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument configDocument;

    @Mock
    private BaseObject configObject;

    private DocumentReference genericMapping = new DocumentReference("xwiki", "PhenoTips", "Generic phenotype mapping");

    private CapturingMatcher<List<RecordSection>> sectionsCapture = new CapturingMatcher<>();

    @Before
    public void setUp() throws ComponentLookupException, XWikiException
    {
        MockitoAnnotations.initMocks(this);
        Mockito.doNothing().when(this.config).setSections(Matchers.argThat(this.sectionsCapture));

        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDocument(PREFERENCES_LOCATION, this.context)).thenReturn(this.configDocument);
        when(this.configDocument.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS))
            .thenReturn(this.configObject);

        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(resolver.resolve("PhenoTips.Generic phenotype mapping")).thenReturn(this.genericMapping);
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
        final Map<String, String> param = new HashMap<>();
        param.put(ENABLED_LABEL, TRUE_LABEL);
        param.put(EXPANDED_BY_DEFAULT_LABEL, TRUE_LABEL);

        when(uixManager.get("org.phenotips.patientSheet.content")).thenReturn(extensions);
        when(orderFilter.filter(extensions, SORT_PARAMETER_NAME)).thenReturn(extensions);

        final Map<String, String> paramA = new HashMap<>(param);
        paramA.put(TITLE_LABEL, SECTION_A_LABEL);
        when(extensionA.getParameters()).thenReturn(paramA);

        final Map<String, String> paramB = new HashMap<>(param);
        paramB.put(TITLE_LABEL, SECTION_B_LABEL);
        when(extensionB.getParameters()).thenReturn(paramB);

        final Map<String, String> paramC = new HashMap<>(param);
        paramC.put(TITLE_LABEL, SECTION_C_LABEL);
        when(extensionC.getParameters()).thenReturn(paramC);

        this.mocker.getComponentUnderTest().process(this.config);
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
    public void processSetsCustomMapping() throws ComponentLookupException
    {
        when(this.configObject.getStringValue("phenotypeMapping")).thenReturn("PhenoTips.Generic phenotype mapping");
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config).setPhenotypeMapping(this.genericMapping);
    }

    @Test
    public void processDoesntSetCustomMappingWhenNotSet() throws ComponentLookupException
    {
        when(this.configObject.getStringValue("phenotypeMapping")).thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config, never()).setPhenotypeMapping(any(DocumentReference.class));
    }

    @Test
    public void processDoesntSetCustomMappingWhenSetToEmptyString() throws ComponentLookupException
    {
        when(this.configObject.getStringValue("phenotypeMapping")).thenReturn("");
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config, never()).setPhenotypeMapping(any(DocumentReference.class));
    }

    @Test
    public void processDoesntSetCustomMappingWhenConfigurationDoesNotExist() throws ComponentLookupException
    {
        when(this.configDocument.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS))
            .thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config, never()).setPhenotypeMapping(any(DocumentReference.class));
    }

    @Test
    public void processDoesntSetCustomMappingWhenReadingConfigurationFails()
        throws ComponentLookupException, XWikiException
    {
        when(this.xwiki.getDocument(PREFERENCES_LOCATION, this.context)).thenThrow(new XWikiException());
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config, never()).setPhenotypeMapping(any(DocumentReference.class));
    }

    @Test
    public void processSetsCustomDobFormat() throws ComponentLookupException
    {
        when(this.configObject.getStringValue("dateOfBirthFormat")).thenReturn("MMMM yyyy");
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config).setDateOfBirthFormat("MMMM yyyy");
    }

    @Test
    public void processDoesntSetCustomDobFormatWhenNotSet() throws ComponentLookupException
    {
        when(this.configObject.getStringValue("dateOfBirthFormat")).thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config, never()).setDateOfBirthFormat(any(String.class));
    }

    @Test
    public void processDoesntSetCustomDobFormatWhenSetToEmptyString() throws ComponentLookupException
    {
        when(this.configObject.getStringValue("dateOfBirthFormat")).thenReturn("");
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config, never()).setDateOfBirthFormat(any(String.class));
    }

    @Test
    public void processDoesntSetCustomDobFormatWhenConfigurationDoesNotExist() throws ComponentLookupException
    {
        when(this.configDocument.getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS))
            .thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config, never()).setDateOfBirthFormat(any(String.class));
    }

    @Test
    public void processDoesntSetCustomDobFormatWhenReadingConfigurationFails()
        throws ComponentLookupException, XWikiException
    {
        when(this.xwiki.getDocument(PREFERENCES_LOCATION, this.context)).thenThrow(new XWikiException());
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config, never()).setDateOfBirthFormat(any(String.class));
    }

    @Test
    public void processReturnsNullForNullInputConfig() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().process(null));
    }

    @Test
    public void priorityIs0() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPriority());
    }

    @Test
    public void supportsPatientRecordType() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().supportsRecordType(PATIENT_LABEL));

    }

    @Test
    public void doesNotSupportOtherRecordTypes() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType(FAMILY_LABEL));
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType("asdfasdf"));
    }

    @Test
    public void supportsRecordTypeReturnsFalseIfNullOrEmpty() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType(null));
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType(""));
    }
}
