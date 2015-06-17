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
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;



import org.mockito.internal.matchers.CapturingMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *  Tests for the {@link HPOScriptService} component
 */
public class HPOScriptServiceTest
{

    @Rule
    public MockitoComponentMockingRule<ScriptService> mocker =
        new MockitoComponentMockingRule<ScriptService>(HPOScriptService.class);

    public HPOScriptService service;

    @Mock
    private SolrClient server;

    @Mock
    private Cache<SolrDocument> cache;

    @Mock
    private QueryResponse response;

    private SolrDocumentList solrDocList;

    private int indexReturn;

    private final String fieldNames = "id,name,def,comment,synonym,is_a";

    private final String testIDValue = "HP:0000118";

    @Before
    public void setUp() throws ComponentLookupException, IOException, SolrServerException, CacheException
    {
        MockitoAnnotations.initMocks(this);
        this.indexReturn = -1;

        SolrVocabularyResourceManager externalServicesAccess =
                this.mocker.getInstance(SolrVocabularyResourceManager.class);
        Mockito.doReturn(this.server).when(externalServicesAccess).getSolrConnection();

        CacheManager cacheFactory = this.mocker.getInstance(CacheManager.class);
        Mockito.doReturn(this.cache).when(cacheFactory).createNewLocalCache(any(CacheConfiguration.class));
        this.service = (HPOScriptService) this.mocker.getComponentUnderTest();
    }

    @Test
    public void indexAddsDocumentsToServerAndClearsCache() throws IOException, SolrServerException
    {
        this.indexMockSolrDocListFromResource();

        verify(this.server).add(anyCollectionOf(SolrInputDocument.class));
        verify(this.server).commit();
        verify(this.cache).removeAll();
        Assert.assertEquals(0, indexReturn);
    }

    @Test
    public void getUsesServer() throws IOException, SolrServerException
    {

        Mockito.doReturn(this.response).when(this.server).query(any(SolrParams.class));
        this.service.get(testIDValue);

        verify(this.server).query(Matchers.argThat(new IsMatchingIDQuery(testIDValue, "id")));
        verify(this.server).query(Matchers.argThat(new IsMatchingIDQuery(testIDValue, "alt_id")));

    }

    @Test
    public void getUsesCache() throws IOException, SolrServerException
    {
        SolrDocument expectedDoc = mock(SolrDocument.class);
        SolrDocument unexpectedDoc = mock(SolrDocument.class);
        String cacheKey = "{id:"+ testIDValue + "\n}";
        Mockito.doReturn(unexpectedDoc).when(this.cache).get(anyString());
        Mockito.doReturn(expectedDoc).when(this.cache).get(cacheKey);
        SolrDocument result = this.service.get(testIDValue);
        verify(this.server, never()).query((SolrParams)any());
        Assert.assertSame(expectedDoc, result);
    }

    @Test
    public void getCantFindIdReturnsNull() throws IOException, SolrServerException
    {
        Mockito.doReturn(response).when(this.server).query(any(SolrParams.class));
        Mockito.doReturn(null).when(this.cache).get(anyString());
        when(this.service.search(anyMapOf(String.class, String.class), 1, 0)).thenReturn(null);
        SolrDocument result = this.service.get(testIDValue);
        verify(this.cache, atLeast(1)).get(anyString());
        Assert.assertNull(result);
    }

