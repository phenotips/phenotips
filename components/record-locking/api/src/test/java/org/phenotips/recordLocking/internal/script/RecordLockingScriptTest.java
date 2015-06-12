package org.phenotips.recordLocking.internal.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.recordLocking.PatientRecordLockManager;
import org.phenotips.recordLocking.script.RecordLockingService;

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

/**
 * Tests for {@link org.phenotips.recordLocking.script.RecordLockingService}
 */
public class RecordLockingScriptTest
{
    @Rule
    public final MockitoComponentMockingRule<RecordLockingService> mocker =
        new MockitoComponentMockingRule<RecordLockingService>(RecordLockingService.class);

    private PatientRecordLockManager lockManager;

    private PatientRepository pr;

    @Mock
    private Patient patient;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        //Mocked injected components
        this.lockManager = this.mocker.getInstance(PatientRecordLockManager.class);
        this.pr = this.mocker.getInstance(PatientRepository.class);
    }

    @Test
    public void respondsOkOnSuccessfulLock() throws ComponentLookupException
    {
        Mockito.doReturn(patient).when(pr).getPatientById(Matchers.anyString());
        Mockito.doReturn(true).when(lockManager).lockPatientRecord(patient);

        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_OK, lockingService.lockPatient("123"));
    }

    @Test
    public void respondsWithErrorOnUnsuccessfulLock() throws ComponentLookupException
    {
        Mockito.doReturn(patient).when(pr).getPatientById(Matchers.anyString());
        Mockito.doReturn(false).when(lockManager).lockPatientRecord(patient);

        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, lockingService.lockPatient("123"));
    }

    @Test
    public void respondsWithErrorWhenLockingNonExistentPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(pr).getPatientById(Matchers.anyString());
        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, lockingService.lockPatient("123"));
    }

    @Test
    public void respondsOkOnSuccessfulUnlock() throws ComponentLookupException
    {
        Mockito.doReturn(patient).when(pr).getPatientById(Matchers.anyString());
        Mockito.doReturn(true).when(lockManager).unlockPatientRecord(patient);

        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_OK, lockingService.unlockPatient("123"));
    }

    @Test
    public void respondsWithErrorOnUnsuccessfulUnlock() throws ComponentLookupException
    {
        Mockito.doReturn(patient).when(pr).getPatientById(Matchers.anyString());
        Mockito.doReturn(false).when(lockManager).lockPatientRecord(patient);

        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, lockingService.unlockPatient("123"));
    }

    @Test
    public void respondsWithErrorWhenUnlockingNonExistentPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(pr).getPatientById(Matchers.anyString());
        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, lockingService.lockPatient("123"));
    }

    @Test
    public void isLockedCallsInternalLockManager () throws ComponentLookupException
    {
        Mockito.doReturn(true).when(lockManager).isLocked(patient);
        RecordLockingService lockingService = this.mocker.getComponentUnderTest();
        lockingService.isLocked(patient);
        verify(lockManager).isLocked(patient);
    }

}
