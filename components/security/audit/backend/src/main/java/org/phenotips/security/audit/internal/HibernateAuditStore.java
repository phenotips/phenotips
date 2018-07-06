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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.users.User;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * Implementation of {@link AuditStore} using Hibernate for persistence.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class HibernateAuditStore implements AuditStore
{
    private static final String TIME_FIELD_NAME = "time";

    private static final List<String> ACTION_IGNORED = Arrays.asList("tex", "temp", "dot", "svg", "skin", "jsx", "ssx",
        "charting", "lock", "imagecaptcha", "unknown");

    /** Handles persistence. */
    @Inject
    private HibernateSessionFactory sessionFactory;

    @Inject
    private Logger logger;

    @Override
    public void store(AuditEvent event)
    {
        if (ACTION_IGNORED.contains(event.getAction())) {
            return;
        }
        Session session = this.sessionFactory.getSessionFactory().openSession();
        try {
            Transaction t = session.beginTransaction();
            t.begin();
            session.save(event);
            t.commit();
        } catch (HibernateException ex) {
            this.logger.error("Failed to save audit event [{}]: {}", event, ex.getMessage(), ex);
        } finally {
            session.close();
        }
    }

    @Override
    public List<AuditEvent> getEventsForEntity(DocumentReference entity)
    {
        return getEvents(null, null, null, entity);
    }

    @Override
    public List<AuditEvent> getEventsForEntity(DocumentReference entity, String type)
    {
        return getEvents(null, null, type, entity);
    }

    @Override
    public List<AuditEvent> getEventsForUser(User user)
    {
        return getEvents(user, null, null, null);
    }

    @Override
    public List<AuditEvent> getEventsForUser(User user, String ip)
    {
        return getEvents(user, ip, null, null);
    }

    @Override
    public List<AuditEvent> getEventsForUser(User user, String ip, String type)
    {
        return getEvents(user, ip, type, null);
    }

    private List<AuditEvent> getEvents(User user, String ip, String type, DocumentReference entity)
    {
        return getEvents(new AuditEvent(user, ip, type, null, entity, null), null, null, 0, 0);
    }

    @Override
    public List<AuditEvent> getEvents(AuditEvent eventTemplate, Calendar fromTime, Calendar toTime, int start,
        int maxResults)
    {
        try {
            Session session = this.sessionFactory.getSessionFactory().openSession();
            Criteria c = session.createCriteria(AuditEvent.class);

            if (eventTemplate != null) {
                c.add(Example.create(eventTemplate));
            }

            setTimeInterval(c, fromTime, toTime);
            c.addOrder(Order.desc(TIME_FIELD_NAME));
            if (start > 0) {
                c.setFirstResult(start);
            }
            if (maxResults > 0) {
                c.setMaxResults(maxResults);
            }
            c.setReadOnly(true);

            @SuppressWarnings("unchecked")
            List<AuditEvent> foundEntries = c.list();
            return foundEntries;
        } catch (HibernateException ex) {
            this.logger.error("Failed to load audit event documents: {}", ex.getMessage(), ex);
        }
        return Collections.emptyList();
    }

    @Override
    public long countEventsForEntity(DocumentReference entity)
    {
        return getCount(null, null, null, entity);
    }

    @Override
    public long countEventsForEntity(DocumentReference entity, String type)
    {
        return getCount(null, null, type, entity);
    }

    @Override
    public long countEventsForUser(User user)
    {
        return getCount(user, null, null, null);
    }

    @Override
    public long countEventsForUser(User user, String ip)
    {
        return getCount(user, ip, null, null);
    }

    @Override
    public long countEventsForUser(User user, String ip, String type)
    {
        return getCount(user, ip, type, null);
    }

    @Override
    public long countEvents(AuditEvent eventTemplate, Calendar fromTime, Calendar toTime)
    {
        try {
            Session session = this.sessionFactory.getSessionFactory().openSession();
            Criteria c = session.createCriteria(AuditEvent.class);

            if (eventTemplate != null) {
                c.add(Example.create(eventTemplate));
            }

            setTimeInterval(c, fromTime, toTime);
            c.setProjection(Projections.rowCount());

            @SuppressWarnings("rawtypes")
            List foundEntries = c.list();
            if (foundEntries != null && !foundEntries.isEmpty()) {
                return (long) foundEntries.get(0);
            }

            return 0;
        } catch (HibernateException ex) {
            this.logger.error("Failed to count audit event documents: {}", ex.getMessage(), ex);
            return -1;
        }
    }

    private long getCount(User user, String ip, String type, DocumentReference entity)
    {
        return countEvents(new AuditEvent(user, ip, type, null, entity, null), null, null);
    }

    private void setTimeInterval(Criteria c, Calendar fromTime, Calendar toTime)
    {
        if (fromTime != null && toTime != null) {
            c.add(Restrictions.between(TIME_FIELD_NAME, fromTime, toTime));
        } else if (fromTime != null) {
            c.add(Restrictions.ge(TIME_FIELD_NAME, fromTime));
        } else if (toTime != null) {
            c.add(Restrictions.le(TIME_FIELD_NAME, toTime));
        }
    }
}
