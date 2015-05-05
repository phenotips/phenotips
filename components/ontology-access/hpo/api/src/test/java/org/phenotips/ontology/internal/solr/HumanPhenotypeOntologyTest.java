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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.ontology.internal.solr;

import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;
import org.phenotips.ontology.SolrOntologyServiceInitializer;

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
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the HPO implementation of the {@link org.phenotips.ontology.OntologyService},
 * {@link org.phenotips.ontology.internal.solr.HumanPhenotypeOntology}.
 */
public class HumanPhenotypeOntologyTest
{
    public int ontologyServiceResult;

    public Cache<OntologyTerm> cache;

    public SolrClient server;

    public OntologyService ontologyService;

    @Rule
    public final MockitoComponentMockingRule<OntologyService> mocker =
        new MockitoComponentMockingRule<OntologyService>(HumanPhenotypeOntology.class);

    @SuppressWarnings("unchecked")
    @Before
    public void setUpOntology()
        throws ComponentLookupException, IOException, SolrServerException, CacheException
    {
        this.cache = mock(Cache.class);
        SolrOntologyServiceInitializer externalServicesAccess =
            this.mocker.getInstance(SolrOntologyServiceInitializer.class);
        when(externalServicesAccess.getCache()).thenReturn(this.cache);
        this.server = mock(SolrClient.class);
        when(externalServicesAccess.getServer()).thenReturn(this.server);
        this.ontologyService = this.mocker.getComponentUnderTest();
        this.ontologyServiceResult =
            this.ontologyService.reindex(this.getClass().getResource("/hpo-test.obo").toString());
    }

    @Test
    public void testHumanPhenotypeOntologyReindex()
        throws ComponentLookupException, IOException, SolrServerException, CacheException
    {
        Mockito.verify(this.server).deleteByQuery("*:*");
        Mockito.verify(this.server).commit();
        Mockito.verify(this.server).add(Matchers.anyCollectionOf(SolrInputDocument.class));
        Mockito.verify(this.cache).removeAll();
        Mockito.verifyNoMoreInteractions(this.cache, this.server);
        Assert.assertTrue(this.ontologyServiceResult == 0);
    }

    @Test
    public void testHumanPhenotypeOntologyVersion() throws SolrServerException, IOException
    {
        QueryResponse response = mock(QueryResponse.class);
        when(this.server.query(any(SolrQuery.class))).thenReturn(response);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(response.getResults()).thenReturn(results);
        when(results.isEmpty()).thenReturn(false);
        SolrDocument versionDoc = mock(SolrDocument.class);
        when(results.get(0)).thenReturn(versionDoc);
        when(versionDoc.getFieldValue("version")).thenReturn("2014:01:01");
        Assert.assertEquals("2014:01:01", this.ontologyService.getVersion());
    }

    @Test
    public void testHumanPhenotypeOntologyDefaultLocation()
    {
        String location = this.ontologyService.getDefaultOntologyLocation();
        Assert.assertNotNull(location);
        Assert.assertTrue(location.endsWith("hp.obo"));
        Assert.assertTrue(location.startsWith("http"));
    }

    @Test
    public void testHumanPhenotypeOntologySuggestTermsBlank() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().termSuggest("", 0, null, null).isEmpty());
    }

    @Test
    public void testHumanPhenotypeOntologySuggestTermsIsId() throws ComponentLookupException, SolrServerException,
        IOException
    {
        QueryResponse response = mock(QueryResponse.class);
        when(this.server.query(any(SolrParams.class))).thenReturn(response);
        when(response.getSpellCheckResponse()).thenReturn(null);
        when(response.getResults()).thenReturn(new SolrDocumentList());

        this.mocker.getComponentUnderTest().termSuggest("HP:0001", 0, null, null);

        verify(this.server).query(argThat(new IsIdQuery()));
    }

    @Test
    public void testHumanPhenotypeOntologySuggestTermsIsNotId() throws ComponentLookupException, SolrServerException,
        IOException
    {
        QueryResponse response = mock(QueryResponse.class);
        when(this.server.query(any(SolrParams.class))).thenReturn(response);
        when(response.getSpellCheckResponse()).thenReturn(null);
        when(response.getResults()).thenReturn(new SolrDocumentList());

        this.mocker.getComponentUnderTest().termSuggest("HP:Test", 0, null, null);

        verify(this.server).query(argThat(new IsDisMaxQuery()));
    }

    @Test
    public void testHumanPhenotypeOntologySuggestTermsMultipleWords() throws ComponentLookupException,
        SolrServerException, IOException
    {
        QueryResponse response = mock(QueryResponse.class);
        when(this.server.query(any(SolrParams.class))).thenReturn(response);
        when(response.getSpellCheckResponse()).thenReturn(null);
        when(response.getResults()).thenReturn(new SolrDocumentList());

        this.mocker.getComponentUnderTest().termSuggest("first second", 0, null, null);

        verify(this.server).query(argThat(new IsDisMaxQuery()));
    }

    class IsDisMaxQuery extends ArgumentMatcher<SolrParams>
    {
        @Override
        public boolean matches(Object argument)
        {
            SolrParams params = (SolrParams) argument;
            return params.get(DisMaxParams.PF) != null
                && params.get(DisMaxParams.QF) != null
                && params.get(CommonParams.Q) != null;
        }
    }

    class LastWord extends ArgumentMatcher<SolrParams>
    {
        @Override
        public boolean matches(Object argument)
        {
            SolrParams params = (SolrParams) argument;
            return params.get(CommonParams.Q).endsWith("second*");
        }
    }

    class IsIdQuery extends ArgumentMatcher<SolrParams>
    {
        @Override
        public boolean matches(Object argument)
        {
            SolrParams params = (SolrParams) argument;
            return params.get(CommonParams.FQ).startsWith("id")
                && params.get(DisMaxParams.PF) == null
                && params.get(DisMaxParams.QF) == null;
        }
    }
}
