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
package org.phenotips.panels.rest.internal;

import org.phenotips.panels.GenePanel;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

/**
 * Provides access to the available vocabularies and their terms.
 *
 * @version $Id$
 * @since 1.3
 */
@Unstable
@Role
interface GenePanelLoader
{
    /**
     * Get the {@link GenePanel} object based on a {@code termList} of IDs.
     *
     * @param panelData an object containing a list of present and/or absent term IDs as strings, and well as rejected
     *                  genes, if any; must not be null
     * @return a {@link GenePanel} object
     * @since 1.4 (updated)
     */
    GenePanel get(@Nonnull PanelData panelData) throws ExecutionException;

    /**
     * Discards all cached entries.
     */
    void invalidateAll();

    /**
     * Discards any cached value for {@code key}.
     *
     * @param key a key to be discarded, must not be null
     */
    void invalidate(@Nonnull Object key);

    /**
     * Returns the size of the cache.
     *
     * @return the size of the cache
     */
    long size();
}
