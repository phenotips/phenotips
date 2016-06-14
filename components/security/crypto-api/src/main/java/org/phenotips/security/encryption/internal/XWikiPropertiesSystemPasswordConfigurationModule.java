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
package org.phenotips.security.encryption.internal;

import org.phenotips.security.encryption.SystemPasswordConfiguration;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Retrieves the system encryption password from {@code WEB-INF/xwiki.properties} using the
 * {@code crypto.encryption.systemPassword} key.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("xwikiproperties")
@Singleton
public class XWikiPropertiesSystemPasswordConfigurationModule implements SystemPasswordConfiguration.ConfigurationModule
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource config;

    @Override
    public String getSystemPassword()
    {
        return this.config.getProperty("crypto.encryption.systemPassword");
    }
}
