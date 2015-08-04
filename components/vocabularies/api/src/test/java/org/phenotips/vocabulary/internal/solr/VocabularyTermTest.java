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

import org.apache.solr.common.SolrDocument;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;
import org.xwiki.cache.Cache;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests of {@link org.phenotips.vocabulary.VocabularyTerm} methods.
 */
public class VocabularyTermTest
{
    @Test
    public void testDistanceToRootTerm()
    {
        Vocabulary vocabulary = mock(Vocabulary.class);

        SolrDocument rootDoc = mock(SolrDocument.class);
        VocabularyTerm rootTermReal = new SolrVocabularyTerm(rootDoc, vocabulary);
        VocabularyTerm rootTerm = spy(rootTermReal);
        doReturn("T0").when(rootTerm).getId();

        Set<VocabularyTerm> p1Parents = new HashSet<>();
        p1Parents.add(rootTerm);
        SolrDocument p1Doc = mock(SolrDocument.class);
        VocabularyTerm p1TermReal = new SolrVocabularyTerm(p1Doc, vocabulary);
        VocabularyTerm p1Term = spy(p1TermReal);
        doReturn("T1").when(p1Term).getId();
        doReturn(p1Parents).when(p1Term).getParents();

        Set<VocabularyTerm> p2Parents = new HashSet<>();
        p2Parents.add(p1Term);
        SolrDocument p2Doc = mock(SolrDocument.class);
        VocabularyTerm p2TermReal = new SolrVocabularyTerm(p2Doc, vocabulary);
        VocabularyTerm p2Term = spy(p2TermReal);
        doReturn("T2").when(p2Term).getId();
        doReturn(p2Parents).when(p2Term).getParents();

        SolrDocument childDoc = mock(SolrDocument.class);
        Set<VocabularyTerm> childParents = new HashSet<>();
        childParents.add(p2Term);
        VocabularyTerm childTermReal = new SolrVocabularyTerm(childDoc, vocabulary);
        VocabularyTerm childTerm = spy(childTermReal);
        doReturn("T3").when(childTerm).getId();
        doReturn(childParents).when(childTerm).getParents();

        assertEquals(3, childTerm.getDistanceTo(rootTerm));
        assertEquals(3, rootTerm.getDistanceTo(childTerm));
    }
}
