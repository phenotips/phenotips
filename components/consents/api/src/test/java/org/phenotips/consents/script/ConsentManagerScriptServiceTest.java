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
package org.phenotips.consents.script;

import org.phenotips.consents.Consent;
import org.phenotips.consents.ConsentManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.LinkedHashSet;
import java.util.Set;

import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class ConsentManagerScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<ConsentManagerScriptService> mocker = new MockitoComponentMockingRule<>(
        ConsentManagerScriptService.class);

    private ConsentManager consentManager;

    @Mock
    private Consent consent1;

    @Mock
    private Consent consent2;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.consentManager = this.mocker.getInstance(ConsentManager.class);
    }

    @Test
    public void hasConsentForwardsCalls() throws ComponentLookupException
    {
        when(this.consentManager.hasConsent("P0123456", "consent1")).thenReturn(true);
        when(this.consentManager.hasConsent("P0123456", "consent2")).thenReturn(false);

        Assert.assertTrue(this.mocker.getComponentUnderTest().hasConsent("P0123456", "consent1"));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasConsent("P0123456", "consent2"));
    }

    @Test
    public void getAllConsentsForPatientForwardsCalls() throws ComponentLookupException
    {
        Set<Consent> consents = new LinkedHashSet<>();
        consents.add(this.consent1);
        consents.add(this.consent2);
        when(this.consentManager.getAllConsentsForPatient("P0123456")).thenReturn(consents);
        JSONArray result = new JSONArray();
        when(this.consentManager.toJSON(consents)).thenReturn(result);

        Assert.assertSame(result, this.mocker.getComponentUnderTest().getAllConsentsForPatient("P0123456"));
    }

    @Test
    public void getAllConsentsForPatientCatchesExceptionsAndReturnsNull() throws ComponentLookupException
    {
        when(this.consentManager.getAllConsentsForPatient("P0123456")).thenThrow(new NullPointerException());

        Assert.assertNull(this.mocker.getComponentUnderTest().getAllConsentsForPatient("P0123456"));
    }
}
