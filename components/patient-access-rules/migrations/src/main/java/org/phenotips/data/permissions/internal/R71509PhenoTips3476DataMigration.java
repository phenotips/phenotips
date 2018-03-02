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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.Visibility;
import org.phenotips.studies.family.Family;

import org.xwiki.model.reference.DocumentReferenceResolver;
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
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #PT-3476: Add owner, collaborators and visibility to family directory filters. This is
 * better implemented if all families have a default visibility object private which until now they do not.
 *
 * @version $Id$
 * @since 1.4
 */
@Named("R71509-PT-3476")
@Singleton
public class R71509PhenoTips3476DataMigration extends AbstractHibernateDataMigration
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /**
     * Serializes the rights name.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Override
    public String getDescription()
    {
        return "Adding a default 'private' visibility object to all families which did not have a "
            + "visibility object #PT-3476";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71509);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new AddVisObjectCallback());
    }

    /**
     * Searches for all family documents that don't contain a {@code PhenoTips.VisibilityClass} object and adds a new
     * {@code PhenoTips.VisibilityClass} object with "private" set as the visibility.
     */
    private final class AddVisObjectCallback implements XWikiHibernateBaseStore.HibernateCallback<Object>
    {
        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {

            XWikiContext context = R71509PhenoTips3476DataMigration.this.getXWikiContext();
            XWiki xwiki = context.getWiki();
            Query q =
                session.createQuery("select doc.fullName from XWikiDocument as doc, BaseObject "
                    + "as f where doc.fullName=f.name and f.className='"
                    + R71509PhenoTips3476DataMigration.this.serializer.serialize(Family.CLASS_REFERENCE)
                    + "' and not exists (from BaseObject visobj where visobj.className='"
                    + R71509PhenoTips3476DataMigration.this.serializer.serialize(Visibility.CLASS_REFERENCE)
                    + "' and  doc.fullName=visobj.name)");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            R71509PhenoTips3476DataMigration.this.logger.debug("Found {} family documents with no visibility object",
                documents.size());
            for (String docName : documents) {
                try {
                    R71509PhenoTips3476DataMigration.this.logger.debug("Checking [{}]", docName);
                    XWikiDocument doc =
                        xwiki.getDocument(R71509PhenoTips3476DataMigration.this.resolver.resolve(docName), context);
                    if (doc == null) {
                        continue;
                    }
                    BaseObject visobj = doc.newXObject(Visibility.CLASS_REFERENCE, context);
                    visobj.set("visibility", "private", context);

                    doc.setComment(R71509PhenoTips3476DataMigration.this.getDescription());
                    doc.setMinorEdit(true);

                    // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                    // so we must clear the session cache first.
                    session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                    session.flush();
                    R71509PhenoTips3476DataMigration.this.logger.debug("Updated [{}]", docName);
                } catch (DataMigrationException e) {
                    // We're in the middle of a migration, we're not expecting another migration
                }
            }
            return null;
        }
    }
}
