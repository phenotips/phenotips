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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Straight-forward implementation of the {@link SystemPasswordConfiguration} role. It reads the
 * {@code crypto.encryption.systemPassword.provider} setting from {@code WEB-INF/xwiki.properties} to choose a
 * {@link ConfigurationModule} which will provide the password.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Singleton
public class DefaultSystemPasswordConfiguration implements SystemPasswordConfiguration
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource config;

    @Inject
    private Map<String, SystemPasswordConfiguration.ConfigurationModule> modules;

    @Override
    public String getSystemPassword() throws IllegalStateException
    {
        String moduleName = this.config.getProperty("crypto.encryption.systemPassword.provider");
        if (StringUtils.isBlank(moduleName)) {
            throw new IllegalStateException(
                "Missing configuration: please specify a valid configuration module name under the"
                    + " \"crypto.encryption.systemPassword.provider\" key.");
        } else if (!this.modules.containsKey(moduleName)) {
            throw new IllegalStateException(
                "Invalid configuration: [" + moduleName + "] is not a valid configuration name. Supported modules: "
                    + StringUtils.join(this.modules.keySet().toArray()));
        }
        ConfigurationModule module = this.modules.get(moduleName);
        String password = module.getSystemPassword();
        if (StringUtils.isBlank(password)) {
            throw new IllegalStateException(
                "Invalid configuration: The module [" + moduleName + "] isn't configured properly.");
        }
        return password;
    }
}
