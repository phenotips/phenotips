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
package org.phenotips.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Registers the proxy credentials, provided via system properties, into the default {@code java.net} connections.
 *
 * @version $Id$
 * @since 1.0.1/1.1M1
 */
@Component
@Named("proxy-authentication")
@Singleton
public class ProxyAuthenticator extends AbstractEventListener
{
    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public ProxyAuthenticator()
    {
        super("proxy-authentication", new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        final String proxyUser = System.getProperty("http.proxyUser");
        final String proxyPassword = System.getProperty("http.proxyPassword");
        if (StringUtils.isNoneBlank(proxyUser, proxyPassword)) {
            Authenticator.setDefault(
                new Authenticator()
                {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication()
                    {
                        return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                    }
                }
            );
        }
    }
}
