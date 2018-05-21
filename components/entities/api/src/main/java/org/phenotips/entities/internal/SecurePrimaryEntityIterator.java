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
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An iterator on an immutable, primary entities collection, which only returns the entities that the current user has
 * access to. This class is NOT thread-safe.
 *
 * @param <E> the type of entities listed by this iterator
 * @version $Id$
 * @since 1.4
 */
public class SecurePrimaryEntityIterator<E extends PrimaryEntity> implements Iterator<E>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurePrimaryEntityIterator.class);

    private final Iterator<E> internalIterator;

    private final User currentUser;

    private final AuthorizationService authorizationService;

    /** The constructor for the secure concrete {@link PrimaryEntity} instance class being managed. */
    private final Constructor<? extends E> secureConstructor;

    private E nextEntity;

    /**
     * Default constructor.
     *
     * @param internalIterator iterator for a collection of entities that this class wraps with security
     * @param authorizationService the authorization manager actually responsible for checking if an entity is
     *            accessible
     * @param currentUser the current user, may be {@code null}
     * @param secureConstructor the constructor that can create a new secure entity wrapping an unsecured one
     */
    public SecurePrimaryEntityIterator(final @Nonnull Iterator<E> internalIterator,
        final @Nonnull AuthorizationService authorizationService, final @Nullable User currentUser,
        final @Nonnull Constructor<? extends E> secureConstructor)
    {
        if (!ObjectUtils.allNotNull(internalIterator, authorizationService, secureConstructor)) {
            throw new IllegalArgumentException();
        }
        this.internalIterator = internalIterator;
        this.currentUser = currentUser;
        this.authorizationService = authorizationService;
        this.secureConstructor = secureConstructor;

        this.findNextEntity();
    }

    @Override
    public boolean hasNext()
    {
        return this.nextEntity != null;
    }

    @Override
    public E next()
    {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final E toReturn = this.nextEntity;
        this.findNextEntity();

        return this.createSecureEntity(toReturn);
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    private void findNextEntity()
    {
        this.nextEntity = null;

        while (this.internalIterator.hasNext() && this.nextEntity == null) {
            E potentialNextEntity = this.internalIterator.next();
            if (this.authorizationService.hasAccess(this.currentUser, Right.VIEW,
                potentialNextEntity.getDocumentReference())) {
                this.nextEntity = potentialNextEntity;
            }
        }
    }

    /**
     * Returns a new secure wrapper around the provided entity.
     *
     * @param entity the entity to secure
     * @return a secure wrapper around the entity, or {@code null} if creating the wrapper failed
     */
    protected E createSecureEntity(E entity)
    {
        try {
            return this.secureConstructor.newInstance(entity);
        } catch (IllegalArgumentException | InvocationTargetException | InstantiationException
            | IllegalAccessException ex) {
            LOGGER.error("Failed to instantiate secure primary entity wrapper from entity [{}]: {}",
                entity.getId(), ex.getMessage());
        }
        return null;
    }
}
