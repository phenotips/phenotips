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
package org.phenotips.studies.family.migrations;

import org.phenotips.studies.family.Family;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

/**
 * Migration for PhenoTips issue PT-3402: Search for documents with families that have XWikiRights objects and remove
 * the objects.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = { HibernateDataMigration.class })
@Named("71505-PT-3402")
@Singleton
public class R71505PhenoTips3402DataMigration extends AbstractHibernateDataMigration implements
    XWikiHibernateBaseStore.HibernateCallback<Object>
{
    /**
     * XWiki class that contains rights to XWiki documents.
     */
    private static final EntityReference RIGHTS_CLASS =
        new EntityReference("XWikiRights", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;

    /**
     * Serializes the rights name.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /**
     * Resolves unprefixed document names to the current wiki.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public String getDescription()
    {
        return "Search for documents with families that have XWikiRights objects and remove the objects.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71505);
    }

    /**
     * Searches for all family documents that contain a XWikiRights object, and removes the objects.
     */
    @Override
    public Object doInHibernate(final Session session) throws HibernateException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        Query q =
            session.createQuery(
                "select distinct doc.fullName from XWikiDocument as doc, BaseObject as f, BaseObject as r where "
                    + "f.name = doc.fullName and f.className = '" + this.serializer.serialize(Family.CLASS_REFERENCE)
                    + "' and r.name = doc.fullName and r.className = '" + this.serializer.serialize(RIGHTS_CLASS)
                    + "'");

        @SuppressWarnings("unchecked")
        List<String> documents = q.list();
        this.logger.debug("Found {} family documents", documents.size());
        for (String docName : documents) {
            try {
                R71505PhenoTips3402DataMigration.this.logger.debug("Checking [{}]", docName);
                XWikiDocument doc =
                    xwiki.getDocument(this.resolver.resolve(docName), context);
                if (doc == null) {
                    continue;
                }
                doc.removeXObjects(RIGHTS_CLASS);
                doc.setComment(this.getDescription());
                doc.setMinorEdit(true);
                // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                // so we must clear the session cache first.
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                session.flush();
                R71505PhenoTips3402DataMigration.this.logger.debug("Updated [{}]", docName);
            } catch (DataMigrationException e) {
                // We're in the middle of a migration, we're not expecting another migration
            }
        }
        return null;
    }
}
