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
package org.phenotips.panels.rest;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import org.json.JSONObject;

import com.google.common.cache.LoadingCache;

/**
 * Provides access to the available vocabularies and their terms.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Unstable
@Role
public interface GenePanelsLoadingCache
{
    /**
     * Gets the stored {@link LoadingCache} instance.
     *
     * @return the stored {@link LoadingCache} instance
     */
    LoadingCache<String, JSONObject> getCache();
}
