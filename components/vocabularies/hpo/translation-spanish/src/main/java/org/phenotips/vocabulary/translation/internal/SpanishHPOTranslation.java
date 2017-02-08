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
package org.phenotips.vocabulary.translation.internal;

import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.translation.AbstractXliffTranslatedSolrVocabularyExtension;

import org.xwiki.component.annotation.Component;

import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.lucene.analysis.es.SpanishAnalyzer;

/**
 * Human Phenotype Ontology translated into Spanish.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("hpo-translation-spanish")
@Singleton
public class SpanishHPOTranslation extends AbstractXliffTranslatedSolrVocabularyExtension implements VocabularyExtension
{
    private static final Locale TARGET_LOCALE = Locale.forLanguageTag("es");

    @Override
    protected Locale getTargetLocale()
    {
        return TARGET_LOCALE;
    }

    @Override
    protected String getAnalyzerType()
    {
        return SpanishAnalyzer.class.getCanonicalName();
    }

    @Override
    protected String getTargetVocabularyId()
    {
        return "hpo";
    }
}
