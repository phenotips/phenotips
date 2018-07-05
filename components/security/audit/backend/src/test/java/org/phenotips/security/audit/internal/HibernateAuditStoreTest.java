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

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.criterion.BetweenExpression;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.RowCountProjection;
import org.hibernate.criterion.SimpleExpression;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
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
    public void storeSkipsIgnoredActions()
    {
        for (String action : new String[] { "ssx", "jsx", "temp", "skin" }) {
            when(this.event.getAction()).thenReturn(action);
            this.store.store(this.event);
            Mockito.verifyZeroInteractions(this.session);
        }
    }

    @Test
    public void storeCatchesHibernateException()
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

    @Test
    public void getEvents()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        AuditEvent eventTemplate = new AuditEvent(this.user, "127.0.0.1", "action", null, null, null);
        when(this.criteria.list()).thenReturn(Collections.singletonList(this.event));
        List<AuditEvent> result = this.store.getEvents(eventTemplate, from, to, 0, 0);
        Assert.assertEquals(Collections.singletonList(this.event), result);
        Mockito.verify(this.criteria, Mockito.times(2)).add(criterion.capture());
        Example ex = (Example) criterion.getAllValues().get(0);
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): action on null at null)", ex.toString());
        BetweenExpression between = (BetweenExpression) criterion.getAllValues().get(1);
        Assert.assertEquals("time between " + from.toString() + " and " + to.toString(), between.toString());
    }

    @Test
    public void getEventsAcceptsNullTemplate()
    {
        this.store.getEvents(null, null, null, 0, 0);
        Mockito.verify(this.criteria, Mockito.never()).add(Matchers.any());
    }

    @Test
    public void getEventsWithStartAndCound()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        AuditEvent eventTemplate = new AuditEvent(this.user, "127.0.0.1", "action", null, null, null);
        this.store.getEvents(eventTemplate, from, to, 100, 25);
        Mockito.verify(this.criteria, Mockito.times(2)).add(criterion.capture());
        Example ex = (Example) criterion.getAllValues().get(0);
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): action on null at null)", ex.toString());
        BetweenExpression between = (BetweenExpression) criterion.getAllValues().get(1);
        Assert.assertEquals("time between " + from.toString() + " and " + to.toString(), between.toString());
        Mockito.verify(this.criteria).setFirstResult(100);
        Mockito.verify(this.criteria).setMaxResults(25);
        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        Mockito.verify(this.criteria).addOrder(order.capture());
        Assert.assertEquals("time desc", order.getValue().toString());
    }

    @Test
    public void getEventsIgnoresNegativeStart()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        AuditEvent eventTemplate = new AuditEvent(this.user, "127.0.0.1", "action", null, null, null);
        this.store.getEvents(eventTemplate, from, to, -100, 25);
        Mockito.verify(this.criteria, Mockito.times(2)).add(criterion.capture());
        Example ex = (Example) criterion.getAllValues().get(0);
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): action on null at null)", ex.toString());
        BetweenExpression between = (BetweenExpression) criterion.getAllValues().get(1);
        Assert.assertEquals("time between " + from.toString() + " and " + to.toString(), between.toString());
        Mockito.verify(this.criteria, Mockito.never()).setFirstResult(Matchers.anyInt());
        Mockito.verify(this.criteria).setMaxResults(25);
    }

    @Test
    public void getEventsIgnoresNegativeCount()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        AuditEvent eventTemplate = new AuditEvent(this.user, "127.0.0.1", "action", null, null, null);
        this.store.getEvents(eventTemplate, from, to, 0, -25);
        Mockito.verify(this.criteria, Mockito.times(2)).add(criterion.capture());
        Example ex = (Example) criterion.getAllValues().get(0);
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): action on null at null)", ex.toString());
        BetweenExpression between = (BetweenExpression) criterion.getAllValues().get(1);
        Assert.assertEquals("time between " + from.toString() + " and " + to.toString(), between.toString());
        Mockito.verify(this.criteria, Mockito.never()).setMaxResults(Matchers.anyInt());
    }

    @Test
    public void getEventsCatchesHibernateException()
    {
        when(this.criteria.list()).thenThrow(new HibernateException(""));
        Assert.assertTrue(this.store.getEvents(null, null, null, 0, 0).isEmpty());
    }

    @Test
    public void countEventsForEntity()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        long count = this.store.countEventsForEntity(this.doc);
        Mockito.verify(this.criteria).setProjection(Matchers.any(RowCountProjection.class));
        Mockito.verify(this.criteria).add(criterion.capture());
        Example ex = (Example) criterion.getValue();
        Assert.assertEquals("example (null (null): null on wiki:Space.Page at null)", ex.toString());
        Assert.assertEquals(0, count);
    }

    @Test
    public void countEventsForEntityAndType()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        long count = this.store.countEventsForEntity(this.doc, "action");
        Mockito.verify(this.criteria).setProjection(Matchers.any(RowCountProjection.class));
        Mockito.verify(this.criteria).add(criterion.capture());
        Example ex = (Example) criterion.getValue();
        Assert.assertEquals("example (null (null): action on wiki:Space.Page at null)", ex.toString());
        Assert.assertEquals(0, count);
    }

    @Test
    public void countEventsForUser()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        long count = this.store.countEventsForUser(this.user);
        Mockito.verify(this.criteria).setProjection(Matchers.any(RowCountProjection.class));
        Mockito.verify(this.criteria).add(criterion.capture());
        Example ex = (Example) criterion.getValue();
        Assert.assertEquals("example (wiki:XWiki.user (null): null on null at null)", ex.toString());
        Assert.assertEquals(0, count);
    }

    @Test
    public void countEventsForUserAndIp()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        long count = this.store.countEventsForUser(this.user, "127.0.0.1");
        Mockito.verify(this.criteria).setProjection(Matchers.any(RowCountProjection.class));
        Mockito.verify(this.criteria).add(criterion.capture());
        Example ex = (Example) criterion.getValue();
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): null on null at null)", ex.toString());
        Assert.assertEquals(0, count);
    }

    @Test
    public void countEventsForUserIpType()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        long count = this.store.countEventsForUser(this.user, "127.0.0.1", "action");
        Mockito.verify(this.criteria).setProjection(Matchers.any(RowCountProjection.class));
        Mockito.verify(this.criteria).add(criterion.capture());
        Example ex = (Example) criterion.getValue();
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): action on null at null)", ex.toString());
        Assert.assertEquals(0, count);
    }

    @Test
    public void countEventsWithNoTimeRange()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        AuditEvent eventTemplate = new AuditEvent(this.user, "127.0.0.1", "action", null, null, null);
        long count = this.store.countEvents(eventTemplate, null, null);
        Mockito.verify(this.criteria).setProjection(Matchers.any(RowCountProjection.class));
        Mockito.verify(this.criteria, Mockito.times(1)).add(criterion.capture());
        Assert.assertEquals(0, count);
        Example ex = (Example) criterion.getAllValues().get(0);
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): action on null at null)", ex.toString());
    }

    @Test
    public void countEventsWithLowerTimeLimit()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        Calendar from = Calendar.getInstance();
        AuditEvent eventTemplate = new AuditEvent(this.user, "127.0.0.1", "action", null, null, null);
        long count = this.store.countEvents(eventTemplate, from, null);
        Mockito.verify(this.criteria).setProjection(Matchers.any(RowCountProjection.class));
        Mockito.verify(this.criteria, Mockito.times(2)).add(criterion.capture());
        Assert.assertEquals(0, count);
        Example ex = (Example) criterion.getAllValues().get(0);
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): action on null at null)", ex.toString());
        SimpleExpression timeFilter = (SimpleExpression) criterion.getAllValues().get(1);
        Assert.assertEquals("time>=" + from.toString(), timeFilter.toString());
    }

    @Test
    public void countEventsWithUpperTimeLimit()
    {
        when(this.criteria.list()).thenReturn(Collections.singletonList(42L));
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        Calendar to = Calendar.getInstance();
        AuditEvent eventTemplate = new AuditEvent(this.user, "127.0.0.1", "action", null, null, null);
        long count = this.store.countEvents(eventTemplate, null, to);
        Mockito.verify(this.criteria).setProjection(Matchers.any(RowCountProjection.class));
        Mockito.verify(this.criteria, Mockito.times(2)).add(criterion.capture());
        Assert.assertEquals(42, count);
        Example ex = (Example) criterion.getAllValues().get(0);
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): action on null at null)", ex.toString());
        SimpleExpression timeFilter = (SimpleExpression) criterion.getAllValues().get(1);
        Assert.assertEquals("time<=" + to.toString(), timeFilter.toString());
    }

    @Test
    public void countEventsWithTimeRange()
    {
        ArgumentCaptor<Criterion> criterion = ArgumentCaptor.forClass(Criterion.class);
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        AuditEvent eventTemplate = new AuditEvent(this.user, "127.0.0.1", "action", null, null, null);
        long count = this.store.countEvents(eventTemplate, from, to);
        Mockito.verify(this.criteria).setProjection(Matchers.any(RowCountProjection.class));
        Mockito.verify(this.criteria, Mockito.times(2)).add(criterion.capture());
        Assert.assertEquals(0, count);
        Example ex = (Example) criterion.getAllValues().get(0);
        Assert.assertEquals("example (wiki:XWiki.user (127.0.0.1): action on null at null)", ex.toString());
        BetweenExpression timeFilter = (BetweenExpression) criterion.getAllValues().get(1);
        Assert.assertEquals("time between " + from.toString() + " and " + to.toString(), timeFilter.toString());
    }

    @Test
    public void countEventsAcceptsNullTemplate()
    {
        long count = this.store.countEvents(null, null, null);
        Mockito.verify(this.criteria).setProjection(Matchers.any(RowCountProjection.class));
        Mockito.verify(this.criteria, Mockito.never()).add(Matchers.any());
        Assert.assertEquals(0, count);
    }

    @Test
    public void countEventsCatchesHibernateException()
    {
        when(this.criteria.list()).thenThrow(new HibernateException(""));
        Assert.assertEquals(-1, this.store.countEvents(null, null, null));
    }
}
