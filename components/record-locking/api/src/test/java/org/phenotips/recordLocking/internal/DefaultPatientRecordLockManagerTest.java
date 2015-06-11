package org.phenotips.recordLocking.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.recordLocking.PatientRecordLockManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.ParameterizedType;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link DefaultPatientRecordLockManager}.
 * @version $Id$
 */
public class DefaultPatientRecordLockManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientRecordLockManager> mocker =
        new MockitoComponentMockingRule<PatientRecordLockManager>(DefaultPatientRecordLockManager.class);

    private PermissionsManager pm;

    private Provider<XWikiContext> contextProvider;

    private AccessLevel manageAccessLevel;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Mock
    private Patient patient;

    @Mock
    private DocumentReference patientDocumentReference;

    @Mock
    private XWikiDocument patientDocument;

    @Mock
    private BaseObject lock;

    @Mock
    private PatientAccess patientAccess;



    @Before
    public void setup() throws ComponentLookupException, XWikiException
    {
        MockitoAnnotations.initMocks(this);

        //Mock injected components
        this.pm = this.mocker.getInstance(PermissionsManager.class);
        this.manageAccessLevel = this.mocker.getInstance(AccessLevel.class, "manage");

        //Provider is special and must be mocked differently
        ParameterizedType cpType = new DefaultParameterizedType(null, Provider.class, XWikiContext.class);
        contextProvider = this.mocker.getInstance(cpType);

        //Common behaviour to mock
        Mockito.doReturn(context).when(contextProvider).get();
        Mockito.doReturn(xwiki).when(context).getWiki();
        Mockito.doReturn(patientDocumentReference).when(patient).getDocument();
        Mockito.doReturn(patientDocument).when(xwiki).getDocument(patientDocumentReference, context);
        Mockito.doReturn(patientAccess).when(pm).getPatientAccess(patient);

    }

    @Test
    public void canLockPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(patientDocument).getXObject(Matchers.<EntityReference>any());

        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertTrue(mockedLockManager.lockPatientRecord(patient));
    }

    @Test
    public void wontLockWithoutManageAccess() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(false).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.lockPatientRecord(patient));
    }

    @Test
    public void wontLockLockedPatient() throws ComponentLookupException
    {
        Mockito.doReturn(lock).when(patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.lockPatientRecord(patient));
    }

    @Test
    public void returnsFalseWhenLockingNullPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.lockPatientRecord(null));
    }

    @Test
    public void canUnlockPatient() throws ComponentLookupException
    {
        Mockito.doReturn(lock).when(patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertTrue(mockedLockManager.unlockPatientRecord(patient));
    }

    @Test
    public void wontUnlockWithoutManageAccess() throws ComponentLookupException
    {
        Mockito.doReturn(lock).when(patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(false).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.unlockPatientRecord(patient));
    }


    @Test
    public void wontUnlockUnlockedPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.unlockPatientRecord(patient));
    }

    @Test
    public void returnsFalseWhenUnlockingNullPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.unlockPatientRecord(null));
    }


    @Test
    public void testIsLockedTrue() throws ComponentLookupException
    {
        Mockito.doReturn(lock).when(patientDocument).getXObject(Matchers.<EntityReference>any());
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertTrue(mockedLockManager.isLocked(patient));
    }

    @Test
    public void testIsLockedFalse() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(patientDocument).getXObject(Matchers.<EntityReference>any());
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.isLocked(patient));
    }

    @Test
    public void returnsFalseAfterXWikiExceptionWhileRetrievingDocument() throws ComponentLookupException, XWikiException
    {
        Mockito.doThrow(new XWikiException()).when(xwiki).getDocument(patientDocumentReference, context);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.lockPatientRecord(patient));
    }

}
