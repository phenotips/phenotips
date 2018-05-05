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
package org.phenotips.entities.script;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityConnectionsManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Allows users to get connection managers.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("entityConnections")
@Singleton
public class PrimaryEntityConnectionsScriptService implements ScriptService
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> cm;

    @Inject
    private Logger logger;

    /**
     * Returns the requested primary entity connections manager, if allowed. Only secure connections manager are
     * allowed, meaning that their name ends with {@code /secure}.
     *
     * @param connectionName the name of the connections manager to retrieve
     * @return the requested connection manager, if allowed and found, {@code null} otherwise
     */
    @Nullable
    public PrimaryEntityConnectionsManager<PrimaryEntity, PrimaryEntity> get(@Nullable String connectionName)
    {
        if (!StringUtils.endsWith(connectionName, "/secure")) {
            this.logger.warn("Non-secure connection manager requested: [{}]", connectionName);
            return null;
        }
        try {
            return this.cm.get().getInstance(PrimaryEntityConnectionsManager.class, connectionName);
        } catch (ComponentLookupException e) {
            this.logger.warn("Unknown connection manager requested: [{}]", connectionName);
        }
        return null;
    }
}
