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
package org.phenotips.entities;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.IterableUtils;

/**
 * Manages connections between primary entities, where a connection is a <b>predicate</b> linking two primary entities,
 * a <b>subject</b> and an <b>object</b>, such as:
 * <ul>
 * <li><b>S contains O</b>, for example a Family contains one or more Patients</li>
 * <li><b>S belongs to O</b>, for example a Patient belongs to a Family</li>
 * <li><b>S uses O</b>, for example a Patient Record uses a Template</li>
 * <li><b>S is used by O</b>, for example a Template is used by one or more Patient Records</li>
 * </ul>
 * <p>
 * The actual connection manager implementation defines the <b>predicate</b> that connects the <b>subject</b> and the
 * <b>object</b>. Two inverse predicates between two types of entities may be defined. Each predicate may be one-to-one,
 * one-to-many, many-to-one, or many-to-many, depending on the semantics of the predicate itself.
 * </p>
 * <p>
 * For the special case of *-to-one predicates, where each subject can be connected to at most one object, special
 * simple methods are provided for convenience: {@link #get(PrimaryEntity)}, {@link #set(PrimaryEntity, PrimaryEntity)},
 * and {@link #remove(PrimaryEntity)}. This type of predicate is called a <b>property</b>.
 * </p>
 * <p>
 * Default implementations for many of the methods are provided, but should be re-implemented when a faster approach is
 * possible.
 * </p>
 * <p>
 * It is recommended that the name of each implementation follows the pattern: {@code subjectType-predicate-objectType},
 * for example {@code family-contains-patient}, {@code patient-belongsTo-family}, {@code patient-uses-template}, and
 * {@code template-usedBy-patient}.
 * </p>
 * <p>
 * Each implementation should be thread-safe, but must not necessarily be atomic. This means that parallel calls to
 * these methods shouldn't raise concurrency exceptions, but, for example, calling
 * {@link #set(PrimaryEntity, PrimaryEntity)} for a subject that is connected to 4 objects before the call may end up
 * breaking two of those connections, leaving the other two in place, and not creating the new connection, or if two
 * calls to {@link #set(PrimaryEntity, PrimaryEntity)} occur at the same time, the subject may end up connected to
 * either, none, or both of the new objects.
 * </p>
 *
 * @param <S> the type of entities being the subject of the connection
 * @param <O> the type of entities being the object of the connection
 * @version $Id$
 * @since 1.4
 */
@Unstable("New API introduced in 1.4")
@Role
public interface PrimaryEntityConnectionsManager<S extends PrimaryEntity, O extends PrimaryEntity>
{
    /**
     * Creates a new connection between the Subject and the Object, if one didn't exist already.
     *
     * @param subject the subject to connect from
     * @param object the object to connect to
     * @return {@code true} if the connection was successfully created, or already existed, {@code false} if the
     *         operation failed
     */
    boolean connect(@Nonnull S subject, @Nonnull O object);

    /**
     * Creates connections between the subject and each of the objects. Each individual connection creation is
     * attempted, even if one of them fails.
     *
     * @param subject the subject to connect from
     * @param objects a collection of objects to connect to
     * @return {@code true} if all the connections were successfully created, {@code false} if any of the operations
     *         failed
     */
    default boolean connectAll(@Nonnull S subject, @Nullable Collection<O> objects)
    {
        if (subject == null) {
            throw new IllegalArgumentException();
        }
        return objects == null
            || objects.stream().filter(Objects::nonNull).map(object -> connect(subject, object))
                .reduce(Boolean.TRUE, (b1, b2) -> b1 && b2);
    }

    /**
     * Creates connections between each subject and the object. Each individual connection creation is attempted, even
     * if one of them fails.
     *
     * @param subjects a collection of subjects to connect from
     * @param object the object to connect to
     * @return {@code true} if all the connections were successfully created, {@code false} if any of the operations
     *         failed
     */
    default boolean connectFromAll(@Nullable Collection<S> subjects, @Nonnull O object)
    {
        if (object == null) {
            throw new IllegalArgumentException();
        }
        return subjects == null
            || subjects.stream().filter(Objects::nonNull).map(subject -> connect(subject, object))
                .reduce(Boolean.TRUE, (b1, b2) -> b1 && b2);
    }

    /**
     * Deletes a connection between a subject and an object, if it exists already.
     *
     * @param subject the subject to disconnect from
     * @param object the object to be disconnected
     * @return {@code true} if the connection was successfully deleted, or if it didn't exist yet, {@code false} if the
     *         operation failed
     */
    boolean disconnect(@Nonnull S subject, @Nonnull O object);

