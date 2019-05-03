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

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/**
 * Tests for the ChEBI implementation of the {@link org.phenotips.vocabulary.Vocabulary},
 * {@link org.phenotips.vocabulary.internal.solr.ChEBIOntology}.
 */
public class ChEBIOntologyTest
{
    @Rule
    public final MockitoComponentMockingRule<Vocabulary> mocker =
        new MockitoComponentMockingRule<>(ChEBIOntology.class);

    private int ontologyServiceResult;

    private Cache<VocabularyTerm> cache;

    private SolrClient server;

    private Vocabulary vocabulary;

    @SuppressWarnings("unchecked")
    @Before
    public void setUpOntology()
        throws ComponentLookupException, IOException, SolrServerException, CacheException
    {
        this.cache = mock(Cache.class);
        this.vocabulary = this.mocker.getComponentUnderTest();
        SolrVocabularyResourceManager externalServicesAccess =
            this.mocker.getInstance(SolrVocabularyResourceManager.class);
        when(externalServicesAccess.getTermCache(this.vocabulary)).thenReturn(this.cache);
        this.server = mock(SolrClient.class);
        when(externalServicesAccess.getReplacementSolrConnection(this.vocabulary)).thenReturn(this.server);
        when(externalServicesAccess.getSolrConnection(this.vocabulary)).thenReturn(this.server);
        this.ontologyServiceResult =
            this.vocabulary.reindex(this.getClass().getResource("/chebi-test.obo").toString());
    }

    @Test
    public void testChEBIOntologyReindex()
        throws ComponentLookupException, IOException, SolrServerException, CacheException
    {
        Mockito.verify(this.server, Mockito.atLeast(1)).commit();
        Mockito.verify(this.server, Mockito.atLeast(1)).add(Matchers.anyCollectionOf(SolrInputDocument.class));
        Mockito.verify(this.cache, Mockito.atLeast(1)).removeAll();
        Mockito.verifyNoMoreInteractions(this.cache);
        Assert.assertTrue(this.ontologyServiceResult == 0);
    }

    @Test
    public void testChEBIOntologyVersion() throws SolrServerException, IOException
    {
        QueryResponse response = mock(QueryResponse.class);
        when(this.server.query(any(SolrQuery.class))).thenReturn(response);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(response.getResults()).thenReturn(results);
        when(results.isEmpty()).thenReturn(false);
        SolrDocument versionDoc = mock(SolrDocument.class);
        when(results.get(0)).thenReturn(versionDoc);
        when(versionDoc.getFieldValue("version")).thenReturn("2014:01:01");
        Assert.assertEquals("2014:01:01", this.vocabulary.getVersion());
    }

    @Test
    public void testChEBIOntologyDefaultLocation()
    {
        String location = this.vocabulary.getDefaultSourceLocation();
        Assert.assertNotNull(location);
        Assert.assertTrue(location.endsWith("chebi.obo"));
        Assert.assertTrue(location.startsWith("ftp"));
    }
}
