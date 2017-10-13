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

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.ManageRight;
import org.xwiki.security.authorization.Right;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * Register a new right that can be set for documents, the right to Manage that document.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("manage-right-registration")
@Singleton
public class ManageRightRegistrationEventListener extends AbstractEventListener
{
    @Inject
    private Logger logger;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public ManageRightRegistrationEventListener()
    {
        super("manage-right-registration", new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.logger.debug("Registered \"manage\" right: {} = {}", ManageRight.MANAGE, Right.toRight("manage"));
    }
}
