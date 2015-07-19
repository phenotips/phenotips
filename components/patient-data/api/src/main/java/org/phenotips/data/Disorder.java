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
package org.phenotips.data;

import org.xwiki.stability.Unstable;

/**
 * Information about a specific disorder recorded for a {@link Patient patient}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public interface Disorder extends VocabularyProperty
{
    /**
     * Returns the PhenoTips value of the disorder.
     *
     * @return the value
     * @todo move to VocabularyProperty and/or implement a VocabularyProperty-to-PhenotipsPropertyName mapping service
     */
    String getValue();
}
