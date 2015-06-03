package org.phenotips.solr;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.CapturingMatcher;
import org.xwiki.cache.Cache;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;


public class HPOScriptServiceTest
{

    private final String ID_FIELD_NAME = "id";


    @Rule
    public MockitoComponentMockingRule<HPOScriptService> mocker =
            new MockitoComponentMockingRule<HPOScriptService>(HPOScriptService.class);

    @Mock
    private Cache<SolrDocument> cache;

    @Mock
    private SolrDocument doc;

    @Mock
    private SolrClient server;

    @Mock
    private QueryResponse response;

    @Mock
    private SolrDocumentList solrDocList;

    @Mock
    private SpellCheckResponse spellCheckResponse;

    @Before
    public void setUp() throws ComponentLookupException, IOException, SolrServerException
    {
        MockitoAnnotations.initMocks(this);
        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "cache", this.cache);
        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "server", this.server);
        when(this.server.query((SolrParams)Matchers.any())).thenReturn(response);
    }

    @Test
    public void testGetUsesServer() throws ComponentLookupException, IOException, SolrServerException
    {

        CapturingMatcher<SolrParams> argCap = new CapturingMatcher<>();
        String expectedQuery = "id:HP\\:0000118";
        when(this.server.query(Matchers.argThat(argCap))).thenReturn(this.response);
        when(this.response.getResults()).thenReturn(solrDocList);
        when(this.response.getSpellCheckResponse()).thenReturn(spellCheckResponse);
        this.mocker.getComponentUnderTest().get("HP:0000118");
        List<SolrParams> capturedArgs = argCap.getAllValues();

        //TODO: Generate query here and use compare query objects, not strings
        Assert.assertEquals("q=id:HP\\:0000118&spellcheck=true&fl=*+score&start=0&rows=1&spellcheck.collate=true", capturedArgs.remove(0).toString());
        Assert.assertEquals("q=alt_id:HP\\:0000118&spellcheck=true&fl=*+score&start=0&rows=1&spellcheck.collate=true", capturedArgs.remove(0).toString());
    }

    @Test
    public void testGetUsesCache() throws ComponentLookupException, IOException, SolrServerException
    {
        String cacheKey = "{id:HP:0000118\n}";
        when(this.cache.get(cacheKey)).thenReturn(doc);
        SolrDocument result = this.mocker.getComponentUnderTest().get("HP:0000118");
        verify(this.server, never()).query((SolrParams)Matchers.any());
        Assert.assertSame(this.doc, result);
    }

    @Test
    public void testCacheReturnsEmptyMarker() throws ComponentLookupException
    {
        when(this.cache.get(Matchers.anyString())).thenReturn(null);
        when(this.mocker.getComponentUnderTest().search((Map<String, String>)Matchers.anyMap(), 1, 0)).thenReturn(null);
        SolrDocument result = this.mocker.getComponentUnderTest().get("HP:0000118");
        Assert.assertNull(result);
    }
}
