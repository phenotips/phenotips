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
package org.phenotips.tools.jetty.listener;

import java.awt.Desktop;
import java.awt.HeadlessException;
import java.net.URI;
import java.util.ResourceBundle;

import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Jetty lifecycle listener that opens a browser when the server is started.
 *
 * @version $Id$
 * @since 1.1M1
 */
public class OpenBrowserListener extends AbstractLifeCycleListener
{
    /** Logging helper object. */
    private static final Logger LOGGER = Log.getLogger(OpenBrowserListener.class);

    /** Handles the message translations. */
    private static final ResourceBundle TRANSLATION =
        ResourceBundle.getBundle(OpenBrowserListener.class.getCanonicalName());

    @Override
    public void lifeCycleStarted(LifeCycle event)
    {
        boolean success = false;
        String serverUrl = "http://localhost:" + System.getProperty("jetty.port", "8080") + "/";

        LOGGER.info(TRANSLATION.getString("jetty.startup.notification"), serverUrl);

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(new URI(serverUrl));
                        success = true;
                    } catch (Exception e) {
                        LOGGER.warn(TRANSLATION.getString("jetty.browser.error"), e);
                    }
                } else {
                    LOGGER.warn(TRANSLATION.getString("jetty.action.error"));
                }
            } catch (HeadlessException e) {
                LOGGER.warn(TRANSLATION.getString("jetty.headless.error"));
            }
        } else {
            LOGGER.warn(TRANSLATION.getString("jetty.noAPI.error"));
        }

        if (success) {
            LOGGER.info(TRANSLATION.getString("jetty.success.notification"));
        }
    }
}
