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
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Set;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

/**
 * Implementation for {@link VocabularyInputTerm} based on a Solr document.
 *
 * @version $Id$
 */
public class SolrVocabularyInputTerm extends AbstractSolrVocabularyTerm implements VocabularyInputTerm
{
    /**
     * Constructor.
     *
     * @param doc the solr document representing the term
     * @param vocabulary the owner vocabulary
     */
    public SolrVocabularyInputTerm(SolrInputDocument doc, Vocabulary vocabulary)
    {
        super(doc, vocabulary);
        initialize();
    }

    @Override
    public Object get(String key)
    {
        // We have to override this because in Solr 5.5 SolrInputDocument#get wrongly forwards to getFirstValue
        if (isNull()) {
            return null;
        }
        SolrInputField field = ((SolrInputDocument) this.doc).getField(key);
        if (field == null) {
            return null;
        }
        return field.getValue();
    }

    @Override
    public VocabularyInputTerm setId(String id)
    {
        if (this.doc != null) {
            this.doc.setField(ID_KEY, id);
        }
        return this;
    }

    @Override
    public VocabularyInputTerm setName(String name)
    {
        if (this.doc != null) {
            this.doc.setField(NAME, name);
        }
        return this;
    }

    @Override
    public VocabularyInputTerm setDescription(String description)
    {
        if (this.doc != null) {
            this.doc.setField(DESCRIPTION, description);
        }
        return this;
    }

    @Override
    public VocabularyInputTerm setParents(Set<VocabularyTerm> parents)
    {
        if (this.doc != null) {
            SolrInputField field = new SolrInputField(PARENTS_KEY);
            for (VocabularyTerm parent : parents) {
                field.addValue(parent.getId(), 1.0f);
            }
            ((SolrInputDocument) this.doc).put(PARENTS_KEY, field);
        }
        return this;
    }

    @Override
    public VocabularyInputTerm set(String key, Object value)
    {
        if (this.doc != null) {
            this.doc.setField(key, value);
        }
        return this;
    }

    @Override
    public VocabularyInputTerm append(String key, Object value)
    {
        if (this.doc != null) {
            this.doc.addField(key, value);
        }
        return this;
    }

    @Override
    public Set<VocabularyTerm> getParents()
    {
        // We have to override this because the parents might be re-set, so the result can't be cached
        return new LazySolrTermSet(getValues(PARENTS_KEY), this.vocabulary);
    }

    @Override
    public Set<VocabularyTerm> getAncestors()
    {
        // We have to override this because the ancestors might be re-set, so the result can't be cached
        return new LazySolrTermSet(getValues(ANCESTORS_KEY), this.vocabulary);
    }

    @Override
    public Set<VocabularyTerm> getAncestorsAndSelf()
    {
        // We have to override this because the ancestors might be re-set, so the result can't be cached
        return getUncachedAncestorsAndSelf();
    }
}
