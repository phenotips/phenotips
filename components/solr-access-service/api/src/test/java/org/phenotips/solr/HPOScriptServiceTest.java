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
package org.phenotips.solr;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.junit.Assert;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.*;

import org.mockito.internal.matchers.CapturingMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.*;


public class HPOScriptServiceTest
{

    @Rule
    public MockitoComponentMockingRule<HPOScriptService> mocker =
            new MockitoComponentMockingRule<>(HPOScriptService.class);

    @Mock
    private Cache<SolrDocument> cache;

    @Mock
    private SolrClient server;

    @Mock
    private QueryResponse response;

    private SolrDocumentList solrDocList;

    private int indexReturn;

    private final String fieldNames = "id,name,def,comment,synonym,is_a";


    @Before
    public void setUp() throws ComponentLookupException, IOException, SolrServerException, CacheException
    {
        MockitoAnnotations.initMocks(this);
        this.solrDocList = null;
        this.indexReturn = -1;

        SolrVocabularyResourceManager externalServicesAccess =
                this.mocker.getInstance(SolrVocabularyResourceManager.class);
        when(externalServicesAccess.getSolrConnection()).thenReturn(this.server);

        CacheManager cacheFactory = this.mocker.getInstance(CacheManager.class);
        when(cacheFactory.createNewLocalCache((CacheConfiguration) Matchers.any())).thenReturn((Cache) this.cache);

    }

    @Test
    public void indexAddsDocumentsToServerAndClearsCache() throws IOException, SolrServerException, ComponentLookupException
    {
        this.indexMockSolrDocListFromResource();

        verify(this.server).add(anyCollectionOf(SolrInputDocument.class));
        verify(this.server).commit();
        verify(this.cache).removeAll();
        Assert.assertEquals(0, indexReturn);
    }

    @Test
    public void getUsesServer() throws ComponentLookupException, IOException, SolrServerException
    {
        String id = "HP:0000118";

        when(this.server.query((SolrParams)any())).thenReturn(this.response);
        this.mocker.getComponentUnderTest().get(id);

        verify(this.server).query(Matchers.argThat(new IsMatchingIDQuery(id)));
        verify(this.server).query(Matchers.argThat(new IsMatchingAltIDQuery(id)));

    }

    @Test
    public void getUsesCache() throws ComponentLookupException, IOException, SolrServerException
    {
        SolrDocument expectedDoc = mock(SolrDocument.class);
        SolrDocument unexpectedDoc = mock(SolrDocument.class);
        String cacheKey = "{id:HP:0000118\n}";
        when(this.cache.get(anyString())).thenReturn(unexpectedDoc);
        when(this.cache.get(cacheKey)).thenReturn(expectedDoc);
        SolrDocument result = this.mocker.getComponentUnderTest().get("HP:0000118");
        verify(this.server, never()).query((SolrParams)any());
        Assert.assertSame(expectedDoc, result);
    }

    @Test
    public void getCantFindIdReturnsNull() throws ComponentLookupException, IOException, SolrServerException
    {
        when(this.server.query((SolrParams)any())).thenReturn(response);
        when(this.cache.get(anyString())).thenReturn(null);
        when(this.mocker.getComponentUnderTest().search(anyMap(), 1, 0)).thenReturn(null);
        SolrDocument result = this.mocker.getComponentUnderTest().get("HP:0000118");
        verify(this.cache, atLeast(1)).get(anyString());
        Assert.assertNull(result);
    }

    @Test
    public void getAllAncestorsAndSelfIDsReturnsAllAncestors() throws ComponentLookupException, IOException, SolrServerException
    {
        this.indexMockSolrDocListFromResource();
        when(this.server.query((SolrParams)any())).thenAnswer(new QueryAnswer());

        Set<String> actualResult = this.mocker.getComponentUnderTest().getAllAncestorsAndSelfIDs("HP:0001507");
        Set<String> expectedResult = new HashSet<>();
        expectedResult.add("HP:0001507");
        expectedResult.add("HP:0000118");
        expectedResult.add("HP:0000001");
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void clearClearsServerAndCache() throws ComponentLookupException, IOException, SolrServerException
    {
        int returnVal = this.mocker.getComponentUnderTest().clear();
        verify(this.server).deleteByQuery("*:*");
        verify(this.server).commit();
        verify(this.cache).removeAll();
        Assert.assertEquals(0, returnVal);
    }

    private void indexMockSolrDocListFromResource() throws IOException, SolrServerException, ComponentLookupException
    {
        CapturingMatcher<Collection<SolrInputDocument>> allTermsCap = new CapturingMatcher<>();
        when(this.server.add(Matchers.argThat(allTermsCap))).thenReturn(new UpdateResponse());

        indexReturn = this.mocker.getComponentUnderTest().index(this.getClass().getResource("/hpo-test.obo").toString(), fieldNames);

        Collection<SolrInputDocument> allTerms = allTermsCap.getLastValue();
        solrDocList = new SolrDocumentList();
        for(SolrInputDocument i : allTerms){
            solrDocList.add(ClientUtils.toSolrDocument(i));
        }
    }


    private class QueryAnswer implements Answer<QueryResponse>
    {

        @Override
        public QueryResponse answer(InvocationOnMock invocationOnMock)
        {
            SolrParams params = (SolrParams) invocationOnMock.getArguments()[0];
            if (params == null) {
                when(response.getResults()).thenReturn(mock(SolrDocumentList.class));
                return response;
            }
            for (SolrDocument item : solrDocList) {
                String fieldValue = (String)item.getFieldValue("id");
                fieldValue = fieldValue.replace(":", "\\:");
                if (params.get(CommonParams.Q).contains(fieldValue)) {
                    SolrDocumentList matchedItem = new SolrDocumentList();
                    matchedItem.add(item);
                    when(response.getResults()).thenReturn(matchedItem);
                    return response;
                }
            }
            when(response.getResults()).thenReturn(mock(SolrDocumentList.class));
            return  response;
        }
    }

    private class IsMatchingIDQuery extends ArgumentMatcher<SolrParams>
    {

        private String id;

        public IsMatchingIDQuery(String id){
            id = id.replace(":", "\\:");
            this.id = id;
        }

        @Override
        public boolean matches(Object argument)
        {
            if (argument == null) {
                return false;
            }
            SolrParams params = (SolrParams)argument;
            return params.get(CommonParams.Q).startsWith("id")
                    && params.get(CommonParams.Q).contains(id);
        }
    }

    private class IsMatchingAltIDQuery extends ArgumentMatcher<SolrParams>
    {

        private String id;

        public IsMatchingAltIDQuery(String id){
            id = id.replace(":", "\\:");
            this.id = id;
        }

        @Override
        public boolean matches(Object argument)
        {
            if (argument == null) {
                return false;
            }
            SolrParams params = (SolrParams)argument;
            return params.get(CommonParams.Q).startsWith("alt_id")
                    && params.get(CommonParams.Q).contains(id);
        }
    }
}
