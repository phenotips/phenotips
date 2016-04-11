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
package org.phenotips.storage.migrators.internal;

import org.phenotips.storage.migrators.DataMigrationManager;
import org.phenotips.storage.migrators.DataTypeMigrator;

import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

/**
 * Implementation for the {@link DataMigrationManager} role, which tries to invoke all available
 * {@link DataTypeMigrator}s.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Singleton
public class AutomaticDataMigrationManager implements DataMigrationManager
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /**
     * All available migrators. Due to a limitation in the current component manager implementation, the generic type
     * cannot be used here. Update once http://jira.xwiki.org/browse/XCOMMONS-651 gets fixed.
     */
    @Inject
    private List<DataTypeMigrator> migrators;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /** The current request context. */
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public boolean migrate()
    {
        XWikiContext context = this.contextProvider.get();
        String originalDatabase = context.getWikiId();
        boolean result = true;
        try {
            for (String db : this.wikiDescriptorManager.getAllIds()) {
                context.setWikiId(db);
                for (DataTypeMigrator<?> migrator : this.migrators) {
                    // Don't change the order, or the operation will be short-circuited before the call
                    result = migrator.migrate() && result;
                }
            }
        } catch (WikiManagerException ex) {
            this.logger.error("Failed to get the list of virtual wikis: {}", ex.getMessage(), ex);
            result = false;
        } finally {
            context.setWikiId(originalDatabase);
        }
        return result;
    }
}
