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

package org.phenotips.data.indexing.internal;

import org.phenotips.data.indexing.PatientIndexer;

import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue PT-4004: trigger reindexing of all patients after the
 * way genes are stored in the SOLR patient index has changed (ENS IDs instead of gene names).
 *
 * @version $Id$
 * @since 1.4.7
 */
@Component
@Named("R74695-PT-4004")
@Singleton
public class R74695PhenoTips4004DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    @Inject
    private PatientIndexer indexer;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Trigger re-indexing for all patients.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(74695);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public Object doInHibernate(final Session session) throws HibernateException, XWikiException
    {
        try {
            this.indexer.reindex();
        } catch (final Exception e) {
            this.logger.error("Error while reindexing patients: [{}]", e);
        }
        return null;
    }
}
