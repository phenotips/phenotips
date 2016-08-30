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

import java.util.Collection;
import java.util.Map;

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
     * The solr document.
     */
    private SolrDocument doc;

    /**
     * Constructor that provides the backing {@link #doc Solr document} and the {@link #ontology owner ontology}.
     *
     * @param doc the {@link #doc Solr document} representing this term
     * @param ontology the {@link #ontology owner ontology}
     */
    public SolrVocabularyTerm(SolrDocument doc, Vocabulary ontology)
    {
        super(ontology);
        this.doc = doc;
    }

    @Override
    protected Iterable<Map.Entry<String, Object>> getEntrySet()
    {
        if (isNull()) {
            return null;
        }
        return this.doc.entrySet();
    }

    @Override
    protected Object getFirstValue(String key)
    {
        if (isNull()) {
            return null;
        }
        return doc.getFirstValue(key);
    }

    @Override
    public Collection<Object> getValues(String key)
    {
        if (isNull()) {
            return null;
        }
        return doc.getFieldValues(key);
    }

    @Override
    public Object get(String key)
    {
        if (isNull()) {
            return null;
        }
        return doc.getFieldValue(key);
    }

    @Override
    protected boolean isNull()
    {
        return doc == null;
    }
}
