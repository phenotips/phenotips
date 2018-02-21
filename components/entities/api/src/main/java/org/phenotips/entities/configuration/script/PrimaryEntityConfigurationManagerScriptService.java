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
package org.phenotips.entities.configuration.script;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.configuration.PrimaryEntityConfiguration;
import org.phenotips.entities.configuration.PrimaryEntityConfigurationManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Provides access to {@code RecordConfiguration record configurations}.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
@Component
@Named("primaryEntityConfiguration")
@Singleton
public class PrimaryEntityConfigurationManagerScriptService implements ScriptService
{
    /** The actual configuration manager. */
    @Inject
    private PrimaryEntityConfigurationManager configuration;

    /**
     * Retrieves the {@link PrimaryEntityConfiguration} active for the current user on a specific record type.
     *
     * @param recordType an identifier for the type of record whose configuration is requested, such as {@code patient}
     *            or {@code family}
     * @return a valid configuration, may be empty if no sections can be displayed
     */
    public PrimaryEntityConfiguration getConfiguration(String recordType)
    {
        return this.configuration.getConfiguration(recordType);
    }

    /**
     * Retrieves the {@link PrimaryEntityConfiguration} active for the current user on a specific record.
     *
     * @param entity the primary entity record whose configuration is requested
     * @return a valid configuration, may be empty if no sections can be displayed
     */
    public PrimaryEntityConfiguration getConfiguration(PrimaryEntity entity)
    {
        return this.configuration.getConfiguration(entity);
    }
}
