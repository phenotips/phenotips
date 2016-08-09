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
package org.phenotips.data.templates.internal;

import org.phenotips.Constants;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

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
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

/**
 * Migration for PhenoTips issue PT-2692: After upgrade from 1.3 to 1.4, migrate existing old
 * {@code PhenoTips.StudyBindingClass} to the new {@code PhenoTips.TemplateBindingClass}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = { HibernateDataMigration.class })
@Named("R71495PhenoTips#2692")
@Singleton
public class R71495PhenoTips2692DataMigration extends AbstractHibernateDataMigration
    implements HibernateCallback<Object>
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Override
    public String getDescription()
    {
        return "Migrate existing old patient data from the 'StudyBindingClass' to the new 'TemplateBindingClass'";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71495);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        DocumentReference oldClassReference =
            new DocumentReference(context.getWikiId(), Constants.CODE_SPACE, "StudyBindingClass");

        DocumentReference newClassReference =
            new DocumentReference(context.getWikiId(), Constants.CODE_SPACE, "TemplateBindingClass");

        Query q = session.createQuery("select distinct o.name from BaseObject as o "
            + "where o.className='PhenoTips.StudyBindingClass'");
        @SuppressWarnings("unchecked")
        List<String> documents = q.list();
        this.logger.debug("Found {} documents with StudyBindingClass", documents.size());
        for (String docName : documents) {
            XWikiDocument doc =
                xwiki.getDocument(R71495PhenoTips2692DataMigration.this.resolver.resolve(docName), context);
            for (BaseObject oldObject : doc.getXObjects(oldClassReference)) {
                BaseObject newObject = oldObject.duplicate();
                newObject.setXClassReference(newClassReference);
                doc.addXObject(newObject);
            }
            doc.removeXObjects(oldClassReference);
            doc.setComment("Migrated patient data to PhenoTips.TemplateBindingClass");
            doc.setMinorEdit(true);
            try {
                // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                // so we must clear the session cache first.
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                session.flush();
            } catch (DataMigrationException e) {
                // We're in the middle of a migration, we're not expecting another migration
            }
        }
        return null;
    }
}
