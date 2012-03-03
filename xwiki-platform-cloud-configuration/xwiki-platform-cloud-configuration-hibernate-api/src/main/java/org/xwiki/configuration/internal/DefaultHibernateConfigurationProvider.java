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
package org.xwiki.configuration.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.HibernateConfigurator;

/**
 * The default provider for Hibernate configurators.
 * 
 * @version $Id$
 */
@Named("hibernate")
public class DefaultHibernateConfigurationProvider implements Provider<HibernateConfigurator>, Initializable
{
    /**
     * The property for selecting the hibernate configurator.
     */
    private static final String HIBERNATE_CONFIGURATOR_PROPERTY = "hibernate.configurator";

    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    /**
     * The component manager for looking up the blob store.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The configuration source for reading XWiki properties.
     */
    @Inject
    @Named("cloud")
    private ConfigurationSource configurationSource;

    /**
     * The configured Hibernate configuration.
     */
    private HibernateConfigurator hibernateConfigurator;

    @Override
    public void initialize() throws InitializationException
    {
        String hibernateConfiguratorHint = configurationSource.getProperty(HIBERNATE_CONFIGURATOR_PROPERTY);
        if (hibernateConfiguratorHint == null) {
            throw new InitializationException(String.format(
                "You must specify the '%s' in your xwiki.properties file for selecting a hibernate configurator.",
                HIBERNATE_CONFIGURATOR_PROPERTY));
        }

        try {
            hibernateConfigurator = componentManager.lookup(HibernateConfigurator.class, hibernateConfiguratorHint);
        } catch (ComponentLookupException e) {
            String errorMessage =
                String.format("Unable to lookup a Hibernate configurator '%s'", hibernateConfiguratorHint);

            logger.error(errorMessage);

            throw new InitializationException(errorMessage, e);
        }

        logger.debug("Hibernate configurator provider initialized with configurator '{}'", hibernateConfiguratorHint);
    }

    @Override
    public HibernateConfigurator get()
    {
        return hibernateConfigurator;
    }
}
