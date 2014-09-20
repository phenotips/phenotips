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
