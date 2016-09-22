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

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Iterator;

/**
 * API that provides access for a specific type of entity, with support for simple CRUD operations. No access rights are
 * checked here.
 *
 * @param <E> the type of entities handled by this manager
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New API introduced in 1.3")
@Role
public interface PrimaryEntityManager<E extends PrimaryEntity>
{
    /**
     * Gets the space where entities of this type are stored in.
     *
     * @return a local space reference, without the wiki name
     */
    EntityReference getDataSpace();

    /**
     * Creates and returns a new empty entity, setting the currently logged in user as the creator.
     *
     * @return the created entity, or {@code null} in case of errors
     */
    E create();

    /**
     * Creates and returns a new empty entity, setting the given principal as the creator.
     *
     * @param creator a reference to the document representing a principal (a user or a group) which will be set as the
     *            creator for the created entity
     * @return the created entity, or {@code null} in case of errors
     */
    @Unstable("The type of the parameter will be replaced by Principal, once the principals module is implemented")
    E create(DocumentReference creator);

    /**
     * Retrieves an {@link PrimaryEntity entity} by its {@link PrimaryEntity#getId() internal PhenoTips identifier}.
     *
     * @param id the {@link PrimaryEntity#getId() entity identifier}, i.e. the serialized document reference
     * @return the requested entity
     */
    E get(String id);

    /**
     * Retrieves an {@link PrimaryEntity entity} from the specified document.
     *
     * @param reference reference of the {@link PrimaryEntity#getDocument() document where the entity is stored}
     * @return the requested entity
     */
    E get(DocumentReference reference);

    /**
     * Retrieves an {@link PrimaryEntity entity} by its {@link PrimaryEntity#getName() name}.
     *
     * @param name the entity's {@link PrimaryEntity#getName() user-friendly name}
     * @return the requested entity, or {@code null} if the requested entity does not exist, is not really a type of the
     *         entity requested or multiple entities with the same name exists
     */
    E getByName(String name);

    /**
     * Retrieves all entities of the managed type, in a random order.
     *
     * @return an iterator over all entities, may be empty if no entities exist
     */
    Iterator<E> getAll();

    /**
     * Deletes an entity.
     *
     * @param entity the entity to delete
     * @return {@code true} if the entity was successfully deleted, {@code false} in case of error
     */
    boolean delete(E entity);

    /**
     * Loads and returns an entity from the specified document. This method will be removed once the new XWiki model is
     * implemented and the intermediary model bridge is no longer needed. Do not use.
     *
     * @param document the document where the entity is stored
     * @return the loaded entity
     * @throws IllegalArgumentException if the provided document doesn't contain a proper entity
     */
    @Unstable("The type of the parameter will be replaced by Document, once the new model module is implemented")
    E load(DocumentModelBridge document) throws IllegalArgumentException;
}