    /**
     * Deletes all the connections between a subject and the specified objects. Each individual connection deletion is
     * attempted, even if one of them fails.
     *
     * @param subject the subject to disconnect from
     * @param objects the objects to be disconnected
     * @return {@code true} if all the connections were successfully deleted, {@code false} if any of the operations
     *         failed
     */
    default boolean disconnectAll(@Nonnull S subject, @Nullable Collection<O> objects)
    {
        if (subject == null) {
            throw new IllegalArgumentException();
        }
        return objects == null
            || objects.stream().filter(Objects::nonNull).map(object -> disconnect(subject, object))
                .reduce(Boolean.TRUE, (b1, b2) -> b1 && b2);
    }

    /**
     * Deletes all connections of this type from the subject. Each individual connection deletion is attempted, even if
     * one of them fails.
     *
     * @param subject the subject to disconnect from
     * @return {@code true} if all the connections were successfully deleted, {@code false} if any of the operations
     *         failed
     */
    default boolean disconnectAll(@Nonnull S subject)
    {
        if (subject == null) {
            throw new IllegalArgumentException();
        }
        return this.disconnectAll(subject, getAllConnections(subject));
    }

    /**
     * Deletes all connections of this type pointing to the object. Each individual connection deletion is attempted,
     * even if one of them fails.
     *
     * @param object the object to disconnect
     * @return {@code true} if all the connections were successfully deleted, {@code false} if any of the operations
     *         failed
     */
    default boolean disconnectFromAll(@Nonnull O object)
    {
        if (object == null) {
            throw new IllegalArgumentException();
        }
        return this.getAllReverseConnections(object).stream().map(subject -> disconnect(subject, object))
            .reduce(Boolean.TRUE, (b1, b2) -> b1 && b2);
    }

    /**
     * Lists all the object entities that are connected from the specified subject.
     *
     * @param subject the subject whose connections are to be listed
     * @return a collection of Entities, may be empty
     */
    @Nonnull
    Collection<O> getAllConnections(@Nonnull S subject);

    /**
     * Lists all the subject entities that are connected to the specified object.
     *
     * @param object the object whose connections are to be listed
     * @return a collection of Entities, may be empty
     */
    @Nonnull
    Collection<S> getAllReverseConnections(@Nonnull O object);

    /**
     * Checks if there is a connection between {@code subject} and {@code object}.
     *
     * @param subject the subject to investigate
     * @param object the object to investigate
     * @return {@code true} if a connection exists, {@code false} otherwise
     */
    default boolean isConnected(@Nonnull S subject, @Nonnull O object)
    {
        // This can be implemented more efficiently, and should be re-implemented when possible
        return subject != null && object != null && getAllConnections(subject).contains(object);
    }

    /**
     * For property predicates, gets the single object connected from the specified subject, if any. If this isn't a
     * *-to-one predicate, and multiple objects are connected from the subject, {@code null} is returned.
     *
     * @param subject the subject to get the property from
     * @return an object, or {@code null} if not set
     */
    @Nullable
    default O get(@Nonnull S subject)
    {
        if (subject == null) {
            throw new IllegalArgumentException();
        }
        Collection<O> objects = this.getAllConnections(subject);
        if (objects.size() == 1) {
            return IterableUtils.get(objects, 0);
        }
        return null;
    }

    /**
     * For property predicates, sets a new value for the given subject, breaking any pre-existing connections. If the
     * value is {@code null}, all connections from the subject are deleted.
     *
     * @param subject the subject for which to set the property
     * @param property the property to set; if {@code null}, this operation behaves like {@link #remove(PrimaryEntity)}
     * @return {@code true} if the property was successfully set, {@code false} if breaking previous connections or
     *         creating the new connection failed, or if the subject is {@code null}
     */
    default boolean set(@Nonnull S subject, @Nullable O property)
    {
        if (subject == null) {
            throw new IllegalArgumentException();
        }
        boolean result = this.disconnectAll(subject);
        if (result && property != null) {
            result = connect(subject, property);
        }
        return result;
    }

    /**
     * For property predicates, removes a property from a subject. This behaves the same as
     * {@link #disconnectAll(PrimaryEntity)}, so for *-to-many predicates, this removes all connections from the
     * subject.
     *
     * @param subject the subject for which to remove the property
     * @return {@code true} if the property was successfully removed, or if it wasn't set, {@code false} if the
     *         operation failed
     */
    default boolean remove(@Nonnull S subject)
    {
        if (subject == null) {
            throw new IllegalArgumentException();
        }
        return disconnectAll(subject);
    }
}
