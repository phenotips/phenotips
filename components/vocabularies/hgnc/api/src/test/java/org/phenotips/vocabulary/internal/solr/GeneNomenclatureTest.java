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

import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GeneNomenclatureTest
{
    private static final String IDENTIFIER = "hgnc";

    private static final String NAME = "HUGO Gene Nomenclature Committee's GeneNames (HGNC)";

    private static final String WEBSITE = "http://www.genenames.org/";

    private static final String DEFAULT_SOURCE = "ftp://ftp.ebi.ac.uk/pub/databases/genenames/new/tsv/"
        + "hgnc_complete_set.txt";

    private static final String CITATION = "HGNC Database, HUGO Gene Nomenclature Committee (HGNC), EMBL Outstation - "
        + "Hinxton, European Bioinformatics Institute, Wellcome Trust Genome Campus, Hinxton, Cambridgeshire, CB10 "
        + "1SD, UK";

    private static final String TERM_PREFIX = "HGNC";

    private static final String QUERY_STRING = "symbol:term1^50 alt_id:term1 id:HGNC\\:term1";

    private static final String TERM_ID = "term1";

    private static final String TERM_NAME = "term1_name";

    private static final String SEPARATOR = ":";

    private static final String ID_LABEL = "id";

    private static final String NAME_LABEL = "name";

    private static final int NUM_DOCS = 500000;

    @Rule
    public final MockitoComponentMockingRule<Vocabulary> mocker =
        new MockitoComponentMockingRule<>(GeneNomenclature.class);

    @Mock
    private SolrClient solrClient;

    @Mock
    private QueryResponse response;

    @Mock
    private SolrDocumentList termList;

    @Mock
    private SolrDocument termDoc;

    @Mock
    private VocabularyTerm term;

    private GeneNomenclature component;

    private GeneNomenclature componentSpy;

    @Before
    public void setUp() throws ComponentLookupException, IOException, SolrServerException
    {
        MockitoAnnotations.initMocks(this);

        this.component = (GeneNomenclature) this.mocker.getComponentUnderTest();
        this.componentSpy = spy(this.component);

        final SolrVocabularyResourceManager externalServicesAccess =
            this.mocker.getInstance(SolrVocabularyResourceManager.class);
        when(externalServicesAccess.getSolrConnection(this.component)).thenReturn(this.solrClient);

        when(this.solrClient.query(any(SolrQuery.class))).thenReturn(this.response);
        when(this.response.getResults()).thenReturn(this.termList);
        when(this.termList.get(0)).thenReturn(this.termDoc);
        when(this.termDoc.getFieldValues(ID_LABEL)).thenReturn(Collections.singletonList(TERM_ID));
        when(this.termDoc.getFieldValues(NAME_LABEL)).thenReturn(Collections.singletonList(TERM_NAME));

        doReturn(this.term).when(this.componentSpy).requestTerm(anyString(), anyString());
    }

    @Test
    public void getDefaultSourceLocationReturnsURLToData()
    {
        final String defaultSource = this.component.getDefaultSourceLocation();
        Assert.assertEquals(DEFAULT_SOURCE, defaultSource);
    }

    @Test
    public void getSolrDocsPerBatchReturnsCorrectNumberOfDocuments()
    {
        final int numDocs = this.component.getSolrDocsPerBatch();
        Assert.assertEquals(NUM_DOCS, numDocs);
    }

    @Test
    public void getIdentifierWorksAsExpected()
    {
        final String identifier = this.component.getIdentifier();
        Assert.assertEquals(IDENTIFIER, identifier);
    }

    @Test
    public void getNameReturnsNameOfVocabulary()
    {
        final String vocabName = this.component.getName();
        Assert.assertEquals(NAME, vocabName);
    }

    @Test
    public void getAliasesReturnsVocabularyAliases()
    {
        final Set<String> expected = new HashSet<>();
        expected.add(IDENTIFIER);
        expected.add(TERM_PREFIX);
        final Set<String> aliases = this.component.getAliases();
        Assert.assertEquals(expected, aliases);

    }

    @Test
    public void getWebsiteReturnsOntologyWebsite()
    {
        final String website = this.component.getWebsite();
        Assert.assertEquals(WEBSITE, website);
    }

    @Test
    public void getCitationReturnsCitationForData()
    {
        final String citation = this.component.getCitation();
        Assert.assertEquals(CITATION, citation);
    }

    @Test
    public void getTermReturnsNullWhenIdIsNull()
    {
        Assert.assertNull(this.component.getTerm(null));
    }

    @Test
    public void getTermReturnsNullWhenIdIsEmptyString()
    {
        Assert.assertNull(this.component.getTerm(StringUtils.EMPTY));
    }

    @Test
    public void getTermReturnsNullWhenIdIsEmptySpace()
    {
        Assert.assertNull(this.component.getTerm(StringUtils.SPACE));
    }

    @Test
    public void getTermReturnsNullIfNothingFound()
    {
        when(this.response.getResults()).thenReturn(null);
        Assert.assertNull(this.component.getTerm(TERM_ID));
    }

    @Test
    public void getTermRemovesHGNCPrefixFromProvidedIds()
    {
        Assert.assertEquals(this.term, this.componentSpy.getTerm(TERM_PREFIX + SEPARATOR + TERM_ID));
        verify(this.componentSpy, times(1)).requestTerm(QUERY_STRING, null);
    }

    @Test
    public void getTermDoesNotChangeIdWhenItHasNoPrefix()
    {
        Assert.assertEquals(this.term, this.componentSpy.getTerm(TERM_ID));
        verify(this.componentSpy, times(1)).requestTerm(QUERY_STRING, null);
    }

    @Test
    public void requestTermReturnsNullIfResultsAreNull()
    {
        when(this.response.getResults()).thenReturn(null);
        Assert.assertNull(this.component.requestTerm(QUERY_STRING, null));
    }

    @Test
    public void requestTermReturnsNullIfResultsAreEmpty()
    {
        when(this.termList.isEmpty()).thenReturn(true);
        Assert.assertNull(this.component.requestTerm(QUERY_STRING, null));
    }

    @Test
    public void requestTermBehavesAsExpected()
    {
        final VocabularyTerm result = this.component.requestTerm(QUERY_STRING, null);
        Assert.assertEquals(TERM_ID, result.getId());
        Assert.assertEquals(TERM_NAME, result.getName());
    }
}
