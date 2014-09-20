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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.internal.CommonsConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.stability.Unstable;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;

/**
 * Exposes the configuration used by the old XWiki core through the modern
 * {@link org.xwiki.configuration.ConfigurationSource} API.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Unstable("This class belongs in XWiki, or at least in another module, not in the storage migrators module.")
@Component
@Named("legacy")
@Singleton
public class LegacyXWikiConfigurationSource extends CommonsConfigurationSource implements Initializable
{
    /** The location of the configuration file, relative to the webapp root directory. */
    private static final String XWIKI_CFG_FILE = "/WEB-INF/xwiki.cfg";

    /** Provides access to the configuration file. */
    @Inject
    private Environment environment;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        URL configurationFileUrl = null;
        try {
            configurationFileUrl = this.environment.getResource(XWIKI_CFG_FILE);
            if (configurationFileUrl != null) {
                setConfiguration(new PropertiesConfiguration(configurationFileUrl));
            } else {
                // We use a debug logging level here since we consider it's ok that there's no XWIKI_CFG_FILE available,
                // in which case default values are used.
                this.logger.debug("No configuration file [{}] found. Using default configuration values.",
                    XWIKI_CFG_FILE);
            }
        } catch (Exception ex) {
            // Note: if we cannot read the configuration file for any reason we log a warning but continue since XWiki
            // will use default values for all configurable elements.
            this.logger.warn("Failed to load configuration file [{}]. Using default configuration values. "
                + "Internal error [{}]", XWIKI_CFG_FILE, ex.getMessage());
        }

        // If no Commons Properties Configuration has been set, use a default empty Commons Configuration implementation
        if (configurationFileUrl == null) {
            setConfiguration(new BaseConfiguration());
        }
    }
}
