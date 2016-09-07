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

import java.util.Collections;

import org.apache.solr.common.SolrDocument;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link org.phenotips.vocabulary.VocabularyTerm}.
 */
public class VocabularyTermTest
{
    @Test
    public void testDistanceToRootTerm()
    {
        Vocabulary vocabulary = mock(Vocabulary.class);

        SolrDocument rootDoc = new SolrDocument();
        rootDoc.setField("id", "T0");
        VocabularyTerm rootTerm = new SolrVocabularyTerm(rootDoc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T0"))).thenReturn(Collections.singleton(rootTerm));

        SolrDocument p1Doc = new SolrDocument();
        p1Doc.setField("id", "T1");
        p1Doc.setField("is_a", Collections.singleton("T0"));
        VocabularyTerm p1Term = new SolrVocabularyTerm(p1Doc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T1"))).thenReturn(Collections.singleton(p1Term));

        SolrDocument p2Doc = new SolrDocument();
        p2Doc.setField("id", "T2");
        p2Doc.setField("is_a", Collections.singleton("T1"));
        VocabularyTerm p2Term = new SolrVocabularyTerm(p2Doc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T2"))).thenReturn(Collections.singleton(p2Term));

        SolrDocument childDoc = new SolrDocument();
        childDoc.setField("id", "T3");
        childDoc.setField("is_a", Collections.singleton("T2"));
        VocabularyTerm childTerm = new SolrVocabularyTerm(childDoc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T3"))).thenReturn(Collections.singleton(p2Term));

        assertEquals(3, childTerm.getDistanceTo(rootTerm));
        assertEquals(3, rootTerm.getDistanceTo(childTerm));
    }
}
