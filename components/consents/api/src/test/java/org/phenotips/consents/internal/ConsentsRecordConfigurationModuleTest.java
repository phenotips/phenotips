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
package org.phenotips.consents.internal;

import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.internal.DefaultRecordConfiguration;
import org.phenotips.configuration.spi.RecordConfigurationModule;
import org.phenotips.configuration.spi.UIXRecordSection;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Tests for the {@link ConsentsRecordConfigurationModule}.
 *
 * @version $Id$
 */
public class ConsentsRecordConfigurationModuleTest
{
    private static final String PATIENT_LABEL = "patient";

    private static final String FAMILY_LABEL = "family";

    private static final String SECTION_A_LABEL = "phenotips.sectionA";

    private static final String SECTION_B_LABEL = "phenotips.sectionB";

    private static final String SECTION_C_LABEL = "phenotips.sectionC";

    private static final String FIELD_A_LABEL = "phenotips.fieldA";

    private static final String FIELD_B1_LABEL = "phenotips.fieldB1";

    private static final String FIELD_B2_LABEL = "phenotips.fieldB2";

    private static final String FIELD_C_LABEL = "phenotips.fieldC";

    @Rule
    public final MockitoComponentMockingRule<RecordConfigurationModule> mocker =
        new MockitoComponentMockingRule<>(ConsentsRecordConfigurationModule.class);

    private ConsentAuthorizer consentAuthorizer;

    private DefaultRecordConfiguration config;

    private PatientRepository patients;

    private Patient patient;

    @Before
    public void setUp() throws ComponentLookupException
    {
        final DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);

        this.patients = this.mocker.getInstance(PatientRepository.class);
        this.consentAuthorizer = this.mocker.getInstance(ConsentAuthorizer.class);
        this.config = mock(DefaultRecordConfiguration.class);
        this.patient = mock(Patient.class);

