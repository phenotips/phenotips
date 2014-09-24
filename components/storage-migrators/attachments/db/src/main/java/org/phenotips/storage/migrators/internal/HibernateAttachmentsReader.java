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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

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

    private static final String DATA_RETRIEVE_QUERY =
        "select d.fullName, a.filename from XWikiDocument d, XWikiAttachment a, XWikiAttachmentContent c"
            + " where a.docId = d.id and c.id = a.id";

    @Inject
    private Logger logger;

    @Inject
    @Named("hibernate")
    private XWikiStoreInterface docStore;

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
    private Provider<XWikiContext> context;

    @Override
    public Type getType()
    {
        return TYPE;
    }

    @Override
    public boolean hasData()
    {
        try {
            return !this.docStore.search(DATA_RETRIEVE_QUERY, 1, 0, this.context.get()).isEmpty();
        } catch (XWikiException ex) {
            this.logger.warn("Failed to search for attachments in the database: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public Iterator<EntityReference> listData()
    {
        try {
            List<Object[]> data = this.docStore.search(DATA_RETRIEVE_QUERY, 0, 0, this.context.get());
            return new ReferenceIterator(data);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to list the database attachments: {}", ex.getMessage());
            return Collections.emptyIterator();
        }
    }

    @Override
    public Iterator<XWikiAttachment> getData()
    {
        try {
            List<Object[]> data = this.docStore.search(DATA_RETRIEVE_QUERY, 0, 0, this.context.get());
            this.logger.debug("Found [{}] attachments in the database", data.size());
            return new AttachmentIterator(data);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to get the list of database attachments: {}", ex.getMessage());
            return Collections.emptyIterator();
        }
    }

    @Override
    public boolean discardEntity(XWikiAttachment entity)
    {
        boolean transaction = false;
        try {
            transaction = ((XWikiHibernateBaseStore) this.store).beginTransaction(this.context.get());
            Session session = ((XWikiHibernateBaseStore) this.store).getSession(this.context.get());
            session.delete(entity.getAttachment_content());
            session.delete(entity.getAttachment_archive());
            this.logger.debug("Deleted attachment [{}] from the database", entity.getReference());
        } catch (XWikiException ex) {
            this.logger.warn("Failed to cleanup attachment from the database: {}", ex.getMessage());
            return false;
        } finally {
            if (transaction) {
                ((XWikiHibernateBaseStore) this.store).endTransaction(this.context.get(), transaction);
            }
        }
        return true;
    }

    @Override
    public boolean discardAllData()
    {
        boolean transaction = false;
        try {
            transaction = ((XWikiHibernateBaseStore) this.store).beginTransaction(this.context.get());
            Session session = ((XWikiHibernateBaseStore) this.store).getSession(this.context.get());
            session.createQuery("delete from XWikiAttachmentContent").executeUpdate();
            session.createQuery("delete from XWikiAttachmentArchive").executeUpdate();
        } catch (XWikiException ex) {
            this.logger.warn("Failed to cleanup all attachments from the database: {}", ex.getMessage());
            return false;
        } finally {
            if (transaction) {
                ((XWikiHibernateBaseStore) this.store).endTransaction(this.context.get(), transaction);
            }
        }
        return true;
    }

    private class ReferenceIterator implements Iterator<EntityReference>
    {
        private Iterator<Object[]> data;

        ReferenceIterator(List<Object[]> data)
        {
            this.data = data.iterator();
        }

        @Override
        public boolean hasNext()
        {
            return this.data.hasNext();
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

        AttachmentIterator(List<Object[]> data)
        {
            this.data = data.iterator();
        }

        @Override
        public boolean hasNext()
        {
            return this.data.hasNext();
        }

        @Override
        public XWikiAttachment next()
        {
            Object[] item = this.data.next();
            try {
                XWikiDocument doc =
                    new XWikiDocument(HibernateAttachmentsReader.this.resolver.resolve(String.valueOf(item[0])));
                XWikiAttachment att = new XWikiAttachment(doc, String.valueOf(item[1]));
                HibernateAttachmentsReader.this.store.loadAttachmentContent(att,
                    HibernateAttachmentsReader.this.context.get(), true);
                HibernateAttachmentsReader.this.archiveStore.loadArchive(att,
                    HibernateAttachmentsReader.this.context.get(), true);
                HibernateAttachmentsReader.this.logger.debug("Loaded [{}] from the database", att.getReference());
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
