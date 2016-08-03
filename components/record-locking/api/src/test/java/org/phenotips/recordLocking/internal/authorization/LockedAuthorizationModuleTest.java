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
package org.phenotips.recordLocking.internal.authorization;

import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

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
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link LockedAuthorizationModuleTest locked} {@link AuthorizationModule} component.
 *
 * @version $Id$
 */
public class LockedAuthorizationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<AuthorizationModule>(LockedAuthorizationModule.class);

    @Mock
    private User user;

    @Mock
    private Right right;

    @Mock
    private DocumentReference documentReference;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    private Provider<XWikiContext> contextProvider;

    @Before
    public void setup() throws ComponentLookupException, XWikiException
    {
        MockitoAnnotations.initMocks(this);
        ParameterizedType cpType = new DefaultParameterizedType(null, Provider.class, XWikiContext.class);
        this.contextProvider = this.mocker.getInstance(cpType);

        Mockito.doReturn(this.context).when(this.contextProvider).get();
        Mockito.doReturn(this.xwiki).when(this.context).getWiki();

    }

    @Test
    public void ignoresDocumentsWithoutPatientLockObjects() throws ComponentLookupException, XWikiException
    {
        Mockito.doReturn(this.document).when(this.xwiki).getDocument(this.documentReference, this.context);
        when(this.document.getXObject(Matchers.<EntityReference>any())).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.documentReference));
    }

    @Test
    public void ignoresNonDocumentReferences() throws ComponentLookupException
    {
        Assert.assertNull(
            this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.documentReference.getParent()));
    }

    @Test
    public void ignoresWhenActionIsReadOnly() throws ComponentLookupException, XWikiException
    {
        Mockito.doReturn(this.document).when(this.xwiki).getDocument(this.documentReference, this.context);

        BaseObject lock = mock(BaseObject.class);
        Mockito.doReturn(Boolean.TRUE).when(this.right).isReadOnly();
        when(this.document.getXObject(Matchers.<EntityReference>any())).thenReturn(lock);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.documentReference));
    }

    @Test
    public void returnsFalseWhenLockedAndRightCanEdit() throws ComponentLookupException, XWikiException
    {
        Mockito.doReturn(this.document).when(this.xwiki).getDocument(this.documentReference, this.context);

        BaseObject lock = mock(BaseObject.class);
        when(this.right.isReadOnly()).thenReturn(Boolean.FALSE);
        when(this.document.getXObject(Matchers.<EntityReference>any())).thenReturn(lock);
        Assert
            .assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.documentReference));
    }

    @Test
    public void returnsNullWhenExceptionIsThrown() throws ComponentLookupException, XWikiException
    {
        Mockito.doThrow(new XWikiException()).when(this.xwiki).getDocument(this.documentReference, this.context);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.documentReference));
    }

    @Test
    public void nullArgumentsAreIgnored() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(null, null, null));
    }

    @Test
    public void expectedPriority() throws ComponentLookupException
    {
        Assert.assertEquals(110, this.mocker.getComponentUnderTest().getPriority());
    }
}
