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

import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.stability.Unstable;

import java.util.Collection;

/**
 * A factory for objects of {@link GenePanel} type.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Unstable("New API introduced in 1.3")
public interface GenePanelFactory
{
    /**
     * Created an object of {@link GenePanel} class.
     *
     * @param presentFeatures the features of interest
     * @param vocabularyManager the vocabulary manager
     * @return a new {@link GenePanel} object
     */
    GenePanel makeGenePanel(final Collection<String> presentFeatures, final VocabularyManager vocabularyManager);
}
