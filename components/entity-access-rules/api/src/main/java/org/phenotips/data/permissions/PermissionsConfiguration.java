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
package org.phenotips.data.permissions;

import org.phenotips.Constants;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Various configurations for the permissions module.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Unstable
@Role
public interface PermissionsConfiguration
{
    /** The XClass used for storing the configuration. */
    EntityReference VISIBILITY_CONFIGURATION_CLASS_REFERENCE =
        new EntityReference("VisibilityConfigurationClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** The document where preferences are stored. */
    EntityReference PREFERENCES_DOCUMENT =
        new EntityReference("XWikiPreferences", EntityType.DOCUMENT, Constants.XWIKI_SPACE_REFERENCE);

    /**
     * @return the name of the default visibility to set for new patient records, {@code null} if none is configured
     */
    String getDefaultVisibility();

    /**
     * @param visibilityName the {@link Visibility#getName() name} of the visibility to check
     * @return {@code true} if the specified visibility is disabled in the configuration, {@code false} otherwise
     */
    boolean isVisibilityDisabled(String visibilityName);
}
