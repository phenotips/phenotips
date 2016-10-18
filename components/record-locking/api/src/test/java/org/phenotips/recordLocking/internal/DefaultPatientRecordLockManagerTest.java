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

/**
 * Tests for the {@link DefaultPatientRecordLockManager}.
 *
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

        // Mock injected components
        this.pm = this.mocker.getInstance(PermissionsManager.class);
        this.manageAccessLevel = this.mocker.getInstance(AccessLevel.class, "manage");

        // Provider is special and must be mocked differently
        ParameterizedType cpType = new DefaultParameterizedType(null, Provider.class, XWikiContext.class);
        this.contextProvider = this.mocker.getInstance(cpType);

        // Common behaviour to mock
        Mockito.doReturn(this.context).when(this.contextProvider).get();
        Mockito.doReturn(this.xwiki).when(this.context).getWiki();
        Mockito.doReturn(this.patientDocumentReference).when(this.patient).getDocumentReference();
        Mockito.doReturn(this.patientDocument).when(this.xwiki).getDocument(this.patientDocumentReference,
            this.context);
        Mockito.doReturn(this.patientAccess).when(this.pm).getPatientAccess(this.patient);

    }

    @Test
    public void canLockPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(this.patientDocument).getXObject(Matchers.<EntityReference>any());

        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertTrue(mockedLockManager.lockPatientRecord(this.patient));
    }

    @Test
    public void wontLockWithoutManageAccess() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(this.patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(false).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.lockPatientRecord(this.patient));
    }

    @Test
    public void wontLockLockedPatient() throws ComponentLookupException
    {
        Mockito.doReturn(this.lock).when(this.patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.lockPatientRecord(this.patient));
    }

    @Test
    public void returnsFalseWhenLockingNullPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(this.patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.lockPatientRecord(null));
    }

    @Test
    public void canUnlockPatient() throws ComponentLookupException
    {
        Mockito.doReturn(this.lock).when(this.patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertTrue(mockedLockManager.unlockPatientRecord(this.patient));
    }

    @Test
    public void wontUnlockWithoutManageAccess() throws ComponentLookupException
    {
        Mockito.doReturn(this.lock).when(this.patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(false).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.unlockPatientRecord(this.patient));
    }

    @Test
    public void wontUnlockUnlockedPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(this.patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.unlockPatientRecord(this.patient));
    }

    @Test
    public void returnsFalseWhenUnlockingNullPatient() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(this.patientDocument).getXObject(Matchers.<EntityReference>any());
        Mockito.doReturn(true).when(this.patientAccess).hasAccessLevel(this.manageAccessLevel);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.unlockPatientRecord(null));
    }

    @Test
    public void testIsLockedTrue() throws ComponentLookupException
    {
        Mockito.doReturn(this.lock).when(this.patientDocument).getXObject(Matchers.<EntityReference>any());
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertTrue(mockedLockManager.isLocked(this.patient));
    }

    @Test
    public void testIsLockedFalse() throws ComponentLookupException
    {
        Mockito.doReturn(null).when(this.patientDocument).getXObject(Matchers.<EntityReference>any());
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.isLocked(this.patient));
    }

    @Test
    public void returnsFalseAfterXWikiExceptionWhileRetrievingDocument() throws ComponentLookupException, XWikiException
    {
        Mockito.doThrow(new XWikiException()).when(this.xwiki).getDocument(this.patientDocumentReference, this.context);
        PatientRecordLockManager mockedLockManager = this.mocker.getComponentUnderTest();
        Assert.assertFalse(mockedLockManager.lockPatientRecord(this.patient));
    }

}
