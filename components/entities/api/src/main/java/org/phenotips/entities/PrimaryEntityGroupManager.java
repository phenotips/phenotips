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
import org.xwiki.stability.Unstable;

import java.util.Collection;

/**
 * API that provides access for a specific type of groups of entities, with support for simple CRUD operations. No
 * access rights are checked here.
 *
 * @param <G> the type of groups handled by this manager
 * @param <E> the type of entities belonging to the groups handled by this manager; if more than one type of entities
 *            can be part of the groups, then a generic {@code PrimaryEntity} should be used instead
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New API introduced in 1.3")
@Role
public interface PrimaryEntityGroupManager<G extends PrimaryEntityGroup<E>, E extends PrimaryEntity>
    extends PrimaryEntityManager<G>
{
    /**
     * Creates and returns a new empty group, setting the currently logged in user as the creator.
     *
     * @return the created group
     */
    @Override
    G create();

    /**
     * Creates and returns a new empty group, setting the given principal as the creator.
     *
     * @param creator a reference to the document representing a principal (a user or a group) which will be set as the
     *            creator for the created group
     * @return the created group
     */
    @Override
    @Unstable("The type of the parameter will be replaced by Principal, once the principals module is implemented")
    G create(DocumentReference creator);

    /**
     * Retrieves a {@link PrimaryEntityGroup group} by its {@link PrimaryEntity#getId() internal PhenoTips identifier}.
     *
     * @param id the {@link PrimaryEntity#getId() group identifier}, i.e. the serialized document reference
     * @return the requested group
     * @throws IllegalArgumentException if the requested group does not exist or is not really a type of the group
     *             requested
     */
    @Override
    G get(String id);

    /**
     * Retrieves a {@link PrimaryEntityGroup group} from the specified document.
     *
     * @param reference reference of the {@link PrimaryEntity#getDocument() document where the entity is stored}
     * @return the requested group
     * @throws IllegalArgumentException if the document doesn't contain a proper group
     */
    @Override
    G get(DocumentReference reference);

    /**
     * Retrieves a {@link PrimaryEntityGroup group} by its {@link PrimaryEntity#getName() name}.
     *
     * @param externalId the group's {@link PrimaryEntity#getName() user-friendly name}
     * @return the requested group, or {@code null} if the requested group does not exist, is not really a type of the
     *         group requested or multiple groups with the same name exists
     */
    @Override
    G getByName(String name);

    /**
     * Retrieves groups that have the specified entity as their member, in a random order.
     *
     * @param entity the entity that must be a member of the returned groups
     * @return a collection of groups, may be empty
     */
    Collection<G> getGroupsForEntity(PrimaryEntity entity);

    /**
     * Retrieves all groups of the managed type, in a random order.
     *
     * @return a collection of groups, may be empty if no groups exist
     */
    @Override
    Collection<G> getAll();

    /**
     * Deletes a group.
     *
     * @param group the group to delete
     * @return {@code true} if the group was successfully deleted, {@code false} in case of error
     */
    @Override
    boolean delete(G group);

    /**
     * Loads and returns a group from the specified document. This method will be removed once the new XWiki model is
     * implemented and the intermediary model bridge is no longer needed. Do not use.
     *
     * @param document the document where the group is stored
     * @return the loaded group
     * @throws IllegalArgumentException if the provided document doesn't contain a proper group
     */
    @Override
    @Unstable("The type of the parameter will be replaced by Document, once the new model module is implemented")
    G load(DocumentModelBridge document) throws IllegalArgumentException;
}
