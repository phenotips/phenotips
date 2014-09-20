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

import org.phenotips.storage.migrators.DataMigrationManager;
import org.phenotips.storage.migrators.DataTypeMigrator;

import org.xwiki.component.annotation.Component;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

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
    /**
     * All available migrators. Due to a limitation in the current component manager implementation, the generic type
     * cannot be used here. Update once http://jira.xwiki.org/browse/XCOMMONS-651 gets fixed.
     */
    @Inject
    private List<DataTypeMigrator> migrators;

    @Override
    public boolean migrate()
    {
        boolean result = true;
        for (DataTypeMigrator<?> migrator : this.migrators) {
            // Don't change the order, or the operation will be short-circuited before the call
            result = migrator.migrate() && result;
        }
        return result;
    }
}
