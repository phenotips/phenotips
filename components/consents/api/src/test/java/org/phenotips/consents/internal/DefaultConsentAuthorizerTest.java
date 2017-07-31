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

import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.spi.UIXRecordElement;
import org.phenotips.consents.Consent;
import org.phenotips.consents.ConsentManager;
import org.phenotips.data.Patient;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.uiextension.UIExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultConsentAuthorizer}.
 *
 * @version $Id $
 */
public class DefaultConsentAuthorizerTest
{
    private static final String FIELD_A_LABEL = "fieldA";

    private static final String FIELD_B_LABEL = "fieldB";

    private static final String FIELD_C_LABEL = "fieldC";

    private static final String FIELD_D_LABEL = "fieldD";

    private static final String CONSENT_A_LABEL = "consentA";

    private static final String CONSENT_B_LABEL = "consentB";

    @Rule
    public final MockitoComponentMockingRule<ConsentAuthorizer> mocker =
        new MockitoComponentMockingRule<>(DefaultConsentAuthorizer.class);

    private ConsentAuthorizer component;

    private ConsentManager consentManager;

    private Patient patient;

    private Consent consentA;

    private Consent consentB;

    @Before
    public void setUp() throws ComponentLookupException
    {
        this.component = this.mocker.getComponentUnderTest();
        this.consentManager = this.mocker.getInstance(ConsentManager.class);
        this.patient = mock(Patient.class);
        this.consentA = mock(Consent.class);
        this.consentB = mock(Consent.class);

        when(this.consentA.getId()).thenReturn(CONSENT_A_LABEL);
        when(this.consentB.getId()).thenReturn(CONSENT_B_LABEL);
        when(this.consentA.isRequired()).thenReturn(false);
        when(this.consentB.isRequired()).thenReturn(false);
    }

    /**
     * Tests that {@link DefaultConsentAuthorizer#consentsGloballyEnabled()} returns false if the set of system consents
     * is empty.
     */
    @Test
    public void consentsGloballyEnabledSystemConsentsEmpty()
    {
        when(this.consentManager.getSystemConsents()).thenReturn(Collections.<Consent>emptySet());
        Assert.assertFalse(this.component.consentsGloballyEnabled());
    }

    /**
     * Tests that {@link DefaultConsentAuthorizer#consentsGloballyEnabled()} returns true if the set of system consents
     * is not empty.
     */
    @Test
    public void consentsGloballyEnabledSystemConsentsNotEmpty()
    {
        final Set<Consent> consents = new HashSet<>();
        consents.add(this.consentA);

        when(this.consentManager.getSystemConsents()).thenReturn(consents);
        Assert.assertTrue(this.component.consentsGloballyEnabled());
    }

    @Test
    public void filterFormHasRequiredConsents()
    {
        final RecordElement recordElementA = mock(UIXRecordElement.class);
        final RecordElement recordElementB = mock(UIXRecordElement.class);
        final RecordElement recordElementC = mock(UIXRecordElement.class);

        final Set<Consent> consents = new HashSet<>();
        consents.add(this.consentA);
        consents.add(this.consentB);
        when(this.consentB.isRequired()).thenReturn(true);
        when(this.consentManager.getMissingConsentsForPatient(this.patient)).thenReturn(consents);
        when(this.consentManager.getSystemConsents()).thenReturn(consents);

        final List<RecordElement> elements = Arrays.asList(recordElementA, recordElementB, recordElementC);
        final List<RecordElement> enabledElements = this.component.filterForm(elements, this.patient);
        Assert.assertTrue(enabledElements.isEmpty());
    }

    @Test
    public void filterFormHasNoRequiredConsents()
    {
        final UIExtension extensionA = mock(UIExtension.class);
        when(extensionA.getId()).thenReturn(FIELD_A_LABEL);
        final UIExtension extensionB = mock(UIExtension.class);
        when(extensionB.getId()).thenReturn(FIELD_B_LABEL);
        final UIExtension extensionC = mock(UIExtension.class);
        when(extensionC.getId()).thenReturn(FIELD_C_LABEL);

        final RecordElement recordElementA = mock(UIXRecordElement.class);
        when(recordElementA.getExtension()).thenReturn(extensionA);
        final RecordElement recordElementB = mock(UIXRecordElement.class);
        when(recordElementB.getExtension()).thenReturn(extensionB);
        final RecordElement recordElementC = mock(UIXRecordElement.class);
        when(recordElementC.getExtension()).thenReturn(extensionC);

        final Set<Consent> consents = new HashSet<>();
        consents.add(this.consentA);
        consents.add(this.consentB);
        when(this.consentA.affectsAllFields()).thenReturn(false);
        when(this.consentA.affectsSomeFields()).thenReturn(true);
        when(this.consentB.affectsAllFields()).thenReturn(false);
        when(this.consentB.affectsSomeFields()).thenReturn(true);
        when(this.consentA.getFields()).thenReturn(Collections.singletonList(FIELD_B_LABEL));
        when(this.consentB.getFields()).thenReturn(Collections.singletonList(FIELD_C_LABEL));
        when(this.consentManager.getMissingConsentsForPatient(this.patient)).thenReturn(consents);
        when(this.consentManager.getSystemConsents()).thenReturn(consents);

        final List<RecordElement> elements = Arrays.asList(recordElementA, recordElementB, recordElementC);
        final List<RecordElement> enabledElements = this.component.filterForm(elements, this.patient);
        Assert.assertEquals(1, enabledElements.size());
        Assert.assertEquals(recordElementA, enabledElements.get(0));
    }