    @Test
    public void getAllAncestorsAndSelfIDsReturnsAllAncestors() throws IOException, SolrServerException
    {
        this.indexMockSolrDocListFromResource();
        Mockito.doAnswer(new QueryAnswer()).when(this.server).query(any(SolrParams.class));

        Set<String> actualResult = this.service.getAllAncestorsAndSelfIDs("HP:0001507");
        Set<String> expectedResult = new HashSet<>();
        expectedResult.add("HP:0001507");
        expectedResult.add("HP:0000118");
        expectedResult.add("HP:0000001");
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void clearClearsServerAndCache() throws IOException, SolrServerException
    {
        int returnVal = this.service.clear();
        verify(this.server).deleteByQuery("*:*");
        verify(this.server).commit();
        verify(this.cache).removeAll();
        Assert.assertEquals(0, returnVal);
    }

    @Test
    public void SolrServerExceptionIsCaughtWhenIndexing() throws IOException, SolrServerException {
        Mockito.doThrow(SolrServerException.class).when(this.server).add(anyCollectionOf(SolrInputDocument.class));
        this.service.index(this.getClass().getResource("/hpo-test.obo").toString(), fieldNames);
        Mockito.reset(server);
        Mockito.doReturn(new UpdateResponse()).when(this.server).add(anyCollectionOf(SolrInputDocument.class));
        Mockito.doThrow(SolrServerException.class).when(this.server).commit();
        this.service.index(this.getClass().getResource("/hpo-test.obo").toString(), fieldNames);
    }

    @Test
    public void IOExceptionIsCaughtWhenIndexing() throws IOException, SolrServerException {
        Mockito.doThrow(IOException.class).when(this.server).add(anyCollectionOf(SolrInputDocument.class));
        this.service.index(this.getClass().getResource("/hpo-test.obo").toString(), fieldNames);
        Mockito.reset(server);
        Mockito.doReturn(new UpdateResponse()).when(this.server).add(anyCollectionOf(SolrInputDocument.class));
        Mockito.doThrow(IOException.class).when(this.server).commit();
        this.service.index(this.getClass().getResource("/hpo-test.obo").toString(), fieldNames);
    }

    @Test
    public void SolrServerExceptionIsCaughtWhenClearing() throws IOException, SolrServerException {
        Mockito.doThrow(SolrServerException.class).when(this.server).deleteByQuery(anyString());
        this.service.clear();
        Mockito.reset(server);
        Mockito.doReturn(new UpdateResponse()).when(this.server).deleteByQuery(anyString());
        Mockito.doThrow(SolrServerException.class).when(this.server).commit();
        this.service.clear();
    }


    @Test
    public void IOExceptionIsCaughtWhenClearing() throws IOException, SolrServerException {
        Mockito.doThrow(IOException.class).when(this.server).deleteByQuery(anyString());
        this.service.clear();
        Mockito.reset(server);
        Mockito.doReturn(new UpdateResponse()).when(this.server).deleteByQuery(anyString());
        Mockito.doThrow(IOException.class).when(this.server).commit();
        this.service.clear();
    }

    @Test
    public void SolrServerExceptionIsCaughtWhenQueryFails() throws IOException, SolrServerException {
        Mockito.doReturn(null).when(this.cache).get(anyString());
        Mockito.doThrow(SolrServerException.class).when(this.server).query(any(SolrParams.class));
        this.service.get(testIDValue);
    }

    @Test
    public void IOExceptionIsCaughtWhenQueryFails() throws IOException, SolrServerException {
        Mockito.doReturn(null).when(this.cache).get(anyString());
        Mockito.doThrow(IOException.class).when(this.server).query(any(SolrParams.class));
        this.service.get(testIDValue);
    }

    @Test
    public void checkReturnValueWhenDataCantBeAccessed() {
        this.indexReturn = this.service.index("", fieldNames);
        Assert.assertEquals(2, this.indexReturn);
    }

    @Test
    public void getAncestorsReturnsEmptyListWhenIDCantBeFound() throws IOException, SolrServerException {
        Mockito.doReturn(response).when(this.server).query(any(SolrParams.class));
        Mockito.doReturn(null).when(this.cache).get(anyString());
        when(this.service.search(anyMapOf(String.class, String.class), 1, 0)).thenReturn(null);
        Set<String> result = this.service.getAllAncestorsAndSelfIDs("");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    private void indexMockSolrDocListFromResource() throws IOException, SolrServerException
    {
        CapturingMatcher<Collection<SolrInputDocument>> allTermsCap = new CapturingMatcher<>();
        Mockito.doReturn(new UpdateResponse()).when(this.server).add(Matchers.argThat(allTermsCap));

        indexReturn = this.service.index(this.getClass().getResource("/hpo-test.obo").toString(), fieldNames);

        Collection<SolrInputDocument> allTerms = allTermsCap.getLastValue();
        solrDocList = new SolrDocumentList();
        for (SolrInputDocument i : allTerms) {
            solrDocList.add(ClientUtils.toSolrDocument(i));
        }
    }


    private class QueryAnswer implements Answer<QueryResponse>{

        @Override
        public QueryResponse answer(InvocationOnMock invocationOnMock) throws Throwable
        {
            SolrParams params = (SolrParams) invocationOnMock.getArguments()[0];
            if (params == null) {
                Mockito.doReturn(mock(SolrDocumentList.class)).when(response).getResults();
                return response;
            }
            for (SolrDocument item : solrDocList) {
                String fieldValue = (String)item.getFieldValue("id");
                fieldValue = fieldValue.replace(":", "\\:");
                if (params.get(CommonParams.Q).contains(fieldValue)) {
                    SolrDocumentList matchedItem = new SolrDocumentList();
                    matchedItem.add(item);
                    Mockito.doReturn(matchedItem).when(response).getResults();
                    return response;
                }
            }
            Mockito.doReturn(mock(SolrDocumentList.class)).when(response).getResults();
            return  response;
        }
    }

    private class IsMatchingIDQuery extends ArgumentMatcher<SolrParams>{

        private String id;
        private String field;

        public IsMatchingIDQuery(String id, String field){
            id = id.replace(":", "\\:");
            this.id = id;
            this.field = field;
        }

        @Override
        public boolean matches(Object argument)
        {
            if (argument == null) {
                return false;
            }
            String params = ((SolrParams) argument).get(CommonParams.Q);
            return params.startsWith(field)
                    && params.contains(id);
        }
    }
}
