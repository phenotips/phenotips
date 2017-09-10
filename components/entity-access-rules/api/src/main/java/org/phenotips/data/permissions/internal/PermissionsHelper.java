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

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Role
public interface PermissionsHelper
{
    @Nullable
    DocumentReference getCurrentUser();

    @Nonnull
    String getType(@Nullable EntityReference userOrGroup);

    /**
     * Gets the string property value for the {@code propertyName property} of interest for provided
     * {@code classReference class}.
     *
     * @param doc the {@link XWikiDocument} being processed
     * @param classReference the {@link DocumentReference} that contains the property of interest
     * @param propertyName the name of the property of interest
     * @return the property value as string
     */
    String getStringProperty(
        @Nonnull XWikiDocument doc,
        @Nonnull DocumentReference classReference,
        @Nonnull String propertyName);

    /**
     * Sets the given {@code propertyValue value} for property with {@code propertyName name}, belonging to given
     * {@code classReference class}.
     *
     * @param doc the {@link XWikiDocument} being processed
     * @param classReference the {@link DocumentReference} that contains the property of interest
     * @param propertyName the name of the property of interest
     * @param propertyValue the value that should be set for the property
     * @throws Exception if property cannot be set
     */
    void setProperty(
        @Nonnull XWikiDocument doc,
        @Nonnull DocumentReference classReference,
        @Nonnull String propertyName,
        @Nonnull Object propertyValue) throws Exception;
}
