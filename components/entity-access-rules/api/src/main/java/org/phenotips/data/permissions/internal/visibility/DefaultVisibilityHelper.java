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
package org.phenotips.data.permissions.internal.visibility;

import org.phenotips.data.permissions.PermissionsConfiguration;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.FilteringIterator;
import org.phenotips.data.permissions.internal.PermissionsHelper;
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
 * The default implementation of {@link VisibilityHelper}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class DefaultVisibilityHelper implements VisibilityHelper
{
    private static final String VISIBILITY = "visibility";

    @Inject
    private Logger logger;

    @Inject
    private PermissionsConfiguration configuration;

    @Inject
    private PermissionsHelper helper;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<EntityReference> partialEntityResolver;

    @Override
    @Nonnull
    public Collection<Visibility> listVisibilityOptions()
    {
        final Collection<Visibility> result = listAllVisibilityOptions();
        result.removeIf(Visibility::isDisabled);
        return result;
    }

    @Override
    @Nonnull
    public Collection<Visibility> listAllVisibilityOptions()
    {
        try {
            Collection<Visibility> result = new TreeSet<>();
            result.addAll(this.componentManager.get().getInstanceList(Visibility.class));
            return result;
        } catch (ComponentLookupException ex) {
            return Collections.emptyList();
        }
    }

    @Override
    @Nonnull
    public Visibility getDefaultVisibility()
    {
        return resolveVisibility(this.configuration.getDefaultVisibility());
    }

    @Override
    @Nonnull
    public Visibility resolveVisibility(@Nullable final String name)
    {
        try {
            if (StringUtils.isNotBlank(name)) {
                return this.componentManager.get().getInstance(Visibility.class, name);
            }
        } catch (ComponentLookupException ex) {
            this.logger.warn("Invalid patient visibility requested: {}", name);
        }
        return new PrivateVisibility();
    }

    @Override
    public boolean setVisibility(@Nonnull final PrimaryEntity entity, @Nullable final Visibility visibility)
    {
        final DocumentReference classReference = this.partialEntityResolver.resolve(Visibility.CLASS_REFERENCE,
            entity.getDocumentReference());
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

    @Override
    @Nonnull
    public Visibility getVisibility(@Nonnull final PrimaryEntity entity)
    {
        final DocumentReference classReference = this.partialEntityResolver.resolve(Visibility.CLASS_REFERENCE,
            entity.getDocumentReference());
        final String visibility = this.helper.getStringProperty(entity.getXDocument(), classReference, VISIBILITY);
        return StringUtils.isNotBlank(visibility) ? resolveVisibility(visibility) : getDefaultVisibility();
    }

    @Override
    @Nonnull
    public Collection<PrimaryEntity> filterByVisibility(
        @Nullable final Collection<PrimaryEntity> entities,
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

    @Override
    @Nonnull
    public Iterator<PrimaryEntity> filterByVisibility(
        @Nullable final Iterator<PrimaryEntity> entities,
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
