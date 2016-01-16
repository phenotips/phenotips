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
package org.phenotips.data.internal;

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientScorer;
import org.phenotips.data.PatientSpecificity;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.CapturingMatcher;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MonarchPatientScorerTest
{
    private ConfigurationSource configuration;

    @Mock
    private Patient patient;

    @Mock
    private CloseableHttpClient client;

    @Mock
    private CloseableHttpResponse response;

    @Mock
    private HttpEntity responseEntity;

    @Mock
    private Cache<PatientSpecificity> cache;

    private Set<Feature> features = new LinkedHashSet<>();

    @Rule
    public final MockitoComponentMockingRule<PatientScorer> mocker =
        new MockitoComponentMockingRule<PatientScorer>(MonarchPatientScorer.class);

    @Before
    public void setup() throws CacheException, ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        CacheManager cm = this.mocker.getInstance(CacheManager.class);
        when(cm.<PatientSpecificity>createNewCache(any(CacheConfiguration.class))).thenReturn(this.cache);

        this.configuration = this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        when(this.configuration.getProperty("phenotips.patientScoring.monarch.serviceURL",
            "http://monarchinitiative.org/score"))
                .thenReturn("http://monarchinitiative.org/score");

        Feature feature = mock(Feature.class);
        when(feature.getId()).thenReturn("HP:1");
        when(feature.isPresent()).thenReturn(true);
        this.features.add(feature);
        feature = mock(Feature.class);
        when(feature.getId()).thenReturn("HP:2");
        when(feature.isPresent()).thenReturn(false);
        this.features.add(feature);
        feature = mock(Feature.class);
        when(feature.getName()).thenReturn("custom");
        this.features.add(feature);

        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "client", this.client);
    }

    @Test
    public void getScoreWithNoFeaturesReturns0() throws ComponentLookupException
    {
        Mockito.doReturn(Collections.emptySet()).when(this.patient).getFeatures();
        double score = this.mocker.getComponentUnderTest().getScore(this.patient);
        Assert.assertEquals(0.0, score, 0.0);
    }

    @Test
    public void getScoreSearchesRemotely() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        Mockito.doReturn(this.features).when(this.patient).getFeatures();
        URI expectedURI = new URI("http://monarchinitiative.org/score");
        CapturingMatcher<HttpPost> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(IOUtils.toInputStream("{\"scaled_score\":2}"));
        double score = this.mocker.getComponentUnderTest().getScore(this.patient);
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        String content =
            URLDecoder.decode(IOUtils.toString(reqCapture.getLastValue().getEntity().getContent(), "UTF-8"), "UTF-8");
        Assert.assertTrue(content.startsWith("annotation_profile="));
        JSONObject actualJson = new JSONObject(content.substring("annotation_profile=".length()));
        JSONObject expectedJson =
            new JSONObject("{\"features\":[{\"id\":\"HP:1\"},{\"id\":\"HP:2\",\"isPresent\":false}]}");
        Assert.assertTrue(expectedJson.similar(actualJson));
        Assert.assertEquals("application/x-www-form-urlencoded; charset=UTF-8",
            reqCapture.getLastValue().getEntity().getContentType().getValue());
        Assert.assertEquals(2.0, score, 0.0);
    }

    @Test
    public void getScoreUsesCache() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        Mockito.doReturn(this.features).when(this.patient).getFeatures();
        PatientSpecificity spec = mock(PatientSpecificity.class);
        when(this.cache.get("HP:1-HP:2")).thenReturn(spec);
        when(spec.getScore()).thenReturn(2.0);
        double score = this.mocker.getComponentUnderTest().getScore(this.patient);
        Assert.assertEquals(2.0, score, 0.0);
        Mockito.verifyZeroInteractions(this.client);
    }

    @Test
    public void getScoreWithNoResponseReturnsNegative1() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException
    {
        Mockito.doReturn(this.features).when(this.patient).getFeatures();
        URI expectedURI = new URI("http://monarchinitiative.org/score");
        CapturingMatcher<HttpPost> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(IOUtils.toInputStream(""));
        double score = this.mocker.getComponentUnderTest().getScore(this.patient);
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        Assert.assertEquals(-1.0, score, 0.0);
    }

    @Test
    public void getScoreWithExceptionReturnsNegative1() throws Exception
    {
        Mockito.doReturn(this.features).when(this.patient).getFeatures();
        when(this.client.execute(any(HttpUriRequest.class))).thenThrow(new IOException());
        double score = this.mocker.getComponentUnderTest().getScore(this.patient);
        Assert.assertEquals(-1.0, score, 0.0);
        Mockito.verify(this.cache, Mockito.never()).set(any(String.class), any(PatientSpecificity.class));
    }

    @Test
    public void getSpecificityWithNoFeaturesReturns0() throws ComponentLookupException
    {
        Mockito.doReturn(Collections.emptySet()).when(this.patient).getFeatures();
        CapturingMatcher<PatientSpecificity> specCapture = new CapturingMatcher<>();
        Mockito.doNothing().when(this.cache).set(Matchers.eq(""), Matchers.argThat(specCapture));
        Date d1 = new Date();
        this.mocker.getComponentUnderTest().getSpecificity(this.patient);
        Date d2 = new Date();
        PatientSpecificity spec = specCapture.getLastValue();
        Assert.assertEquals(0.0, spec.getScore(), 0.0);
        Assert.assertEquals("monarchinitiative.org", spec.getComputingMethod());
        Assert.assertFalse(d1.after(spec.getComputationDate()));
        Assert.assertFalse(d2.before(spec.getComputationDate()));
    }

    @Test
    public void getSpecificitySearchesRemotely() throws Exception
    {
        Mockito.doReturn(this.features).when(this.patient).getFeatures();
        URI expectedURI = new URI("http://monarchinitiative.org/score");
        CapturingMatcher<HttpPost> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(IOUtils.toInputStream("{\"scaled_score\":2}"));
        CapturingMatcher<PatientSpecificity> specCapture = new CapturingMatcher<>();
        Mockito.doNothing().when(this.cache).set(Matchers.eq("HP:1-HP:2"), Matchers.argThat(specCapture));
        Date d1 = new Date();
        this.mocker.getComponentUnderTest().getSpecificity(this.patient);
        Date d2 = new Date();
        PatientSpecificity spec = specCapture.getLastValue();
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        String content =
            URLDecoder.decode(IOUtils.toString(reqCapture.getLastValue().getEntity().getContent(), "UTF-8"), "UTF-8");
        Assert.assertTrue(content.startsWith("annotation_profile="));
        JSONObject actualJson = new JSONObject(content.substring("annotation_profile=".length()));
        JSONObject expectedJson =
            new JSONObject("{\"features\":[{\"id\":\"HP:1\"},{\"id\":\"HP:2\",\"isPresent\":false}]}");
        Assert.assertTrue(expectedJson.similar(actualJson));
        Assert.assertEquals("application/x-www-form-urlencoded; charset=UTF-8",
            reqCapture.getLastValue().getEntity().getContentType().getValue());
        Assert.assertEquals(2.0, spec.getScore(), 0.0);
        Assert.assertEquals("monarchinitiative.org", spec.getComputingMethod());
        Assert.assertFalse(d1.after(spec.getComputationDate()));
        Assert.assertFalse(d2.before(spec.getComputationDate()));
    }

    @Test
    public void getSpecificityUsesCache() throws Exception
    {
        Mockito.doReturn(this.features).when(this.patient).getFeatures();
        PatientSpecificity spec = mock(PatientSpecificity.class);
        when(this.cache.get("HP:1-HP:2")).thenReturn(spec);
        Assert.assertSame(spec, this.mocker.getComponentUnderTest().getSpecificity(this.patient));
        Mockito.verifyZeroInteractions(this.client);
    }

    @Test
    public void getSpecificityWithNoResponseReturnsNull() throws Exception
    {
        Mockito.doReturn(this.features).when(this.patient).getFeatures();
        when(this.client.execute(any(HttpUriRequest.class))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(IOUtils.toInputStream(""));
        Assert.assertNull(this.mocker.getComponentUnderTest().getSpecificity(this.patient));
    }

    @Test(expected = InitializationException.class)
    public void initializationFailsWhenCreatingCacheFails() throws ComponentLookupException, CacheException,
        InitializationException
    {
        CacheManager cm = this.mocker.getInstance(CacheManager.class);
        when(cm.<PatientSpecificity>createNewCache(any(CacheConfiguration.class))).thenThrow(
            new CacheException("failed"));
        ((org.xwiki.component.phase.Initializable) this.mocker.getComponentUnderTest()).initialize();
    }

    @Test
    public void checkURLConfigurable() throws ComponentLookupException, URISyntaxException,
        ClientProtocolException, IOException, InitializationException
    {
        when(this.configuration.getProperty("phenotips.patientScoring.monarch.serviceURL",
            "http://monarchinitiative.org/score"))
                .thenReturn("http://proxy/score");
        Mockito.doReturn(this.features).when(this.patient).getFeatures();
        URI expectedURI = new URI("http://proxy/score");
        CapturingMatcher<HttpPost> reqCapture = new CapturingMatcher<>();
        when(this.client.execute(Matchers.argThat(reqCapture))).thenReturn(this.response);
        when(this.response.getEntity()).thenReturn(this.responseEntity);
        when(this.responseEntity.getContent()).thenReturn(IOUtils.toInputStream("{\"scaled_score\":2}"));
        // Since the component was already initialized in setUp() with the default URL, re-initialize it
        // with the new configuration mock
        ((Initializable) this.mocker.getComponentUnderTest()).initialize();
        double score = this.mocker.getComponentUnderTest().getScore(this.patient);
        Assert.assertEquals(expectedURI, reqCapture.getLastValue().getURI());
        String content =
            URLDecoder.decode(IOUtils.toString(reqCapture.getLastValue().getEntity().getContent(), "UTF-8"), "UTF-8");
        Assert.assertTrue(content.startsWith("annotation_profile="));
        JSONObject actualJson = new JSONObject(content.substring("annotation_profile=".length()));
        JSONObject expectedJson =
            new JSONObject("{\"features\":[{\"id\":\"HP:1\"},{\"id\":\"HP:2\",\"isPresent\":false}]}");
        Assert.assertTrue(expectedJson.similar(actualJson));
        Assert.assertEquals("application/x-www-form-urlencoded; charset=UTF-8",
            reqCapture.getLastValue().getEntity().getContentType().getValue());
        Assert.assertEquals(2.0, score, 0.0);
    }
}
