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
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.PrimaryEntityResolver;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of the {@link PrimaryEntityResolver} component, which uses all the
 * {@link PrimaryEntityManager entity managers} registered in the component manager.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class DefaultPrimaryEntityResolver implements PrimaryEntityResolver, Initializable
{
    /** The currently available primary entity managers. */
    @Inject
    private Map<String, PrimaryEntityManager> repositories;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceParser;

    /** Currently available primary entity managers, mapped by their entity ID prefix. */
    private Map<String, PrimaryEntityManager> repositoriesByPrefix;

    /** Currently available primary entity managers, mapped by their type. */
    private Map<String, PrimaryEntityManager> repositoriesByType;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.repositoriesByPrefix = new HashMap<>();
            this.repositoriesByType = new HashMap<>();

            this.repositories.values().forEach(this::addToMaps);
        } catch (final RuntimeException ex) {
            throw new InitializationException(ex.getMessage());
        }
    }

    @Nullable
    @Override
    public PrimaryEntity resolveEntity(@Nullable final String entityId)
    {
        // Not a valid id.
        if (StringUtils.isBlank(entityId)) {
            return null;
        }
        final DocumentReference entityDoc = this.referenceParser.resolve(entityId);
        if (entityDoc == null) {
            return null;
        }
        // Entity name cannot be null or empty.
        // Try to get the prefix; don't bother searching if it's blank.
        final String prefix = entityDoc.getName().replaceAll("^(\\D+)\\d+$", "$1");
        if (StringUtils.isBlank(prefix)) {
            return null;
        }
        // Get the repository by prefix.
        final PrimaryEntityManager repository = this.repositoriesByPrefix.get(prefix);
        // Try to get the entity.
        return repository == null ? null : repository.get(entityId);
    }

    @Nullable
    @Override
    public PrimaryEntityManager getEntityManager(@Nullable final String managerType)
    {
        return StringUtils.isNotBlank(managerType) ? this.repositoriesByType.get(managerType) : null;
    }

    @Override
    public boolean hasEntityManager(@Nullable final String managerType)
    {
        return StringUtils.isNotBlank(managerType) && this.repositoriesByType.containsKey(managerType);
    }

    /**
     * Tries to get the id prefix from the provided primary entity {@code manager}. Throws an exception if the prefix
     * is blank.
     *
     * @param manager the {@link PrimaryEntityManager} from which the id prefix will be retrieved; must not be null
     * @return the id prefix for the specified {@code manager}
     */
    private String getIdPrefix(@Nonnull final PrimaryEntityManager manager)
    {
        final String prefix = manager.getIdPrefix();
        if (StringUtils.isBlank(prefix)) {
            throw new RuntimeException("No prefix specified for PrimaryEntityManager");
        }
        return prefix;
    }

    /**
     * Tries to get the manager type from the provided primary entity {@code manager}. Throws an exception if the type
     * is blank.
     *
     * @param manager the {@link PrimaryEntityManager} from which the manager type will be retrieved; must not be null
     * @return the manager type for the specified {@code manager}
     */
    private String getManagerType(@Nonnull final PrimaryEntityManager manager)
    {
        final String type = manager.getType();
        if (StringUtils.isBlank(type)) {
            throw new RuntimeException("No type specified for PrimaryEntityManager");
        }
        return type;
    }

    /**
     * Adds the manager to internal maps.
     *
     * @param manager the {@link PrimaryEntityManager}
     */
    private void addToMaps(@Nonnull final PrimaryEntityManager manager)
    {
        final String idPrefix = this.getIdPrefix(manager);
        if (this.repositoriesByPrefix.containsKey(idPrefix)) {
            throw new RuntimeException("PrimaryEntityManager objects must not have the same ID prefix. Duplicate "
                + "prefix detected: " + idPrefix);
        }
        this.repositoriesByPrefix.put(idPrefix, manager);

        final String managerType = this.getManagerType(manager);
        if (this.repositoriesByType.containsKey(managerType)) {
            throw new RuntimeException("PrimaryEntityManager objects must not have the same type. Duplicate type "
                + "detected: " + managerType);
        }
        this.repositoriesByType.put(managerType, manager);
    }
}
