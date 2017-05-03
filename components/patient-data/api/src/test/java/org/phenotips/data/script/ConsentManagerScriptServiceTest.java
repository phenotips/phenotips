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
package org.phenotips.data.script;

import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Consent;
import org.phenotips.data.ConsentManager;
import org.phenotips.data.Patient;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

public class ConsentManagerScriptServiceTest {

    @Rule
    public final MockitoComponentMockingRule<ConsentManagerScriptService> mocker = new MockitoComponentMockingRule<>(
            ConsentManagerScriptService.class);

    private ConsentManager consentManager;

    @Mock
    private Patient patient;

    @Mock
    private Consent consent1;

    @Mock
    private Consent consent2;

    @Before
    public void setUp() throws ComponentLookupException {
        MockitoAnnotations.initMocks(this);
        this.consentManager = this.mocker.getInstance(ConsentManager.class);
    }

    @Test
    public void hasConsentTest() throws ComponentLookupException {
        when(this.consentManager.hasConsent("P0123456", "consent")).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasConsent("P0123456", "consent"));
    }

    @Test
    public void hasNoConsentTest() throws ComponentLookupException {
        when(this.consentManager.hasConsent("P0123456", "consent")).thenReturn(false);
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasConsent("P0123456", "consent"));
    }

    @Test
    public void invalidPatientId() throws ComponentLookupException {
        Set<Consent> consents = new LinkedHashSet<>();
        consents.add(consent1);
        consents.add(consent2);

        when(this.consentManager.toJSON(consents)).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getAllConsentsForPatient(Patient.JSON_KEY_ID));
    }

    @Test
    public void getConsentsForPatientWithException() throws ComponentLookupException {
        Set<Consent> consents = new LinkedHashSet<>();
        Assert.assertTrue(consents.isEmpty());

        when(this.consentManager.getAllConsentsForPatient(patient)).thenThrow(new NullPointerException());
        Assert.assertNull(this.mocker.getComponentUnderTest().getAllConsentsForPatient(Patient.JSON_KEY_ID));
    }

}
