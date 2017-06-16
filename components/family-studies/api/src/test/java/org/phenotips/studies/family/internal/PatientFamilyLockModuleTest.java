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
package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.locks.LockModule;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Date;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PatientFamilyLockModule}.
 *
 * @version $Id$
 */
public class PatientFamilyLockModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<LockModule> mocker =
        new MockitoComponentMockingRule<>(PatientFamilyLockModule.class);

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    private DocumentReference currentDocumentReference = new DocumentReference("xwiki", "data", "P01");

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument patientDoc;

    private DocumentReference familyDocumentReference = new DocumentReference("xwiki", "Family", "F01");

    @Mock
    private Family family;

    @Mock
    private XWikiDocument familyDoc;

    @Mock
    private XWikiLock lock;

    @Mock
    private Date lockDate;

    @Mock
    private User lockingUser;

    @Mock
    private User otherUser;

    private FamilyRepository familyRepository;

    private PatientRepository patientRepository;

    private UserManager userManager;

    @Before
    public void setup() throws ComponentLookupException, XWikiException
    {
        MockitoAnnotations.initMocks(this);

        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDocument(this.currentDocumentReference, this.context)).thenReturn(this.patientDoc);
        when(this.xwiki.getDocument(this.familyDocumentReference, this.context)).thenReturn(this.familyDoc);

        when(this.patientDoc.getDocumentReference()).thenReturn(this.currentDocumentReference);

        this.patientRepository = this.mocker.getInstance(PatientRepository.class);
        when(this.patientRepository.get(this.currentDocumentReference.getName())).thenReturn(this.patient);

        this.familyRepository = this.mocker.getInstance(FamilyRepository.class);
        when(this.familyRepository.getFamilyForPatient(this.patient)).thenReturn(this.family);

        when(this.family.getXDocument()).thenReturn(this.familyDoc);
        when(this.family.getDocumentReference()).thenReturn(this.familyDocumentReference);
        when(this.familyDoc.getLock(this.context)).thenReturn(this.lock);

        when(this.lock.getDate()).thenReturn(this.lockDate);
        when(this.lock.getUserName()).thenReturn("lockerUser");

        this.userManager = this.mocker.getInstance(UserManager.class);
        when(this.userManager.getUser("lockerUser")).thenReturn(this.lockingUser);
        when(this.lockingUser.getId()).thenReturn("lockerUser");
        when(this.userManager.getUser("otherUser")).thenReturn(this.otherUser);
        when(this.otherUser.getId()).thenReturn("otherUser");
    }

    @Test
    public void lockedWhenFamilyIsLockedByDifferentUser() throws ComponentLookupException, XWikiException
    {
        when(this.userManager.getCurrentUser()).thenReturn(this.otherUser);

        Assert.assertNotNull(this.mocker.getComponentUnderTest().getLock(this.currentDocumentReference));
    }

    @Test
    public void lockedWhenFamilyIsLockedBySameUser() throws ComponentLookupException, XWikiException
    {
        when(this.userManager.getCurrentUser()).thenReturn(this.lockingUser);

        Assert.assertNotNull(this.mocker.getComponentUnderTest().getLock(this.currentDocumentReference));
    }

    @Test
    public void noLockIfFamilyIsNotLocked() throws ComponentLookupException, XWikiException
    {
        when(this.familyDoc.getLock(this.context)).thenReturn(null);

        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.currentDocumentReference));
    }

    @Test
    public void exceptionsAreCaughtAndNoLockIsReturned() throws ComponentLookupException, XWikiException
    {
        when(this.context.getWiki().getDocument(this.currentDocumentReference, this.context))
            .thenThrow(new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC,
                "Exception while reading document [xwiki:PhenoTips.FamilyLockModule]"));
        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.currentDocumentReference));
        verify(this.mocker.getMockedLogger()).error(anyString(), anyString(),
            Matchers.any(XWikiException.class));
    }

    @Test
    public void noLockForNonPatientDocument() throws ComponentLookupException, XWikiException
    {
        when(this.patientRepository.get(this.currentDocumentReference.getName())).thenReturn(null);

        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.currentDocumentReference));
    }

    @Test
    public void noLockForPatientWithoutFamily() throws ComponentLookupException, XWikiException
    {
        when(this.familyRepository.getFamilyForPatient(this.patient)).thenReturn(null);

        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.currentDocumentReference));
    }

    @Test
    public void noLockForNullDocument() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(null));
    }

    @Test
    public void priorityIsCorrect() throws ComponentLookupException
    {
        Assert.assertEquals(300, this.mocker.getComponentUnderTest().getPriority());
    }
}
