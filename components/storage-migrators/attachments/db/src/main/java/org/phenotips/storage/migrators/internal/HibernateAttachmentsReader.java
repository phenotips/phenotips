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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * {@link DataReader} that can read {@link XWikiAttachment attachment} contents and history from a Hibernate-managed
 * database (the default storage engine of XWiki).
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("attachments/hibernate")
@Singleton
public class HibernateAttachmentsReader implements DataReader<XWikiAttachment>
{
    private static final Type TYPE = new Type("attachments", "hibernate");

    private static final String SESSION_KEY = "hibsession";

    private static final String DATA_RETRIEVE_QUERY =
        "select d.fullName, a.filename from XWikiDocument d, XWikiAttachment a, XWikiAttachmentContent c"
            + " where a.docId = d.id and c.id = a.id";

    @Inject
    private Logger logger;

    @Inject
    private HibernateSessionFactory hibernate;

    @Inject
    @Named("hibernate")
    private XWikiAttachmentStoreInterface store;

    @Inject
    @Named("hibernate")
    private AttachmentVersioningStore archiveStore;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private DocumentAccessBridge dab;

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
            Criteria c = session.createCriteria(XWikiAttachmentContent.class);
            c.setMaxResults(1);
            return !c.list().isEmpty();
        } finally {
            session.close();
        }
    }

    @Override
    public Iterator<EntityReference> listData()
    {
        // FIXME The iterator requires an open connection to the database, so the Hibernate session must be left open;
        // however, the transaction must be closed at some point for discarded items to be actually deleted
        // FIXME Revisit how the session/transaction is managed
        Session session = getSession();
        @SuppressWarnings("unchecked")
        Iterator<Object[]> data = session.createQuery(DATA_RETRIEVE_QUERY).iterate();
        return new ReferenceIterator(data);
    }

    @Override
    public Iterator<XWikiAttachment> getData()
    {
        Session session = getSession();
        @SuppressWarnings("unchecked")
        Iterator<Object[]> data = session.createQuery(DATA_RETRIEVE_QUERY).iterate();
        return new AttachmentIterator(data);
    }

    @Override
    public boolean discardEntity(XWikiAttachment entity)
    {
        Session session = getSession();
        session.delete(entity.getAttachment_content());
        session.delete(entity.getAttachment_archive());
        // FIXME The transaction needs to be committed... The last item discarded might be left in a dangling session
        return true;
    }

    @Override
    public boolean discardAllData()
    {
        Session session = getSession();
        session.createQuery("delete from XWikiAttachmentContent").executeUpdate();
        session.createQuery("delete from XWikiAttachmentArchive").executeUpdate();
        // FIXME The transaction needs to be committed...
        return true;
    }

    private Session getSession()
    {
        Session session = (Session) this.context.get().get(SESSION_KEY);
        if (session == null) {
            try {
                ((XWikiHibernateBaseStore) this.store).beginTransaction(this.context.get());
                session = (Session) this.context.get().get(SESSION_KEY);
            } catch (XWikiException ex) {
                this.logger.error("Failed to start a new Hibernate session: {}", ex.getMessage(), ex);
            }
        }
        return session;
    }

    private void closeSession()
    {
        Session session = (Session) this.context.get().get(SESSION_KEY);
        if (session != null) {
            ((XWikiHibernateBaseStore) this.store).endTransaction(this.context.get(), true);
        }
    }

    private class ReferenceIterator implements Iterator<EntityReference>
    {
        private Iterator<Object[]> data;

        ReferenceIterator(Iterator<Object[]> data)
        {
            this.data = data;
        }

        @Override
        public boolean hasNext()
        {
            boolean result = this.data.hasNext();
            if (!result) {
                closeSession();
            }
            return result;
        }

        @Override
        public EntityReference next()
        {
            Object[] item = this.data.next();
            return new AttachmentReference(String.valueOf(item[1]),
                HibernateAttachmentsReader.this.resolver.resolve(String.valueOf(item[0])));
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    private class AttachmentIterator implements Iterator<XWikiAttachment>
    {
        private Iterator<Object[]> data;

        AttachmentIterator(Iterator<Object[]> data)
        {
            this.data = data;
        }

        @Override
        public boolean hasNext()
        {
            boolean result = this.data.hasNext();
            if (!result) {
                closeSession();
            }
            return result;
        }

        @Override
        public XWikiAttachment next()
        {
            Object[] item = this.data.next();
            try {
                XWikiDocument doc = (XWikiDocument) HibernateAttachmentsReader.this.dab.getDocument(
                    HibernateAttachmentsReader.this.resolver.resolve(String.valueOf(item[0])));
                XWikiAttachment att = new XWikiAttachment(doc, String.valueOf(item[1]));
                HibernateAttachmentsReader.this.store.loadAttachmentContent(att,
                    HibernateAttachmentsReader.this.context.get(), false);
                HibernateAttachmentsReader.this.archiveStore.loadArchive(att,
                    HibernateAttachmentsReader.this.context.get(), false);
                return att;
            } catch (Exception ex) {
                HibernateAttachmentsReader.this.logger.error("Failed to read attachment from the database store: {}",
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
