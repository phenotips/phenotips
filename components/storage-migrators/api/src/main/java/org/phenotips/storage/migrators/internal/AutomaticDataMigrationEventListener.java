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

import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Automatically performs data migration when PhenoTips starts.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("automatic-data-migration")
@Singleton
public class AutomaticDataMigrationEventListener extends AbstractEventListener
{
    @Inject
    private DataMigrationManager migrationManager;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public AutomaticDataMigrationEventListener()
    {
        super("automatic-data-migration", new ApplicationReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.migrationManager.migrate();
    }
}