    @Test
    public void authorizeInteractionForPatientWhenPatientIsNull()
    {
        Assert.assertFalse(this.component.authorizeInteraction((Patient) null));
    }

    @Test
    public void authorizeInteractionForPatientWhenPatientHasNoMissingConsents()
    {
        when(this.consentManager.getMissingConsentsForPatient(this.patient))
            .thenReturn(Collections.<Consent>emptySet());
        final boolean authorizeInteraction = this.component.authorizeInteraction(this.patient);
        Assert.assertTrue(authorizeInteraction);
    }

    @Test
    public void authorizeInteractionForPatientWhenPatientHasMissingConsentsButNotRequired()
    {
        final Set<Consent> consents = new HashSet<>();
        consents.add(this.consentA);
        consents.add(this.consentB);
        when(this.consentManager.getMissingConsentsForPatient(this.patient)).thenReturn(consents);

        final boolean authorizeInteraction = this.component.authorizeInteraction(this.patient);
        Assert.assertTrue(authorizeInteraction);
    }

    @Test
    public void authorizeInteractionForPatientWhenPatientHasMissingRequiredConsents()
    {
        when(this.consentB.isRequired()).thenReturn(true);
        final Set<Consent> consents = new HashSet<>();
        consents.add(this.consentA);
        consents.add(this.consentB);
        when(this.consentManager.getMissingConsentsForPatient(this.patient)).thenReturn(consents);

        final boolean authorizeInteraction = this.component.authorizeInteraction(this.patient);
        Assert.assertFalse(authorizeInteraction);
    }

    @Test
    public void authorizeInteractionGivenGrantedConsentsSystemConsentsAreNotSpecified()
    {
        when(this.consentManager.getSystemConsents()).thenReturn(Collections.<Consent>emptySet());
        final Set<String> grantedConsents = new HashSet<>();
        grantedConsents.add(FIELD_A_LABEL);

        final boolean authorizeInteraction = this.component.authorizeInteraction(grantedConsents);
        Assert.assertTrue(authorizeInteraction);
    }

    @Test
    public void authorizeInteractionGivenGrantedConsentsSystemConsentsAreSpecified()
    {
        final Set<Consent> systemConsents = new HashSet<>();
        systemConsents.add(this.consentA);
        systemConsents.add(this.consentB);

        when(this.consentManager.getSystemConsents()).thenReturn(systemConsents);
        final Set<String> grantedConsents = new HashSet<>();
        grantedConsents.add(CONSENT_A_LABEL);

        final boolean authorizeInteraction = this.component.authorizeInteraction(grantedConsents);
        Assert.assertTrue(authorizeInteraction);
    }

    @Test
    public void authorizeInteractionGivenGrantedConsentsSystemConsentsAreSpecifiedAndRequired()
    {
        when(this.consentB.isRequired()).thenReturn(true);

        final Set<Consent> systemConsents = new HashSet<>();
        systemConsents.add(this.consentA);
        systemConsents.add(this.consentB);

        when(this.consentManager.getSystemConsents()).thenReturn(systemConsents);
        final Set<String> grantedConsents = new HashSet<>();
        grantedConsents.add(CONSENT_A_LABEL);

        final boolean authorizeInteraction = this.component.authorizeInteraction(grantedConsents);
        Assert.assertFalse(authorizeInteraction);
    }

    @Test
    public void isElementConsentedWithMandatoryConsent()
    {
        final UIExtension extension = mock(UIExtension.class);
        when(extension.getId()).thenReturn(FIELD_A_LABEL);

        final RecordElement element = mock(RecordElement.class);
        when(element.getExtension()).thenReturn(extension);

        final Set<Consent> missingConsents = new HashSet<>();
        missingConsents.add(this.consentA);

        when(this.consentManager.getMissingConsentsForPatient(this.patient)).thenReturn(missingConsents);
        when(this.consentA.isRequired()).thenReturn(true);

        final boolean isElementConsented = this.component.isElementConsented(element, this.patient);
        Assert.assertFalse(isElementConsented);
    }

