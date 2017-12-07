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
package org.phenotips.entities.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityMetadataManager;
import org.phenotips.entities.PrimaryEntityMetadataProvider;

import org.xwiki.component.annotation.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Default implementation of the {@link PrimaryEntityMetadataManager} component, which uses all the
 * {@link PrimaryEntityMetadataProvider metadata providers} registered in the component manager.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class DefaultPrimaryEntityMetadataManager implements PrimaryEntityMetadataManager
{
    @Inject
    private Provider<List<PrimaryEntityMetadataProvider>> allProviders;

    @Override
    public Map<String, Object> getMetadata(PrimaryEntity entity)
    {
        Map<String, Object> result = new HashMap<>();
        this.allProviders.get().forEach(provider -> result.putAll(provider.provideMetadata(entity)));
        return result;
    }
}
