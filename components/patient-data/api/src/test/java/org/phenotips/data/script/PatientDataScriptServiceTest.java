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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PatientDataScriptService} component.
 *
 * @version $Id$
 * @since 1.0M1
 */
public class PatientDataScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientDataScriptService> mocker =
        new MockitoComponentMockingRule<>(PatientDataScriptService.class);

    @Mock
    private Patient patient;

    private PatientRepository repo;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.repo = this.mocker.getInstance(PatientRepository.class, "secure");
    }

    @Test
    public void getForwardsCalls() throws ComponentLookupException
    {
        when(this.repo.get("P0123456")).thenReturn(this.patient);
        Assert.assertSame(this.patient, this.mocker.getComponentUnderTest().get("P0123456"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void getPatientByIdForwardsCalls() throws ComponentLookupException
    {
        when(this.repo.get("P0123456")).thenReturn(this.patient);
        Assert.assertSame(this.patient, this.mocker.getComponentUnderTest().getPatientById("P0123456"));
    }

    @Test
    public void getCatchesUnauthorizedException() throws ComponentLookupException
    {
        when(this.repo.get("P0123456")).thenThrow(new SecurityException("Unauthorized"));
        Assert.assertNull(this.mocker.getComponentUnderTest().get("P0123456"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void getPatientByIdCatchesUnauthorizedException() throws ComponentLookupException
    {
        when(this.repo.get("P0123456")).thenThrow(new SecurityException("Unauthorized"));
        Assert.assertNull(this.mocker.getComponentUnderTest().getPatientById("P0123456"));
    }

    @Test
    public void getPatientByExternalIdForwardsCalls() throws ComponentLookupException
    {
        when(this.repo.getByName("Neuro123")).thenReturn(this.patient);
        Assert.assertSame(this.patient, this.mocker.getComponentUnderTest().getPatientByExternalId("Neuro123"));
    }

    @Test
    public void getPatientByExternalIdCatchesUnauthorizedException() throws ComponentLookupException
    {
        when(this.repo.getByName("Neuro123")).thenThrow(new SecurityException("Unauthorized"));
        Assert.assertNull(this.mocker.getComponentUnderTest().getPatientByExternalId("Neuro123"));
    }

    @Test
    public void createForwardsCalls() throws ComponentLookupException
    {
        when(this.repo.create()).thenReturn(this.patient);
        Assert.assertSame(this.patient, this.mocker.getComponentUnderTest().create());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void createNewPatientForwardsCalls() throws ComponentLookupException
    {
        when(this.repo.create()).thenReturn(this.patient);
        Assert.assertSame(this.patient, this.mocker.getComponentUnderTest().createNewPatient());
    }

    @Test
    public void createCatchesUnauthorizedException() throws ComponentLookupException
    {
        when(this.repo.create()).thenThrow(new SecurityException("Unauthorized"));
        Assert.assertNull(this.mocker.getComponentUnderTest().create());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void createNewCatchesUnauthorizedException() throws ComponentLookupException
    {
        when(this.repo.create()).thenThrow(new SecurityException("Unauthorized"));
        Assert.assertNull(this.mocker.getComponentUnderTest().createNewPatient());
    }
}
