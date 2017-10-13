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
package org.phenotips.vocabulary;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Provides methods for accessing the location of the vocabulary source files to reindex vocabulary from a different
 * location.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
@Role
public interface VocabularySourceRelocationService
{
    /**
     * get the XObject of type {@link PhenoTips.VocabularySourceRelocationClass} with the value for the {@link original}
     * property equal to the value of the {@code original} argument, and if not {@code null}, return the value of the
     * {@link relocation} property. If the relocation isn't defined, then just return {@code original}.
     *
     * @param original the identifier of the target vocabulary
     * @return the value of the relocation property
     */
    String getRelocation(String original);
}
