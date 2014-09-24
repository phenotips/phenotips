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
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

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
    @Named("hibernate")
    private XWikiStoreInterface docStore;

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
        try {
            return !this.docStore.search(DATA_RETRIEVE_QUERY, 1, 0, this.context.get()).isEmpty();
        } catch (XWikiException ex) {
            this.logger.warn("Failed to search for deleted attachments in the database trash: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public Iterator<EntityReference> listData()
    {
        try {
            List<Object[]> data = this.docStore.search(DATA_REFERENCE_QUERY, 0, 0, this.context.get());
            return new ReferenceIterator(data);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to list the database deleted attachments: {}", ex.getMessage());
            return Collections.emptyIterator();
        }
    }

    @Override
    public Iterator<DeletedAttachment> getData()
    {
        try {
            List<Long> data = this.docStore.search(DATA_RETRIEVE_QUERY, 0, 0, this.context.get());
            this.logger.debug("Found [{}] deleted attachments in the database trash", data.size());
            return new DeletedAttachmentIterator(data);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to get the list of database deleted attachments: {}", ex.getMessage());
            return Collections.emptyIterator();
        }
    }

    @Override
    public boolean discardEntity(DeletedAttachment entity)
    {
        boolean transaction = false;
        try {
            transaction = ((XWikiHibernateBaseStore) this.store).beginTransaction(this.context.get());
            Session session = ((XWikiHibernateBaseStore) this.store).getSession(this.context.get());
            session.delete(entity);
            this.logger.debug("Deleted deleted attachment [{}@{}#{}] from the database trash",
                entity.getDocName(), entity.getFilename(), entity.getId());
        } catch (XWikiException ex) {
            this.logger.warn("Failed to cleanup attachment from the database trash: {}", ex.getMessage());
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
            session.createQuery("delete from DeletedAttachment").executeUpdate();
        } catch (XWikiException ex) {
            this.logger
                .warn("Failed to cleanup all attachments from the database trash: {}", ex.getMessage());
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

        DeletedAttachmentIterator(List<Long> data)
        {
            this.data = data.iterator();
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
                DeletedAttachment result = HibernateDeletedAttachmentsReader.this.store.getDeletedAttachment(
                    item, HibernateDeletedAttachmentsReader.this.context.get(), true);
                HibernateDeletedAttachmentsReader.this.logger.debug("Loaded [{}@{}#{}] from the database trash",
                    result.getDocName(), result.getFilename(), result.getId());
                return result;
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
