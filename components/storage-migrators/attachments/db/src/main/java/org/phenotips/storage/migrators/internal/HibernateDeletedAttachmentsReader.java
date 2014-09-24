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
package org.phenotips.storage.migrators.internal;

import org.phenotips.storage.migrators.DataReader;
import org.phenotips.storage.migrators.Type;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * {@link DataReader} that can read {@link DeletedAttachment deleted attachments} from a Hibernate-managed database (the
 * default storage engine of XWiki).
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("deleted attachments/hibernate")
@Singleton
public class HibernateDeletedAttachmentsReader implements DataReader<DeletedAttachment>
{
    private static final Type TYPE = new Type("deleted attachments", "hibernate");

    private static final String DATA_RETRIEVE_QUERY = "select a.id from DeletedAttachment a";

    private static final String DATA_REFERENCE_QUERY = "select a.docName, a.filename from DeletedAttachment a";

    @Inject
    private Logger logger;

    @Inject
    private HibernateSessionFactory hibernate;

    @Inject
    @Named("hibernate")
    private AttachmentRecycleBinStore store;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Provider<XWikiContext> context;

    @Override
    public Type getType()
    {
        return TYPE;
    }

    @Override
    public boolean hasData()
    {
        Session session = null;
        try {
            session = this.hibernate.getSessionFactory().openSession();
            return !session.createQuery(DATA_RETRIEVE_QUERY).setMaxResults(1).list().isEmpty();
        } finally {
            session.close();
        }
    }

    @Override
    public Iterator<EntityReference> listData()
    {
        Session session = null;
        try {
            session = this.hibernate.getSessionFactory().openSession();
            @SuppressWarnings("unchecked")
            Iterator<Object[]> data = session.createQuery(DATA_REFERENCE_QUERY).iterate();
            return new ReferenceIterator(data);
        } finally {
            session.close();
        }
    }

    @Override
    public Iterator<DeletedAttachment> getData()
    {
        Session session = null;
        try {
            session = this.hibernate.getSessionFactory().openSession();
            @SuppressWarnings("unchecked")
            Iterator<Long> data = session.createQuery(DATA_RETRIEVE_QUERY).iterate();
            return new DeletedAttachmentIterator(data);
        } finally {
            session.close();
        }
    }

    @Override
    public boolean discardEntity(DeletedAttachment entity)
    {
        Session session = null;
        try {
            session = this.hibernate.getSessionFactory().openSession();
            Transaction t = session.beginTransaction();
            session.delete(entity);
            t.commit();
        } finally {
            session.close();
        }
        return true;
    }

    @Override
    public boolean discardAllData()
    {
        Session session = null;
        try {
            session = this.hibernate.getSessionFactory().openSession();
            Transaction t = session.beginTransaction();
            session.createQuery("delete from DeletedAttachment").executeUpdate();
            t.commit();
        } finally {
            session.close();
        }
        return true;
    }

    private class ReferenceIterator implements Iterator<EntityReference>
    {
        private Iterator<Object[]> data;

        ReferenceIterator(Iterator<Object[]> data)
        {
            List<Object[]> copy = new ArrayList<>();
            while (data.hasNext()) {
                copy.add(data.next());
            }
            this.data = copy.iterator();
        }

        @Override
        public boolean hasNext()
        {
            boolean result = this.data.hasNext();
            return result;
        }

        @Override
        public EntityReference next()
        {
            Object[] item = this.data.next();
            return new AttachmentReference(String.valueOf(item[1]),
                HibernateDeletedAttachmentsReader.this.resolver.resolve(String.valueOf(item[0])));
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    private class DeletedAttachmentIterator implements Iterator<DeletedAttachment>
    {
        private Iterator<Long> data;

        DeletedAttachmentIterator(Iterator<Long> data)
        {
            List<Long> copy = new ArrayList<>();
            while (data.hasNext()) {
                copy.add(data.next());
            }
            this.data = copy.iterator();
        }

        @Override
        public boolean hasNext()
        {
            boolean result = this.data.hasNext();
            return result;
        }

        @Override
        public DeletedAttachment next()
        {
            Long item = this.data.next();
            try {
                return HibernateDeletedAttachmentsReader.this.store.getDeletedAttachment(
                    item, HibernateDeletedAttachmentsReader.this.context.get(), true);
            } catch (Exception ex) {
                HibernateDeletedAttachmentsReader.this.logger.error(
                    "Failed to read deleted attachment from the database store: {}",
                    ex.getMessage(), ex);
            }
            return null;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