    @Test
    public void isElementConsentedOneConsentAffectsAllFields()
    {
        final UIExtension extension = mock(UIExtension.class);
        when(extension.getId()).thenReturn(FIELD_A_LABEL);

        final RecordElement element = mock(RecordElement.class);
        when(element.getExtension()).thenReturn(extension);

        final Set<Consent> missingConsents = new HashSet<>();
        missingConsents.add(this.consentA);
        missingConsents.add(this.consentB);

        when(this.consentManager.getMissingConsentsForPatient(this.patient)).thenReturn(missingConsents);
        when(this.consentA.affectsAllFields()).thenReturn(true);
        when(this.consentB.affectsAllFields()).thenReturn(false);

        final List<String> nonConsentedFields = Arrays.asList(FIELD_A_LABEL, FIELD_B_LABEL);
        when(this.consentA.getFields()).thenReturn(nonConsentedFields);

        final boolean isElementConsented = this.component.isElementConsented(element, this.patient);
        Assert.assertFalse(isElementConsented);
    }

    @Test
    public void isElementConsentedConsentsDoNotAffectAnyFields()
    {
        final UIExtension extension = mock(UIExtension.class);
        when(extension.getId()).thenReturn(FIELD_A_LABEL);

        final RecordElement element = mock(RecordElement.class);
        when(element.getExtension()).thenReturn(extension);

        final Set<Consent> missingConsents = new HashSet<>();
        missingConsents.add(this.consentA);
        missingConsents.add(this.consentB);

        when(this.consentManager.getMissingConsentsForPatient(this.patient)).thenReturn(missingConsents);
        when(this.consentA.affectsAllFields()).thenReturn(false);
        when(this.consentB.affectsAllFields()).thenReturn(false);
        when(this.consentA.affectsSomeFields()).thenReturn(false);
        when(this.consentB.affectsSomeFields()).thenReturn(false);

        final List<String> nonConsentedFields = Arrays.asList(FIELD_A_LABEL, FIELD_B_LABEL);
        when(this.consentA.getFields()).thenReturn(nonConsentedFields);

        final boolean isElementConsented = this.component.isElementConsented(element, this.patient);
        Assert.assertTrue(isElementConsented);
    }

    @Test
    public void isElementConsentedConsentsAffectSomeFieldsButNotElement()
    {
        final UIExtension extension = mock(UIExtension.class);
        when(extension.getId()).thenReturn(FIELD_D_LABEL);

        final RecordElement element = mock(RecordElement.class);
        when(element.getExtension()).thenReturn(extension);

        final Set<Consent> missingConsents = new HashSet<>();
        missingConsents.add(this.consentA);
        missingConsents.add(this.consentB);

        when(this.consentManager.getMissingConsentsForPatient(this.patient)).thenReturn(missingConsents);
        when(this.consentA.affectsAllFields()).thenReturn(false);
        when(this.consentB.affectsAllFields()).thenReturn(false);
        when(this.consentA.affectsSomeFields()).thenReturn(true);
        when(this.consentB.affectsSomeFields()).thenReturn(true);

        when(this.consentA.getFields()).thenReturn(Arrays.asList(FIELD_A_LABEL, FIELD_B_LABEL));
        when(this.consentB.getFields()).thenReturn(Collections.singletonList(FIELD_C_LABEL));

        final boolean isElementConsented = this.component.isElementConsented(element, this.patient);
        Assert.assertTrue(isElementConsented);
    }

    @Test
    public void isElementConsentedConsentsAffectSomeFieldsElementNotConsented()
    {
        final UIExtension extension = mock(UIExtension.class);
        when(extension.getId()).thenReturn(FIELD_B_LABEL);

        final RecordElement element = mock(RecordElement.class);
        when(element.getExtension()).thenReturn(extension);

        final Set<Consent> missingConsents = new HashSet<>();
        missingConsents.add(this.consentA);
        missingConsents.add(this.consentB);

        when(this.consentManager.getMissingConsentsForPatient(this.patient)).thenReturn(missingConsents);
        when(this.consentA.affectsAllFields()).thenReturn(false);
        when(this.consentB.affectsAllFields()).thenReturn(false);
        when(this.consentA.affectsSomeFields()).thenReturn(true);
        when(this.consentB.affectsSomeFields()).thenReturn(true);

        when(this.consentA.getFields()).thenReturn(Arrays.asList(FIELD_A_LABEL, FIELD_B_LABEL));
        when(this.consentB.getFields()).thenReturn(Collections.singletonList(FIELD_C_LABEL));

        final boolean isElementConsented = this.component.isElementConsented(element, this.patient);
        Assert.assertFalse(isElementConsented);
    }
}
