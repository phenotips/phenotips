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
package org.phenotips.entities.configuration.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityResolver;
import org.phenotips.entities.configuration.PrimaryEntityConfiguration;
import org.phenotips.entities.configuration.PrimaryEntityConfigurationManager;
import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationBuilder;
import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationModule;

import org.xwiki.component.annotation.Component;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * Default implementation for the {@link PrimaryEntityConfigurationManager} component.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Component
@Singleton
public class DefaultPrimaryEntityConfigurationManager implements PrimaryEntityConfigurationManager
{
    /** Logging helper. */
    @Inject
    private Logger logger;

    @Inject
    private Provider<List<PrimaryEntityConfigurationModule>> modules;

    @Inject
    private PrimaryEntityResolver resolver;

    @Override
    public PrimaryEntityConfiguration getConfiguration(String recordType)
    {
        if (recordType == null) {
            throw new IllegalArgumentException();
        }
        return getConfiguration(recordType, null);
    }

    @Override
    public PrimaryEntityConfiguration getConfiguration(PrimaryEntity entity)
    {
        if (entity == null) {
            throw new IllegalArgumentException();
        }
        return getConfiguration(entity.getTypeId(), entity);
    }

    private PrimaryEntityConfiguration getConfiguration(@Nonnull String recordType, @Nullable PrimaryEntity entity)
    {
        PrimaryEntityConfigurationBuilder config =
            DefaultPrimaryEntityConfigurationBuilder.with(this.resolver.getEntityManager(recordType));
        for (PrimaryEntityConfigurationModule service : this.modules.get()) {
            try {
                PrimaryEntityConfigurationBuilder tempConfig = null;
                if (entity != null && service.supportsRecord(entity)) {
                    tempConfig = service.process(config, entity);
                } else if (entity == null && service.supportsEntityType(recordType)) {
                    tempConfig = service.process(config);
                }
                if (tempConfig != null) {
                    config = tempConfig;
                }
            } catch (Exception ex) {
                this.logger.warn("Failed to execute record configuration module [{}]: {}",
                    service.getClass().getName(), ex.getMessage());
            }
        }
        return config.build();
    }
}
