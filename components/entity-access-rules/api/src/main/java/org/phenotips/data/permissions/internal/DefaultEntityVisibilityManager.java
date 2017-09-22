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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.PermissionsConfiguration;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

/**
 * The default implementation of {@link EntityVisibilityManager}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class DefaultEntityVisibilityManager implements EntityVisibilityManager
{
    private static final String VISIBILITY = "visibility";

    @Inject
    private Logger logger;

    @Inject
    private PermissionsConfiguration configuration;

    @Inject
    private EntityAccessHelper helper;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<EntityReference> partialEntityResolver;

    @Inject
    @Named("private")
    private Visibility privateVisibility;

    @Nonnull
    @Override
    public Collection<Visibility> listVisibilityOptions()
    {
        final Collection<Visibility> result = listAllVisibilityOptions();
        result.removeIf(Visibility::isDisabled);
        return result;
    }

    @Nonnull
    @Override
    public Collection<Visibility> listAllVisibilityOptions()
    {
        try {
            Collection<Visibility> result = new TreeSet<>();
            result.addAll(this.componentManager.get().getInstanceList(Visibility.class));
            return result;
        } catch (final ComponentLookupException ex) {
            return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public Visibility getDefaultVisibility()
    {
        return resolveVisibility(this.configuration.getDefaultVisibility());
    }

    @Nonnull
    @Override
    public Visibility resolveVisibility(@Nullable final String name)
    {
        try {
            if (StringUtils.isNotBlank(name)) {
                final Visibility visibility = this.componentManager.get().getInstance(Visibility.class, name);
                // The component manager doesn't seem to throw the exception if looking up component with wrong name...
                return visibility == null ? this.privateVisibility : visibility;
            }
        } catch (ComponentLookupException ex) {
            this.logger.warn("Invalid entity visibility requested: {}", name);
        }
        return this.privateVisibility;
    }

    @Override
    public boolean setVisibility(
        @Nullable final PrimaryEntity entity,
        @Nullable final Visibility visibility)
    {
        if (entity == null || entity.getDocumentReference() == null) {
            return false;
        }
        final DocumentReference classReference =
            this.partialEntityResolver.resolve(Visibility.CLASS_REFERENCE, entity.getDocumentReference());
        try {
            final String visibilityAsString = (visibility != null) ? visibility.getName() : StringUtils.EMPTY;
            final String currentVisibility = this.helper.getStringProperty(entity.getXDocument(), classReference,
                VISIBILITY);
            if (!visibilityAsString.equals(currentVisibility)) {
                this.helper.setProperty(entity.getXDocument(), classReference, VISIBILITY, visibilityAsString);
                final XWikiContext context = this.xcontextProvider.get();
                context.getWiki().saveDocument(entity.getXDocument(), "Set visibility: " + visibilityAsString,
                    true, context);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Nonnull
    @Override
    public Visibility getVisibility(@Nullable final PrimaryEntity entity)
    {
        if (entity == null) {
            return this.privateVisibility;
        }
        final DocumentReference classReference =
            this.partialEntityResolver.resolve(Visibility.CLASS_REFERENCE, entity.getDocumentReference());
        final String visibility = this.helper.getStringProperty(entity.getXDocument(), classReference, VISIBILITY);
        return resolveVisibility(visibility);
    }

    @Nonnull
    @Override
    public Collection<? extends PrimaryEntity> filterByVisibility(
        @Nullable final Collection<? extends PrimaryEntity> entities,
        @Nullable final Visibility requiredVisibility)
    {
        if (entities == null) {
            return Collections.emptyList();
        }
        return requiredVisibility == null
            ? entities
            : entities.stream()
                // Filter out any nulls.
                .filter(Objects::nonNull)
                // Select entities with visibility above or the same as the required visibility.
                .filter(entity -> requiredVisibility.compareTo(getVisibility(entity)) <= 0)
                // Get a list of entities that meet the criteria.
                .collect(Collectors.toList());

    }

    @Nonnull
    @Override
    public Iterator<? extends PrimaryEntity> filterByVisibility(
        @Nullable final Iterator<? extends PrimaryEntity> entities,
        @Nullable final Visibility requiredVisibility)
    {
        if (entities == null) {
            return Collections.emptyIterator();
        }
        if (requiredVisibility == null) {
            return entities;
        }
        return !entities.hasNext()
            ? Collections.emptyIterator()
            : new FilteringIterator(entities, requiredVisibility, this);
    }
}
