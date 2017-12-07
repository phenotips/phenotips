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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * The abstract implementation of the {@link PrimaryEntityResolver} interface.
 *
 * @version $Id$
 * @since 1.4
 */
public abstract class AbstractPrimaryEntityResolver implements PrimaryEntityResolver
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceParser;

    @Inject
    @Named("context")
    private Provider<ComponentManager> cmProvider;

    @Inject
    private Logger logger;

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
        if (StringUtils.isBlank(prefix) || prefix.equals(entityId)) {
            return null;
        }

        final List<PrimaryEntityManager> managers = getAvailableManagers();
        return managers.isEmpty() ? null : performSearch(managers, prefix, entityId);
    }

    @Nullable
    @Override
    public PrimaryEntityManager<?> getEntityManager(@Nullable final String managerType)
    {
        return StringUtils.isNotBlank(managerType)
            ? getAvailableManagers().stream()
                .filter(manager -> managerType.equals(manager.getType()))
                .findFirst()
                .orElse(null)
            : null;
    }

    @Override
    public boolean hasEntityManager(@Nullable final String managerType)
    {
        return getEntityManager(managerType) != null;
    }

    /**
     * Tries to resolve the {@code entityId} to a {@link PrimaryEntity}.
     *
     * @param managers a list of {@link PrimaryEntityManager}s to search for {@code entityId}
     * @param prefix the {@code entityId} prefix, as retrieved from {@code entityId}
     * @param entityId the identifier for the {@link PrimaryEntity} of interest
     * @return the {@link PrimaryEntity} corresponding with the {@code entityId}, {@code null} if nothing found
     */
    @Nullable
    private PrimaryEntity performSearch(
        @Nonnull final List<PrimaryEntityManager> managers,
        @Nonnull final String prefix,
        @Nonnull final String entityId)
    {
        return managers.stream()
            .filter(manager -> prefix.equals(manager.getIdPrefix()))
            .map(manager -> manager.get(entityId))
            .filter(Objects::nonNull)
            .findFirst().orElseGet(
                () -> this.performSecondarySearch(managers, prefix, entityId)
            );
    }

    /**
     * Try to search all {@code managers} that do not match {@code prefix} for {@code entityId}. Should only be
     * performed if search by prefix fails.
     *
     * @param managers a list of {@link PrimaryEntityManager}
     * @param entityId the identifier of {@link PrimaryEntity} of interest, as string
     * @return the {@link PrimaryEntity} corresponding with the {@code entityId}, {@code null} if nothing found
     */
    @Nullable
    private PrimaryEntity performSecondarySearch(
        @Nonnull final List<PrimaryEntityManager> managers,
        @Nonnull final String prefix,
        @Nonnull final String entityId)
    {
        return managers.stream()
            .filter(manager -> !prefix.equals(manager.getIdPrefix()))
            .map(manager -> manager.get(entityId))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets all the available {@link PrimaryEntityManager}s.
     *
     * @return all available {@link PrimaryEntityManager}s
     */
    @Nonnull
    private List<PrimaryEntityManager> getAvailableManagers()
    {
        try {
            // Returns a list of all primary entity managers.
            final List<PrimaryEntityManager> mgrs = this.cmProvider.get().getInstanceList(PrimaryEntityManager.class);
            // Filter out the managers with the wrong name.
            return mgrs.stream()
                .filter(this::isValidManager)
                .collect(Collectors.toList());
        } catch (ComponentLookupException e) {
            this.logger.debug("Unable to retrieve primary entity managers: [{}].", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns true iff the {@link PrimaryEntityResolver} implementation should use {@code manager} to resolve entities,
     * for example, a secure implementation should only use managers that are secure.
     *
     * @return true iff {@code manager} should be used to resolve entities, false otherwise
     */
    abstract boolean isValidManager(@Nonnull PrimaryEntityManager<?> manager);
}
