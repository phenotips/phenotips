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
