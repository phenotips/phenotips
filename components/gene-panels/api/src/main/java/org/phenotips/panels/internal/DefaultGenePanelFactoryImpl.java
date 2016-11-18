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
package org.phenotips.panels.internal;

import org.phenotips.panels.GenePanel;
import org.phenotips.panels.GenePanelFactory;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.stability.Unstable;

import java.util.Collection;

import org.apache.commons.lang3.Validate;

/**
 * Default implementation of the {@link GenePanelFactory}.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Unstable("New API introduced in 1.3")
public class DefaultGenePanelFactoryImpl implements GenePanelFactory
{
    @Override
    public GenePanel makeGenePanel(final Collection<String> presentFeatures, final VocabularyManager vocabularyManager)
    {
        Validate.notNull(vocabularyManager);
        Validate.notNull(presentFeatures);
        return new DefaultGenePanelImpl(presentFeatures, vocabularyManager);
    }
}
