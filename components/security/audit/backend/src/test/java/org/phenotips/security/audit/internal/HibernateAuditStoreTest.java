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
package org.phenotips.security.audit.internal;

import org.phenotips.security.audit.AuditEvent;
import org.phenotips.security.audit.AuditStore;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link HibernateAuditStore} component.
 *
 * @version $Id$
 */
public class HibernateAuditStoreTest
{
    @Rule
    public final MockitoComponentMockingRule<AuditStore> mocker =
        new MockitoComponentMockingRule<>(HibernateAuditStore.class);

    @Mock
    private User user;

    @Mock
    private Right right;

    private DocumentReference doc = new DocumentReference("wiki", "Space", "Page");

    private HibernateSessionFactory hsf;

    @Mock
    private SessionFactory sf;

    @Mock
    private Session session;

    @Mock
    private Transaction transaction;

    @Mock
    private Criteria criteria;

    private AuditStore store;

    @Mock
    private AuditEvent event;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.hsf = this.mocker.getInstance(HibernateSessionFactory.class);
        when(this.hsf.getSessionFactory()).thenReturn(this.sf);
        when(this.sf.openSession()).thenReturn(this.session);
        when(this.session.beginTransaction()).thenReturn(this.transaction);
        when(this.session.createCriteria(AuditEvent.class)).thenReturn(this.criteria);
        this.store = this.mocker.getComponentUnderTest();
        when(this.user.getId()).thenReturn("wiki:XWiki.user");
    }

    @Test
    public void storeSavesEventInSession()
    {
        this.store.store(this.event);
        Mockito.verify(this.session).save(this.event);
        Mockito.verify(this.session).close();
        Mockito.verify(this.transaction).commit();
    }

    @Test
    public void hibernateExceptionIsCaught()
    {
        when(this.session.save(this.event)).thenThrow(new HibernateException("failed"));
        this.store.store(this.event);
        Mockito.verify(this.session).close();
    }

    @Test
    public void sessionIsClosedOnException()
    {
        when(this.session.save(this.event)).thenThrow(new NullPointerException());
        try {
            this.store.store(this.event);
        } catch (NullPointerException ex) {
            // We're testing this exception, it's expected here
        }
        Mockito.verify(this.session).close();
    }

    @Test
    public void getEventsForEntity()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        this.store.getEventsForEntity(this.doc);
        Mockito.verify(this.criteria).add(criterion.capture());
        Example ex = (Example) criterion.getValue();
        Assert.assertEquals("example (null (null): null on wiki:Space.Page at null)", ex.toString());
    }

    @Test
    public void getEventsForEntityAndType()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        this.store.getEventsForEntity(this.doc, "action");
        Mockito.verify(this.criteria).add(criterion.capture());
        Example ex = (Example) criterion.getValue();
        Assert.assertEquals("example (null (null): action on wiki:Space.Page at null)", ex.toString());
    }

    @Test
    public void getEventsForUser()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        this.store.getEventsForUser(this.user);
        Mockito.verify(this.criteria).add(criterion.capture());
        Example ex = (Example) criterion.getValue();
        Assert.assertEquals("example (wiki:XWiki.user (null): null on null at null)", ex.toString());
    }

    @Test
    public void getEventsForUserAndIp()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        this.store.getEventsForUser(this.user, "127.0.0.1");
        Mockito.verify(this.criteria).add(criterion.capture());
        Example ex = (Example) criterion.getValue();
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): null on null at null)", ex.toString());
    }

    @Test
    public void getEventsForUserIpAndType()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        this.store.getEventsForUser(this.user, "127.0.0.1", "action");
        Mockito.verify(this.criteria).add(criterion.capture());
        Example ex = (Example) criterion.getValue();
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): action on null at null)", ex.toString());
    }
}
