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
package org.phenotips.panels;

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;

import javax.annotation.Nullable;

/**
 * A factory for objects of {@link GenePanel} type.
 *
 * @version $Id$
 * @since 1.3M6
 */
@Role
@Unstable("New API introduced in 1.3")
public interface GenePanelFactory
{
    /**
     * Create an object of {@link GenePanel} class, given a collection of {@link Feature} objects.
     *
     * @param features a collection of {@link Feature} objects, both observed to be present and observed to be absent
     * @return a new {@link GenePanel} object for the collection of features
     */
    GenePanel build(@Nullable final Collection<? extends Feature> features);

    /**
     * Creates an object of {@link GenePanel} class, given a collection of present and absent {@link VocabularyTerm}
     * objects.
     *
     * @param presentTerms present {@link VocabularyTerm} objects
     * @param absentTerms absent {@link VocabularyTerm} objects
     * @return a new {@link GenePanel} object for the collection of present and absent {@link VocabularyTerm} objects
     */
    GenePanel build(@Nullable final Collection<VocabularyTerm> presentTerms,
        @Nullable final Collection<VocabularyTerm> absentTerms);

    /**
     * Create an object of {@link GenePanel} class for a given {@link Patient} object.
     *
     * @param patient the {@link Patient} of interest
     * @return a new {@link GenePanel} object for the patient
     */
    GenePanel build(@Nullable final Patient patient);
}
