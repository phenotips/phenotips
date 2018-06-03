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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
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
    /** Handles persistence. */
    @Inject
    private HibernateSessionFactory sessionFactory;

    @Inject
    private Logger logger;

    @Override
    public void store(AuditEvent event)
    {
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
        Session session = this.sessionFactory.getSessionFactory().openSession();
        Criteria c = session.createCriteria(AuditEvent.class);
        AuditEvent sample = new AuditEvent(user, ip, type, null, entity, null);
        c.add(Example.create(sample));
        c.addOrder(Order.desc("time"));
        @SuppressWarnings("unchecked")
        List<AuditEvent> foundEntries = c.list();
        return foundEntries;
    }
}
