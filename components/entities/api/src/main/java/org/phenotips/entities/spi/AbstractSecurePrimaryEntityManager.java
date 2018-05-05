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
package org.phenotips.entities.spi;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.internal.SecurePrimaryEntityIterator;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Base class for implementing specific secure entity managers. A "secure" version of an entity manager is one that
 * enforces access right checks on all the methods. All the restrictions of the {@link AbstractPrimaryEntityManager
 * unsecured super class} apply, with the exception that secure variants of an entity manager should have a
 * {@code @Named} annotation ending with {@code /secure}, such as {@code @Named("Patient/secure")}.
 *
 * @param <E> the type of entities handled by this manager
 * @version $Id$
 * @since 1.4
 */
@Unstable("New class and interface added in 1.4")
public abstract class AbstractSecurePrimaryEntityManager<E extends PrimaryEntity>
    extends AbstractPrimaryEntityManager<E>
{
    /** Used for obtaining the current user. */
    @Inject
    protected UserManager userManager;

    @Inject
    protected AuthorizationService authorizationService;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    protected EntityReferenceResolver<EntityReference> currentResolver;

    /** Serializes references to strings. */
    @Inject
    protected EntityReferenceSerializer<String> serializer;

    /** The secure variant of the concrete {@link PrimaryEntity} instance class being managed. */
    private Class<? extends E> seclass;

    /** The constructor for the secure concrete {@link PrimaryEntity} instance class being managed. */
    private Constructor<? extends E> seconstructor;

    @Override
    public synchronized E create(DocumentReference creator)
    {
        User user = this.userManager.getUser(this.serializer.serialize(creator));
        if (this.authorizationService.hasAccess(user, Right.EDIT,
            this.currentResolver.resolve(getDataSpace(), EntityType.SPACE))) {
            return createSecureEntity(super.create(creator));
        }
        throw new SecurityException("User not authorized to create new entities");
    }

    @Override
    public E get(DocumentReference reference)
    {
        return checkAccess(super.get(reference));
    }

    @Override
    public Iterator<E> getAll()
    {
        final Iterator<E> unsecuredIterator = super.getAll();
        return new SecurePrimaryEntityIterator<>(unsecuredIterator, this.authorizationService,
            this.userManager.getCurrentUser(), getEntityConstructor());
    }

    @Override
    public boolean delete(E entity)
    {
        if (checkAccess(Right.DELETE, entity, this.userManager.getCurrentUser()) != null) {
            return super.delete(entity);
        }
        return false;
    }

    @Override
    public E load(DocumentModelBridge document) throws IllegalArgumentException
    {
        return createSecureEntity(super.load(document));
    }

    /**
     * Gets the concrete class of the managed PrimaryEntity wrapper. The base implementation assumes that the class has
     * the name of the primary entity class, prefixed with {@code Secure}, and can be found in the same package.
     * Override if this assumption isn't valid.
     *
     * @return a class
     * @throws AbstractMethodError if the class cannot be automatically found
     */
    @SuppressWarnings("unchecked")
    protected Class<? extends E> getSecureEntityClass()
    {
        if (this.seclass == null) {
            final Class<? extends E> primaryClass = getEntityClass();
            final String secureClassName = primaryClass.getCanonicalName().replaceFirst("\\.([^.]++)$", ".Secure$1");
            try {
                this.seclass =
                    (Class<? extends E>) Class.forName(secureClassName, true, primaryClass.getClassLoader());
            } catch (ClassNotFoundException ex) {
                this.logger.error(
                    "Incomplete secure entity manager implementation: cannot find the secure entity class");
                throw new AbstractMethodError(
                    "The SecurePrimaryEntityManager class " + this.getClass().getCanonicalName()
                        + " must override #getSecureEntityClass to define a real class for the secure entity wrapper");
            }
        }
        return this.seclass;
    }

    /**
     * Gets the constructor of the managed secure PrimaryEntity concrete class that accepts an unsecured PrimaryEntity
     * as the only parameter. Override if another constructor is needed, but keep in mind that
     * {@link #createSecureEntity(PrimaryEntity)} must also be overridden to pass in the required arguments.
     *
     * @return a constructor that requires an XWikiDocument as its only parameter
     * @throws AbstractMethodError if no such constructor can be found
     */
    protected Constructor<? extends E> getSecureEntityConstructor() throws AbstractMethodError
    {
        if (this.seconstructor == null) {
            try {
                this.seconstructor = getSecureEntityClass().getConstructor(getEntityClass());
            } catch (NullPointerException | NoSuchMethodException | SecurityException ex) {
                this.logger.error("Cannot instantiate secure primary entity [{}]: {}", getSecureEntityClass().getName(),
                    ex.getMessage());
                throw new AbstractMethodError(
                    "The Secure PrimaryEntity class " + getSecureEntityClass().getCanonicalName()
                        + " must have a public constructor that accepts an unsecured entity parameter");
            }
        }
        return this.seconstructor;

    }

    /**
     * Returns a secure wrapper around the provided {@link PrimaryEntity} if the current user has view access on the
     * provided {@code entity}.
     *
     * @param entity the {@link PrimaryEntity} of interest
     * @return a secure wrapper for the entity, {@code null} if {@code entity} is {@code null}.
     * @throws SecurityException if the current user does not have the right to view the entity
     */
    protected E checkAccess(E entity)
    {
        return checkAccess(Right.VIEW, entity, this.userManager.getCurrentUser());
    }

    /**
     * Returns a secure wrapper around the provided {@link PrimaryEntity} if {@code user} has the required {@code right}
     * for interacting with the provided {@code entity}.
     *
     * @param right the required {@link Right} that the user needs to have in order to interact with the entity
     * @param entity the {@link PrimaryEntity} of interest
     * @param user the {@link User} requesting access to {@code entity}
     * @return a secure wrapper for the entity, {@code null} if {@code entity} is {@code null}.
     * @throws SecurityException if {@code user} does not have the required {@code right} for {@code entity}
     */
    protected E checkAccess(Right right, E entity, User user)
    {
        if (entity != null && this.authorizationService.hasAccess(user, right, entity.getDocumentReference())) {
            return createSecureEntity(entity);
        } else if (entity != null) {
            this.logger.warn("Illegal access requested for entity [{}] by user [{}]", entity.getId(), user);
            throw new SecurityException("Unauthorized access");
        }
        return null;
    }

    /**
     * Returns a new secure wrapper around the provided entity.
     *
     * @param entity the entity to secure
     * @return a secure wrapper around the entity, or {@code null} if creating the wrapper failed
     */
    protected E createSecureEntity(E entity)
    {
        if (entity == null) {
            return null;
        }
        try {
            return getSecureEntityConstructor().newInstance(entity);
        } catch (IllegalArgumentException | InvocationTargetException ex) {
            this.logger.info("Tried to load invalid entity of type [{}] from document [{}]",
                entity.getDocumentReference(), getSecureEntityClass());
        } catch (InstantiationException | IllegalAccessException ex) {
            this.logger.error("Failed to instantiate secure primary entity of type [{}] from entity [{}]: {}",
                getSecureEntityClass(), entity.getId(), ex.getMessage());
        }
        return null;
    }
}
