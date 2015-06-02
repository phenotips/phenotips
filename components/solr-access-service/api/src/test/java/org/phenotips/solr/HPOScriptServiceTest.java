package org.phenotips.solr;

import org.apache.solr.client.solrj.response.QueryResponse;
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
import org.xwiki.cache.Cache;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;


public class HPOScriptServiceTest
{

    private static final String ALTERNATIVE_ID_FIELD_NAME = "alt_id";

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

    @Before
    public void setUp() throws ComponentLookupException, IOException, SolrServerException
    {
        MockitoAnnotations.initMocks(this);
        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "cache", this.cache);
        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "server", this.server);
        when(this.server.query((SolrParams)Matchers.any())).thenReturn(response);
    }

    @Test
    public void testGetUsesServer()
    {

    }

    @Test
    public void testGetUsesCache() throws ComponentLookupException, IOException, SolrServerException
    {
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put("id", "ABC1");
        String cacheKey = dumpMap(fieldValues);
        when(this.cache.get(cacheKey)).thenReturn(doc);
        SolrDocument result = this.mocker.getComponentUnderTest().get("ABC1");
        verify(this.server, never()).query((SolrParams)Matchers.any());
        Assert.assertSame(this.doc, result);
    }

    @Test
    public void testSuperGetReturnsNull() throws ComponentLookupException
    {
        when(this.cache.get(Matchers.anyString())).thenReturn(null);
        this.mocker.getComponentUnderTest().get("ABC1");

    }

    private String dumpMap(Map<String, ?> map)
    {
        StringBuilder out = new StringBuilder();
        out.append('{');
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            out.append(entry.getKey() + ':' + entry.getValue() + '\n');
        }
        out.append('}');
        return out.toString();
    }
}
