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
package org.phenotips.tools.jetty.listener;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Jetty lifecycle listener that prints a message to open a browser when the server is started. This is to provide
 * information to newbies so that they know what to do after the server is started.
 *
 * @version $Id$
 * @since 1.0M13
 */
public class NotifyListener extends AbstractLifeCycleListener
{
    /** Logging helper object. */
    private static final Logger LOGGER = Log.getLogger(NotifyListener.class);

    /** Handles the message translations. */
    private static final ResourceBundle TRANSLATION = ResourceBundle.getBundle(NotifyListener.class.getCanonicalName());

    @Override
    public void lifeCycleStarted(LifeCycle event)
    {
        try {
            String port = System.getProperty("jetty.port", "8080");
            String serverUrl = "http://" + Inet4Address.getLocalHost().getCanonicalHostName() + ":" + port + "/";
            LOGGER.info(TRANSLATION.getString("jetty.startup.notification"), serverUrl);
        } catch (UnknownHostException ex) {
            // Shouldn't happen, localhost should be available
            LOGGER.ignore(ex);
        }
        LOGGER.info("----------------------------------");
    }

    @Override
    public void lifeCycleStopping(LifeCycle event)
    {
        LOGGER.info(TRANSLATION.getString("jetty.stopping.notification"));
    }

    @Override
    public void lifeCycleStopped(LifeCycle event)
    {
        LOGGER.info(TRANSLATION.getString("jetty.stopped.notification"));
    }
}
