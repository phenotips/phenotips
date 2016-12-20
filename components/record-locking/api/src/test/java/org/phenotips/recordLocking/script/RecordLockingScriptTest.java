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
package org.phenotips.recordLocking.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.recordLocking.PatientRecordLockManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link org.phenotips.recordLocking.script.RecordLockingService}
 *
 * @version $Id$
 */
public class RecordLockingScriptTest
{
    @Rule
    public final MockitoComponentMockingRule<RecordLockingService> mocker =
        new MockitoComponentMockingRule<>(RecordLockingService.class);

    private PatientRecordLockManager lockManager;

    private PatientRepository pr;

    @Mock
    private Patient scriptablePatient;

    @Mock
    private Patient securePatient;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        // Mocked injected components
        this.lockManager = this.mocker.getInstance(PatientRecordLockManager.class);
        this.pr = this.mocker.getInstance(PatientRepository.class);

        when(this.pr.get(this.scriptablePatient.getDocumentReference())).thenReturn(this.securePatient);
        when(this.pr.get(this.securePatient.getDocumentReference())).thenReturn(this.securePatient);
        when(this.pr.get(Matchers.anyString())).thenReturn(this.securePatient);
    }

    @Test
    public void respondsOkOnSuccessfulLock() throws ComponentLookupException
    {
        Mockito.doReturn(true).when(this.lockManager).lockPatientRecord(this.securePatient);

        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_OK, lockingService.lockPatient("123"));
    }

    @Test
    public void respondsWithErrorOnUnsuccessfulLock() throws ComponentLookupException
    {
        Mockito.doReturn(false).when(this.lockManager).lockPatientRecord(this.securePatient);

        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, lockingService.lockPatient("123"));
    }

    @Test
    public void respondsWithErrorWhenLockingNullPatient() throws ComponentLookupException
    {
        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, lockingService.lockPatient((Patient) null));
    }

    @Test
    public void respondsWithErrorWhenLockingNonExistentPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(this.pr).get(Matchers.anyString());
        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, lockingService.lockPatient("123"));
    }

    @Test
    public void respondsOkOnSuccessfulUnlock() throws ComponentLookupException
    {
        Mockito.doReturn(true).when(this.lockManager).unlockPatientRecord(this.securePatient);

        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_OK, lockingService.unlockPatient("123"));
    }

    @Test
    public void respondsWithErrorOnUnsuccessfulUnlock() throws ComponentLookupException
    {
        Mockito.doReturn(false).when(this.lockManager).lockPatientRecord(this.securePatient);

        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, lockingService.unlockPatient("123"));
    }

    @Test
    public void respondsWithErrorWhenUnlockingNullPatient() throws ComponentLookupException
    {
        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, lockingService.unlockPatient((Patient) null));
    }

    @Test
    public void respondsWithErrorWhenUnlockingNonExistentPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(this.pr).get(Matchers.anyString());
        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, lockingService.unlockPatient("123"));
    }

    @Test
    public void isLockedCallsInternalLockManager() throws ComponentLookupException
    {
        Mockito.doReturn(true).when(this.lockManager).isLocked(this.securePatient);
        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertTrue(lockingService.isLocked(this.scriptablePatient));
        verify(this.lockManager).isLocked(this.securePatient);
    }

    @Test
    public void respondsFalseWhenCheckingNullPatientLock() throws ComponentLookupException
    {
        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertFalse(lockingService.isLocked((Patient) null));
    }

}
