/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.ontology.internal;

import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.CapturingMatcher;

import net.sf.json.JSONArray;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link GeneNomenclature} component.
 *
 * @version $Id$
 */
public class GeneNomenclatureTest
{
    @Rule
    public MockitoComponentMockingRule<OntologyService> mocker =
        new MockitoComponentMockingRule<OntologyService>(GeneNomenclature.class);
    
    private ConfigurationSource configuration;

    @Mock
    private CloseableHttpClient client;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private HttpEntity responseEntity;

    @Mock
    private Cache<OntologyTerm> cache;

    @Mock
    private OntologyTerm term;

    private OntologyTerm emptyMarker;

    @Before
    public void setUp() throws ComponentLookupException, CacheException, NoSuchFieldException,
        IllegalArgumentException, IllegalAccessException
    {
        MockitoAnnotations.initMocks(this);
        when(this.mocker.<CacheManager>getInstance(CacheManager.class).<OntologyTerm>createNewLocalCache(
            any(CacheConfiguration.class))).thenReturn(this.cache);
        this.configuration = this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        when(this.configuration.getProperty("phenotips.ontologies.hgnc.serviceURL", "http://rest.genenames.org/")).thenReturn("http://rest.genenames.org/");
        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "client", this.client);
        Field em = ReflectionUtils.getField(GeneNomenclature.class, "EMPTY_MARKER");
        em.setAccessible(true);
        this.emptyMarker = (OntologyTerm) em.get(null);
    }

    @Test
    public void checkURLConfigurable() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException, InitializationException
    {
        when(this.configuration.getProperty("phenotips.ontologies.hgnc.serviceURL", "http://rest.genenames.org/"))
            .thenReturn("https://proxy/genenames/");
        URI expectedURI = new URI("https://proxy/genenames/fetch/symbol/BRCA1");
        CapturingMatcher<HttpUriRequest> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("BRCA1.json"));
        // Since the component was already initialized in setUp() with the default URL, re-initialize it
        // with the new configuration mock
        ((Initializable) this.mocker.getComponentUnderTest()).initialize();
        OntologyTerm result = this.mocker.getComponentUnderTest().getTerm("BRCA1");
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        Assert.assertEquals("application/json", reqCapture.getLastValue().getLastHeader("Accept").getValue());
        Assert.assertNotNull(result);
        Assert.assertEquals("BRCA1", result.get("symbol"));
        Assert.assertEquals("breast cancer 1, early onset", result.getName());
        JSONArray aliases = (JSONArray) result.get("alias_symbol");
        Assert.assertArrayEquals(new String[] { "RNF53", "BRCC1", "PPP1R53" }, aliases.toArray());
        verify(this.cache).set("BRCA1", result);
    }

    @Test
    public void getTermFetchesFromRemoteServer() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        URI expectedURI = new URI("http://rest.genenames.org/fetch/symbol/BRCA1");
        CapturingMatcher<HttpUriRequest> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("BRCA1.json"));
        OntologyTerm result = this.mocker.getComponentUnderTest().getTerm("BRCA1");
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        Assert.assertEquals("application/json", reqCapture.getLastValue().getLastHeader("Accept").getValue());
        Assert.assertNotNull(result);
        Assert.assertEquals("BRCA1", result.get("symbol"));
        Assert.assertEquals("breast cancer 1, early onset", result.getName());
        JSONArray aliases = (JSONArray) result.get("alias_symbol");
        Assert.assertArrayEquals(new String[] { "RNF53", "BRCC1", "PPP1R53" }, aliases.toArray());
        verify(this.cache).set("BRCA1", result);
    }

    @Test
    public void getTermUsesCache() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        when(this.cache.get("BRCA1")).thenReturn(this.term);
        OntologyTerm result = this.mocker.getComponentUnderTest().getTerm("BRCA1");
        verify(this.client, never()).execute(any(HttpUriRequest.class));
        Assert.assertSame(this.term, result);
    }

    @Test
    public void getTermWithInvalidTermReturnsNull() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        URI expectedURI = new URI("http://rest.genenames.org/fetch/symbol/NOTHING");
        CapturingMatcher<HttpUriRequest> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("NOTHING.json"));
        OntologyTerm result = this.mocker.getComponentUnderTest().getTerm("NOTHING");
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        Assert.assertEquals("application/json", reqCapture.getLastValue().getLastHeader("Accept").getValue());
        Assert.assertNull(result);
        verify(this.cache).set("NOTHING", this.emptyMarker);
    }

    @Test
    public void getTermWithEmptyMarkerInCacheReturnsNull() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        when(this.cache.get("NOTHING")).thenReturn(this.emptyMarker);
        OntologyTerm result = this.mocker.getComponentUnderTest().getTerm("NOTHING");
        verify(this.client, never()).execute(any(HttpUriRequest.class));
        Assert.assertNull(result);
    }

    @Test
    public void getTermWithExceptionReturnsNull() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        when(this.client.execute(any(HttpUriRequest.class))).thenThrow(new IOException());
        OntologyTerm result = this.mocker.getComponentUnderTest().getTerm("ERROR");
        Assert.assertNull(result);
    }

    @Test
    public void getTermsFetchesFromRemoteServer() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        URI expectedURI1 = new URI("http://rest.genenames.org/fetch/symbol/BRCA1");
        URI expectedURI2 = new URI("http://rest.genenames.org/fetch/symbol/NOTHING");
        CapturingMatcher<HttpUriRequest> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("BRCA1.json"),
            ClassLoader.getSystemResourceAsStream("NOTHING.json"));
        Set<OntologyTerm> result = this.mocker.getComponentUnderTest().getTerms(Arrays.asList("BRCA1", "NOTHING"));
        List<HttpUriRequest> calledURIs = reqCapture.getAllValues();
        Assert.assertEquals(expectedURI1, calledURIs.get(0).getURI());
        Assert.assertEquals(expectedURI2, calledURIs.get(1).getURI());
        Assert.assertEquals("application/json", reqCapture.getLastValue().getLastHeader("Accept").getValue());
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("BRCA1", result.iterator().next().getId());
    }

    @Test
    public void getStringDistanceIsFlat() throws ComponentLookupException
    {
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance("A", "B"));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance("A", "A"));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance("A", null));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance(null, "B"));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance((String) null, null));
    }

    @Test
    public void getTermDistanceIsFlat() throws ComponentLookupException
    {
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance(this.term, this.term));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance(this.term, mock(OntologyTerm.class)));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance(this.term, null));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance(null, this.term));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance((OntologyTerm) null, null));
    }

    @Test
    public void getSizeFetchesFromRemoteServer() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        URI expectedURI = new URI("http://rest.genenames.org/info");
        CapturingMatcher<HttpUriRequest> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("info.json"));
        long result = this.mocker.getComponentUnderTest().size();
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        Assert.assertEquals("application/json", reqCapture.getLastValue().getLastHeader("Accept").getValue());
        Assert.assertEquals(40045, result);
    }

    @Test
    public void getSizeWithErrorReturnsNegative1() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        when(this.client.execute(any(HttpUriRequest.class))).thenThrow(new IOException());
        long result = this.mocker.getComponentUnderTest().size();
        Assert.assertEquals(-1, result);
    }

    @Test
    public void getVersionFetchesFromRemoteServer() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        URI expectedURI = new URI("http://rest.genenames.org/info");
        CapturingMatcher<HttpUriRequest> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("info.json"));
        String result = this.mocker.getComponentUnderTest().getVersion();
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        Assert.assertEquals("application/json", reqCapture.getLastValue().getLastHeader("Accept").getValue());
        Assert.assertEquals("2014-09-01T04:42:14.649Z", result);
    }

    @Test
    public void getVersionWithErrorReturnsEmptyString() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        when(this.client.execute(any(HttpUriRequest.class))).thenThrow(new IOException());
        String result = this.mocker.getComponentUnderTest().getVersion();
        Assert.assertEquals("", result);
    }

    @Test
    public void reindexInvalidatesCache() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().reindex(null));
        Mockito.verify(this.cache).removeAll();
        Mockito.verifyNoMoreInteractions(this.client);
    }

    @Test
    public void checkReturnedTermsBehavior() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        when(this.client.execute(any(HttpUriRequest.class))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("BRCA1.json"));
        OntologyTerm result = this.mocker.getComponentUnderTest().getTerm("BRCA1");
        Assert.assertEquals("BRCA1", result.get("symbol"));
        Assert.assertEquals("breast cancer 1, early onset", result.getName());
        Assert.assertEquals("", result.getDescription());
        Assert.assertEquals(-1, result.getDistanceTo(null));
        Assert.assertEquals(-1, result.getDistanceTo(result));
        Assert.assertEquals(-1, result.getDistanceTo(mock(OntologyTerm.class)));
        Assert.assertEquals(this.mocker.getComponentUnderTest(), result.getOntology());
        Assert.assertTrue(result.getParents().isEmpty());
        Assert.assertTrue(result.getAncestors().isEmpty());
        Assert.assertEquals(1, result.getAncestorsAndSelf().size());
        Assert.assertTrue(result.getAncestorsAndSelf().contains(result));
        Assert.assertEquals("BRCA1", result.getId());
        Assert.assertEquals("HGNC:BRCA1", result.toString());
    }

    @Test
    public void searchFetchesFromRemoteServer() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        URI expectedURI = new URI("http://rest.genenames.org/search/"
            + "+status%3A%28Approved%29+AND+%28+symbol%3A%28brcA*%29+alias_symbol%3A%28brcA*%29%29");
        CapturingMatcher<HttpUriRequest> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("brca.json"));
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("status", "Approved");
        Map<String, String> subquery = new LinkedHashMap<>();
        subquery.put("symbol", "brcA*");
        subquery.put("alias_symbol", "brcA*");
        search.put("AND", subquery);
        Map<String, String> queryOptions = new LinkedHashMap<>();
        queryOptions.put("start", "3");
        queryOptions.put("rows", "2");
        Set<OntologyTerm> result = this.mocker.getComponentUnderTest().search(search, queryOptions);
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        Assert.assertEquals("application/json", reqCapture.getLastValue().getLastHeader("Accept").getValue());
        Assert.assertEquals(2, result.size());
        Iterator<OntologyTerm> terms = result.iterator();
        Assert.assertEquals("BRCA1", terms.next().getId());
        Assert.assertEquals("BRCA1P1", terms.next().getId());
    }

    @Test
    public void searchWithErrorReturnsEmptySet() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        when(this.client.execute(any(HttpUriRequest.class))).thenThrow(new IOException());
        Set<OntologyTerm> result = this.mocker.getComponentUnderTest().search(new HashMap<String, Object>());
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void searchIgnoresBadOptions() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        when(this.client.execute(any(HttpUriRequest.class))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("brca.json"));
        this.responseEntity.getContent().mark(5000);
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("status", "Approved");
        Map<String, String> queryOptions = new LinkedHashMap<>();

        queryOptions.put("start", "three");
        queryOptions.put("rows", "");
        Assert.assertEquals(6, this.mocker.getComponentUnderTest().search(search, queryOptions).size());
        this.responseEntity.getContent().reset();

        queryOptions.put("start", "2");
        queryOptions.put("rows", "100");
        Assert.assertEquals(4, this.mocker.getComponentUnderTest().search(search, queryOptions).size());
        this.responseEntity.getContent().reset();

        queryOptions.put("start", "-2");
        queryOptions.put("rows", "-3");
        Assert.assertEquals(6, this.mocker.getComponentUnderTest().search(search, queryOptions).size());
    }

    @Test
    public void countFetchesFromRemoteServer() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        URI expectedURI = new URI("http://rest.genenames.org/search/"
            + "+status%3A%28Approved%29+AND+%28+symbol%3A%28brcA*%29+alias_symbol%3A%28brcA*%29%29");
        CapturingMatcher<HttpUriRequest> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("brca.json"));
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("status", "Approved");
        Map<String, String> subquery = new LinkedHashMap<>();
        subquery.put("symbol", "brcA*");
        subquery.put("alias_symbol", "brcA*");
        search.put("AND", subquery);
        long result = this.mocker.getComponentUnderTest().count(search);
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        Assert.assertEquals("application/json", reqCapture.getLastValue().getLastHeader("Accept").getValue());
        Assert.assertEquals(6, result);
    }

    @Test
    public void countWithExceptionReturnsZero() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        when(this.client.execute(any(HttpUriRequest.class))).thenThrow(new IOException());
        long result = this.mocker.getComponentUnderTest().count(new HashMap<String, Object>());
        Assert.assertEquals(0, result);
    }

    @Test
    public void testQueryBuilder() throws URISyntaxException, ClientProtocolException, IOException,
        ComponentLookupException
    {
        URI expectedURI = new URI("http://rest.genenames.org/search/+status%3A%28Approved%29"
            + "+AND+locus_type%3A%28RNA%2C%5C+cluster+RNA%2C%5C+micro*+%29"
            + "+%28+symbol%3A%28br%5C%3AcA*%29+alias_symbol%3A%28br%5C%5EcA*%29%29"
            + "+AND+%28+locus_group%3A%28non%5C-coding%5C+RNA%29%29+-%28+symbol%3A%28M*%29%29");
        CapturingMatcher<HttpUriRequest> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream("NOTHING.json"));
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("status", "Approved");
        search.put("locus_type", Arrays.asList("RNA, cluster", "RNA, micro*"));
        search.put("hgnc_id", Collections.emptyList());
        Map<String, String> subquery = new LinkedHashMap<>();
        subquery.put("symbol", "br:cA*");
        subquery.put("alias_symbol", "br^cA*");
        search.put("OR", subquery);
        subquery = new LinkedHashMap<>();
        subquery.put("locus_group", "non-coding RNA");
        search.put("AND", subquery);
        subquery = new LinkedHashMap<>();
        subquery.put("symbol", "M*");
        search.put("NOT", subquery);
        subquery = new LinkedHashMap<>();
        subquery.put("what", "where");
        search.put("DISCARD", subquery);
        this.mocker.getComponentUnderTest().search(search);
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        Assert.assertEquals("application/json", reqCapture.getLastValue().getLastHeader("Accept").getValue());
    }

    @Test
    public void getAliases() throws ComponentLookupException
    {
        Set<String> aliases = this.mocker.getComponentUnderTest().getAliases();
        Assert.assertTrue(aliases.contains("hgnc"));
        Assert.assertTrue(aliases.contains("HGNC"));
    }

    @Test
    public void getDefaultOntologyLocation() throws ComponentLookupException
    {
        String location = this.mocker.getComponentUnderTest().getDefaultOntologyLocation();
        Assert.assertEquals("http://rest.genenames.org/", location);
    }

    @Test
    public void invalidResponseReturnsEmptySearch() throws ComponentLookupException, ClientProtocolException,
    IOException
    {
        when(this.client.execute(any(HttpUriRequest.class))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream(""));
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("status", "Approved");
        Map<String, String> queryOptions = new LinkedHashMap<>();

        queryOptions.put("start", "three");
        queryOptions.put("rows", "");
        Assert.assertEquals(Collections.emptySet(), this.mocker.getComponentUnderTest().search(search, queryOptions));
    }

    @Test
    public void invalidOrEmptyResponseReturnsNoInfo() throws ComponentLookupException, ClientProtocolException,
        IOException
    {
        when(this.client.execute(any(HttpUriRequest.class))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(ClassLoader.getSystemResourceAsStream(""));
        Assert.assertEquals("", this.mocker.getComponentUnderTest().getVersion());
    }
}
