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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Role
public interface EntityAccessHelper
{
    /**
     * Gets the current {@link DocumentReference} user document.
     *
     * @return the {@link DocumentReference} for the current user
     */
    DocumentReference getCurrentUser();

    /**
     * Retrieves the entity type of {@code userOrGroup}.
     *
     * @param userOrGroup an {@link EntityReference} object representing a user or a group
     * @return the type of entity (e.g. "user") that {@code userOrGroup} is
     */
    @Nonnull
    String getType(@Nullable EntityReference userOrGroup);

    /**
     * Retrieves the entity document of {@code userOrGroup}.
     *
     * @param userOrGroup an {@link EntityReference} object representing a user or a group
     * @return the {@link Document} of entity
     */
    @Nonnull
    Document getDocument(EntityReference userOrGroup);

    /**
     * Gets the string property value given the xwiki {@code doc}, the {@code classReference}, and the
     * {@code propertyName}.
     *
     * @param doc the {@link XWikiDocument}
     * @param classReference {@link DocumentReference} for property
     * @param propertyName the name of the property
     * @return the property value, as string
     */
    @Nullable
    String getStringProperty(
        @Nonnull XWikiDocument doc,
        @Nullable DocumentReference classReference,
        @Nullable String propertyName);

    /**
     * Sets the {@code propertyName} and {@code propertyValue} with {@code classReference}, to the {@code doc}.
     *
     * @param doc the {@link XWikiDocument}
     * @param classReference the {@link DocumentReference} for the property
     * @param propertyName the name of the property
     * @param propertyValue the property value
     */
    void setProperty(
        @Nonnull XWikiDocument doc,
        @Nullable DocumentReference classReference,
        @Nullable String propertyName,
        @Nullable Object propertyValue);
}
