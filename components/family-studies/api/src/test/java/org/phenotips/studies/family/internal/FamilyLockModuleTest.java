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
 * Tests for the {@link FamilyLockModule}.
 *
 * @version $Id$
 */
public class FamilyLockModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<LockModule> mocker =
        new MockitoComponentMockingRule<>(FamilyLockModule.class);

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    private DocumentReference currentDocumentReference = new DocumentReference("xwiki", "Family", "F01");

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

    private UserManager userManager;

    private FamilyRepository familyRepository;

    @Before
    public void setup() throws ComponentLookupException, XWikiException
    {
        MockitoAnnotations.initMocks(this);

        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDocument(this.currentDocumentReference, this.context)).thenReturn(this.familyDoc);
        when(this.familyDoc.getDocumentReference()).thenReturn(this.currentDocumentReference);

        this.familyRepository = this.mocker.getInstance(FamilyRepository.class);
        when(this.familyRepository.getFamilyById(this.currentDocumentReference.getName())).thenReturn(this.family);

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
    public void noLockReturnedIfNotLocked() throws ComponentLookupException, XWikiException
    {
        when(this.familyDoc.getLock(this.context)).thenReturn(null);
        when(this.userManager.getCurrentUser()).thenReturn(this.lockingUser);

        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.currentDocumentReference));
    }

    @Test
    public void differentUserGetsLockedWhenFamilyIsLocked() throws ComponentLookupException, XWikiException
    {
        when(this.userManager.getCurrentUser()).thenReturn(this.otherUser);

        Assert.assertNotNull(this.mocker.getComponentUnderTest().getLock(this.currentDocumentReference));
    }

    @Test
    public void sameUserIsNotLocked() throws ComponentLookupException, XWikiException
    {
        when(this.userManager.getCurrentUser()).thenReturn(this.lockingUser);

        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.currentDocumentReference));
    }

    @Test
    public void noLockForNonFamilyDocument() throws ComponentLookupException, XWikiException
    {
        when(this.familyRepository.getFamilyById(this.currentDocumentReference.getName())).thenReturn(null);

        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.currentDocumentReference));
    }

    @Test
    public void noLockForNullDocument() throws ComponentLookupException, XWikiException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(null));
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
    public void priorityIsCorrect() throws ComponentLookupException
    {
        Assert.assertEquals(400, this.mocker.getComponentUnderTest().getPriority());
    }
}