        when(dab.getCurrentDocumentReference()).thenReturn(mock(DocumentReference.class));
        when(this.patients.get(any(DocumentReference.class))).thenReturn(this.patient);
    }

    @Test
    public void processWithNullConfigReturnsNull() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().process(null));
    }

    @Test
    public void processWithNonPatientReturnsUnchagedConfig() throws ComponentLookupException
    {
        when(this.patients.get(any(DocumentReference.class))).thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);
    }

    @Test
    public void processWithEmptyConfigReturnsUnchagedConfig() throws ComponentLookupException
    {
        when(this.config.getEnabledSections()).thenReturn(Collections.emptyList());

        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config, times(1)).getEnabledSections();
        verifyNoMoreInteractions(this.config);
    }

    @Test
    public void afterProcessOnlyConsentedSectionsRemain() throws ComponentLookupException
    {
        final UIExtension sectionExtensionA = mock(UIExtension.class);
        final UIExtension sectionExtensionB = mock(UIExtension.class);
        final UIExtension sectionExtensionC = mock(UIExtension.class);
        final UIExtensionManager uixManager = mock(UIExtensionManager.class);
        final UIExtensionFilter orderFilter = mock(UIExtensionFilter.class, new ReturnsArgumentAt(0));

        when(sectionExtensionA.getId()).thenReturn(SECTION_A_LABEL);
        when(sectionExtensionB.getId()).thenReturn(SECTION_B_LABEL);
        when(sectionExtensionC.getId()).thenReturn(SECTION_C_LABEL);

        final UIExtension fieldExtensionA = mock(UIExtension.class);
        final List<UIExtension> fieldsA = Collections.singletonList(fieldExtensionA);
        when(fieldExtensionA.getId()).thenReturn(FIELD_A_LABEL);
        when(uixManager.get(SECTION_A_LABEL)).thenReturn(fieldsA);

        final UIExtension fieldExtensionB1 = mock(UIExtension.class);
        final UIExtension fieldExtensionB2 = mock(UIExtension.class);
        final List<UIExtension> fieldsB = Arrays.asList(fieldExtensionB1, fieldExtensionB2);
        when(fieldExtensionB1.getId()).thenReturn(FIELD_B1_LABEL);
        when(fieldExtensionB2.getId()).thenReturn(FIELD_B2_LABEL);
        when(uixManager.get(SECTION_B_LABEL)).thenReturn(fieldsB);

        final UIExtension fieldExtensionC = mock(UIExtension.class);
        final List<UIExtension> fieldsC = Collections.singletonList(fieldExtensionC);
        when(fieldExtensionC.getId()).thenReturn(FIELD_C_LABEL);
        when(uixManager.get(SECTION_C_LABEL)).thenReturn(fieldsC);

        final RecordSection sectionA = new UIXRecordSection(sectionExtensionA, uixManager, orderFilter);
        final RecordSection sectionB = new UIXRecordSection(sectionExtensionB, uixManager, orderFilter);
        final RecordSection sectionC = new UIXRecordSection(sectionExtensionC, uixManager, orderFilter);

        when(this.config.getAllSections()).thenReturn(Arrays.asList(sectionA, sectionB, sectionC));
        when(this.config.getEnabledSections()).thenCallRealMethod();
        Assert.assertEquals(3, this.config.getEnabledSections().size());
        Assert.assertEquals(1, this.config.getEnabledSections().get(0).getEnabledElements().size());
        Assert.assertEquals(2, this.config.getEnabledSections().get(1).getEnabledElements().size());
        Assert.assertEquals(1, this.config.getEnabledSections().get(2).getEnabledElements().size());
        Assert.assertTrue(sectionA.isEnabled());
        Assert.assertTrue(sectionA.getAllElements().get(0).isEnabled());
        Assert.assertTrue(sectionB.isEnabled());
        Assert.assertTrue(sectionB.getAllElements().get(0).isEnabled());
        Assert.assertTrue(sectionB.getAllElements().get(1).isEnabled());
        Assert.assertTrue(sectionC.isEnabled());
        Assert.assertTrue(sectionC.getAllElements().get(0).isEnabled());

        when(this.consentAuthorizer.filterForm(sectionA.getEnabledElements(), this.patient))
            .thenReturn(Collections.emptyList());
        when(this.consentAuthorizer.filterForm(sectionB.getEnabledElements(), this.patient))
            .thenReturn(sectionB.getEnabledElements().subList(1, 2));
        when(this.consentAuthorizer.filterForm(sectionC.getEnabledElements(), this.patient))
            .thenReturn(Collections.emptyList());

        // Call the method being tested.
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));

        Assert.assertEquals(1, this.config.getEnabledSections().size());
        Assert.assertEquals(1, this.config.getEnabledSections().get(0).getEnabledElements().size());
        Assert.assertEquals(0, sectionA.getEnabledElements().size());
        Assert.assertEquals(1, sectionA.getAllElements().size());
        Assert.assertFalse(sectionA.getAllElements().get(0).isEnabled());
        Assert.assertEquals(1, sectionB.getEnabledElements().size());
        Assert.assertEquals(2, sectionB.getAllElements().size());
        Assert.assertFalse(sectionB.getAllElements().get(0).isEnabled());
        Assert.assertTrue(sectionB.getAllElements().get(1).isEnabled());
        Assert.assertEquals(0, sectionC.getEnabledElements().size());
        Assert.assertEquals(1, sectionC.getAllElements().size());
        Assert.assertFalse(sectionC.getAllElements().get(0).isEnabled());

        Assert.assertFalse(sectionA.isEnabled());
        Assert.assertTrue(sectionB.isEnabled());
        Assert.assertFalse(sectionC.isEnabled());
    }

    @Test
    public void priorityIs90() throws ComponentLookupException
    {
        final int priority = this.mocker.getComponentUnderTest().getPriority();
        Assert.assertEquals(90, priority);
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
