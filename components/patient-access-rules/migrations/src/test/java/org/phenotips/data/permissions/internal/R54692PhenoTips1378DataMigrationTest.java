/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.Visibility;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link R54590PhenoTips931DataMigration}.
 *
 * @version $Id$
 */
public class R54692PhenoTips1378DataMigrationTest
{
    @Rule
    public final MockitoComponentMockingRule<HibernateDataMigration> mocker =
        new MockitoComponentMockingRule<HibernateDataMigration>(R54692PhenoTips1378DataMigration.class);

    /** Sending an event with a non-patient document doesn't alter the document. */
    @Test
    public void hibernateMigrate() throws Exception
    {
        this.mocker.registerMockComponent(ComponentManager.class);
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class);
        XWikiHibernateStore store = mock(XWikiHibernateStore.class);
        when(cm.getInstance(XWikiStoreInterface.class, "hibernate")).thenReturn(store);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext xc = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(xc);
        @SuppressWarnings("deprecation")
        ArgumentCaptor<HibernateCallback<Object>> callbackCaptor = new ArgumentCaptor<HibernateCallback<Object>>();
        this.mocker.getComponentUnderTest().migrate();
        Mockito.verify(store).executeWrite(Matchers.same(xc), callbackCaptor.capture());

        XWiki xwiki = mock(XWiki.class);
        when(xc.getWiki()).thenReturn(xwiki);
        HibernateCallback<Object> callback = callbackCaptor.getValue();
        Assert.assertNotNull(callback);
        Session session = mock(Session.class);
        Query q = mock(Query.class);
        when(session.createQuery(Matchers.anyString())).thenReturn(q);
        List<String> docs = new ArrayList<String>();
        docs.add("data.hasNoVisibility");
        when(q.list()).thenReturn(docs);
        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");

        DocumentReference r1 = new DocumentReference("xwiki", "data", "hasNoVisibility");
        when(resolver.resolve("data.hasNoVisibility")).thenReturn(r1);
        XWikiDocument p1 = mock(XWikiDocument.class);
        when(xwiki.getDocument(r1, xc)).thenReturn(p1);
        BaseObject o1 = mock(BaseObject.class);
        when(p1.newXObject(Visibility.CLASS_REFERENCE, xc)).thenReturn(o1);
        callback.doInHibernate(session);
        verify(o1).set("visibility", "private", xc);
    }

    /** Although no migration exception should be thrown during migrations, such an exception won't break the code. */
    @Test
    public void hibernateMigrateIgnoresMigrationErrors() throws Exception
    {
        this.mocker.registerMockComponent(ComponentManager.class);
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class);
        XWikiHibernateStore store = mock(XWikiHibernateStore.class);
        when(cm.getInstance(XWikiStoreInterface.class, "hibernate")).thenReturn(store);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext xc = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(xc);
        @SuppressWarnings("deprecation")
        ArgumentCaptor<HibernateCallback<Object>> callbackCaptor = new ArgumentCaptor<HibernateCallback<Object>>();
        this.mocker.getComponentUnderTest().migrate();
        Mockito.verify(store).executeWrite(Matchers.same(xc), callbackCaptor.capture());

        XWiki xwiki = mock(XWiki.class);
        when(xc.getWiki()).thenReturn(xwiki);
        HibernateCallback<Object> callback = callbackCaptor.getValue();
        Assert.assertNotNull(callback);
        Session session = mock(Session.class);
        Query q = mock(Query.class);
        when(session.createQuery(Matchers.anyString())).thenReturn(q);
        List<String> docs = new ArrayList<String>();
        docs.add("data.ThrowsDME");
        when(q.list()).thenReturn(docs);
        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");

        DocumentReference r2 = new DocumentReference("xwiki", "data", "ThrowsDME");
        when(resolver.resolve("data.ThrowsDME")).thenReturn(r2);
        XWikiDocument p2 = mock(XWikiDocument.class);
        when(xwiki.getDocument(r2, xc)).thenReturn(p2);
        BaseObject o2 = mock(BaseObject.class);
        when(p2.newXObject(Visibility.CLASS_REFERENCE, xc)).thenReturn(o2);
        when(cm.getInstance(XWikiStoreInterface.class,
            "hibernate")).thenThrow(new ComponentLookupException("Nope"));
        callback.doInHibernate(session);
    }

    /** Unreadable data doesn't affect the migrator. */
    @Test
    public void hibernateMigrateIgnoresMissingDocuments() throws Exception
    {
        this.mocker.registerMockComponent(ComponentManager.class);
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class);
        XWikiHibernateStore store = mock(XWikiHibernateStore.class);
        when(cm.getInstance(XWikiStoreInterface.class, "hibernate")).thenReturn(store);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext xc = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(xc);
        @SuppressWarnings("deprecation")
        ArgumentCaptor<HibernateCallback<Object>> callbackCaptor = new ArgumentCaptor<HibernateCallback<Object>>();
        this.mocker.getComponentUnderTest().migrate();
        Mockito.verify(store).executeWrite(Matchers.same(xc), callbackCaptor.capture());

        XWiki xwiki = mock(XWiki.class);
        when(xc.getWiki()).thenReturn(xwiki);
        HibernateCallback<Object> callback = callbackCaptor.getValue();
        Assert.assertNotNull(callback);
        Session session = mock(Session.class);
        Query q = mock(Query.class);
        when(session.createQuery(Matchers.anyString())).thenReturn(q);
        List<String> docs = new ArrayList<String>();
        docs.add("data.NotFound");
        when(q.list()).thenReturn(docs);
        callback.doInHibernate(session);
    }

    /** Non empty description. */
    @Test
    public void getDescription() throws Exception
    {
        Assert.assertTrue(StringUtils.isNotBlank(this.mocker.getComponentUnderTest().getDescription()));
    }

    /** Non empty name. */
    @Test
    public void getName() throws Exception
    {
        Assert.assertTrue(StringUtils.isNotBlank(this.mocker.getComponentUnderTest().getName()));
    }

    /** Correct version number. */
    @Test
    public void getVersion() throws Exception
    {
        Assert.assertEquals(54692, this.mocker.getComponentUnderTest().getVersion().getVersion());
    }

    /**
     * Always executes. When the initial database version is greater than this migrator's, the manager decides not to
     * run.
     */
    @Test
    public void shouldExecute() throws Exception
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().shouldExecute(new XWikiDBVersion(54689)));
        Assert.assertTrue(this.mocker.getComponentUnderTest().shouldExecute(new XWikiDBVersion(54693)));
    }
}
