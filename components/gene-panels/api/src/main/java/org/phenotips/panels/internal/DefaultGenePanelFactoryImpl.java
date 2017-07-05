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

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.panels.GenePanel;
import org.phenotips.panels.GenePanelFactory;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.Validate;

/**
 * Default implementation of the {@link GenePanelFactory}.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Singleton
public class DefaultGenePanelFactoryImpl implements GenePanelFactory
{
    /** The vocabulary manager required for accessing the available vocabularies. */
    @Inject
    private VocabularyManager vocabularyManager;

    @Override
    public GenePanel build(@Nonnull final Collection<VocabularyTerm> presentTerms,
        @Nonnull final Collection<VocabularyTerm> absentTerms)
    {
        Validate.notNull(presentTerms);
        Validate.notNull(absentTerms);
        return new DefaultGenePanelImpl(presentTerms, absentTerms, this.vocabularyManager);
    }

    @Override
    public GenePanel build(@Nonnull final Collection<? extends Feature> features)
    {
        Validate.notNull(features);
        return new DefaultGenePanelImpl(features, this.vocabularyManager);
    }

    @Override
    public GenePanel build(@Nonnull final Patient patient)
    {
        Validate.notNull(patient);
        return new DefaultGenePanelImpl(patient, this.vocabularyManager);
    }
}
