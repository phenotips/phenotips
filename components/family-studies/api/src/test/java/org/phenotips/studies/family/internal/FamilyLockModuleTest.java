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
import static org.mockito.Mockito.doReturn;
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
    private Family family;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument xdoc;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiLock xlock;

    @Mock
    private User user;

    @Mock
    private User currentUser;

    private DocumentReference doc = new DocumentReference("xwiki", "Family", "F01");

    @Before
    public void setup() throws ComponentLookupException, XWikiException
    {
        MockitoAnnotations.initMocks(this);

        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(contextProvider.get()).thenReturn(this.context);
        doReturn(this.xwiki).when(this.context).getWiki();
    }

    @Test
    public void familyLockTest() throws ComponentLookupException, XWikiException
    {
        FamilyRepository repo = this.mocker.getInstance(FamilyRepository.class);
        when(this.context.getWiki().getDocument(this.doc, this.context)).thenReturn(this.xdoc);
        when(this.xdoc.getDocumentReference()).thenReturn(this.doc);
        when(repo.getFamilyById("F01")).thenReturn(this.family);

        UserManager userManager = this.mocker.getInstance(UserManager.class);
        when(this.xdoc.getLock(this.context)).thenReturn(this.xlock);
        when(this.xlock.getUserName()).thenReturn("Donut");
        when(userManager.getUser(this.xlock.getUserName())).thenReturn(this.user);
        when(userManager.getCurrentUser()).thenReturn(this.currentUser);
        when(this.user.getId()).thenReturn("User 1");
        when(this.currentUser.getId()).thenReturn("User 2");

        Assert.assertNotNull(this.mocker.getComponentUnderTest().getLock(this.doc));
    }

    @Test
    public void sameUserNotLockedTest() throws ComponentLookupException, XWikiException
    {
        FamilyRepository repo = this.mocker.getInstance(FamilyRepository.class);
        when(this.context.getWiki().getDocument(this.doc, this.context)).thenReturn(this.xdoc);
        when(this.xdoc.getDocumentReference()).thenReturn(this.doc);
        when(repo.getFamilyById("F01")).thenReturn(this.family);

        UserManager userManager = this.mocker.getInstance(UserManager.class);
        when(this.xdoc.getLock(this.context)).thenReturn(this.xlock);
        when(this.xlock.getUserName()).thenReturn("Donut");
        when(userManager.getUser(this.xlock.getUserName())).thenReturn(this.user);
        when(userManager.getCurrentUser()).thenReturn(this.currentUser);
        when(this.user.getId()).thenReturn("User");
        when(this.currentUser.getId()).thenReturn("User");

        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.doc));
    }

    @Test
    public void emptyFamily() throws ComponentLookupException, XWikiException
    {
        FamilyRepository repo = this.mocker.getInstance(FamilyRepository.class);
        when(this.context.getWiki().getDocument(this.doc, this.context)).thenReturn(this.xdoc);
        when(this.xdoc.getDocumentReference()).thenReturn(this.doc);
        when(repo.getFamilyById("F01")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.doc));
    }

    @Test
    public void nullDocumentTest() throws ComponentLookupException, XWikiException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(null));
    }

    @Test
    public void throwsXWikiExceptionTest() throws ComponentLookupException, XWikiException
    {
        when(this.context.getWiki().getDocument(this.doc, this.context))
            .thenThrow(new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC,
                "Exception while reading document [xwiki:PhenoTips.FamilyLockModule]"));
        this.mocker.getComponentUnderTest().getLock(this.doc);
        verify(this.mocker.getMockedLogger()).error(anyString(), anyString(),
            Matchers.any(XWikiException.class));
    }

    @Test
    public void priorityIsCorrectTest() throws ComponentLookupException
    {
        Assert.assertEquals(400, this.mocker.getComponentUnderTest().getPriority());
    }
}
