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
package org.phenotips.vocabulary.internal.solr;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.apache.solr.common.SolrDocument;

/**
 * Implementation for {@link VocabularyTerm} based on an indexed Solr document.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M8)
 */
public class SolrVocabularyTerm extends AbstractSolrVocabularyTerm
{
    /**
     * Constructor that provides the backing {@link #doc Solr document} and the {@link #vocabulary owner vocabulary}.
     *
     * @param doc the {@link #doc Solr document} representing this term
     * @param vocabulary the {@link #vocabulary owner vocabulary}
     */
    public SolrVocabularyTerm(SolrDocument doc, Vocabulary vocabulary)
    {
        super(doc, vocabulary);
        initialize();
    }
}
