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
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.translation.internal.GermanHPOTranslation;

import org.xwiki.cache.CacheException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.AddFieldType;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.util.NamedList;
import org.json.JSONException;
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
import static org.mockito.Mockito.when;

/**
 * Tests for the German translation of HPO.
 *
 * @version $Id$
 */
public class GermanHPOTranslationTest
{
    @Rule
    public final MockitoComponentMockingRule<VocabularyExtension> mocker =
        new MockitoComponentMockingRule<>(GermanHPOTranslation.class);

    @Mock
    private Vocabulary vocabulary;

    @Mock
    private SolrClient solrClient;

    @Mock
    private VocabularyInputTerm term;

    private VocabularyExtension component;

    private LocalizationContext localizationContext;

    private ConfigurationSource config;

    private CapturingMatcher<? extends SolrRequest<? extends SolrResponse>> coreRequests = new CapturingMatcher<>();

    @Before
    public void setUp()
        throws ComponentLookupException, IOException, SolrServerException, CacheException
    {
        MockitoAnnotations.initMocks(this);

        SolrVocabularyResourceManager solrManager = this.mocker.getInstance(SolrVocabularyResourceManager.class);
        when(solrManager.getSolrConnection(any(Vocabulary.class))).thenReturn(this.solrClient);
        when(this.solrClient.request(Matchers.argThat(this.coreRequests), any())).thenReturn(new NamedList<>());

        this.localizationContext = this.mocker.getInstance(LocalizationContext.class);
        when(this.localizationContext.getCurrentLocale()).thenReturn(Locale.GERMAN);

        this.config = this.mocker.getInstance(ConfigurationSource.class, "wiki");
        when(this.config.getProperty("multilingual", false)).thenReturn(true);
        when(this.config.getProperty("languages", Collections.singleton("de")))
            .thenReturn(Collections.singleton("de"));

        when(this.vocabulary.getIdentifier()).thenReturn("hpo");

        this.component = this.mocker.getComponentUnderTest();
    }

    @Test
    public void germanFieldTypeIsAddedDuringInitialization() throws JSONException, IOException
    {
        SchemaRequest.AddFieldType fieldTypeRequest = (AddFieldType) this.coreRequests.getAllValues().stream()
            .filter(item -> item instanceof SchemaRequest.AddFieldType).findAny().get();
        JSONObject request =
            new JSONObject(IOUtils.toString(fieldTypeRequest.getContentStreams().iterator().next().getReader()))
                .getJSONObject("add-field-type");
        Assert.assertEquals("text_general_de", request.get("name"));
        Assert.assertEquals("org.apache.lucene.analysis.de.GermanAnalyzer",
            request.getJSONObject("analyzer").get("class"));
    }

    @Test
    public void germanFieldsAreAddedDuringInitialization() throws JSONException, IOException
    {
        List<JSONObject> fieldRequests = this.coreRequests.getAllValues().stream()
            .filter(item -> item instanceof SchemaRequest.AddField)
            .map(item -> {
                try {
                    return new JSONObject(IOUtils.toString(item.getContentStreams().iterator().next().getReader()))
                        .getJSONObject("add-field");
                } catch (JSONException | IOException e) {
                    return null;
                }
            })
            .collect(Collectors.toList());
        Set<String> fieldNames = fieldRequests.stream().map(item -> item.getString("name")).collect(Collectors.toSet());
        Assert.assertTrue(fieldNames.contains("name_de"));
        Assert.assertTrue(fieldNames.contains("def_de"));
        Assert.assertTrue(fieldNames.contains("synonym_de"));
        for (JSONObject request : fieldRequests) {
            Assert.assertEquals("text_general_de", request.get("type"));
            Assert.assertTrue(request.getBoolean("stored"));
            Assert.assertTrue(request.getBoolean("indexed"));
        }
    }

    @Test
    public void queriesAreExtendedToIncludeGermanFieldsWhenLocaleIsDe()
    {
        SolrQuery query = new SolrQuery("seizures");
        query.set(DisMaxParams.QF, "name");
        query.set(DisMaxParams.PF, "name");
        this.component.extendQuery(query, this.vocabulary);
        Assert.assertEquals("name name_de^60 synonym_de^45 def_de^12 ", query.get(DisMaxParams.PF));
        Assert.assertEquals("name name_de^30 synonym_de^21 def_de^6 ", query.get(DisMaxParams.QF));
    }

    @Test
    public void queriesAreExtendedToIncludeGermanFieldsWhenLocaleIsDeDE()
    {
        when(this.localizationContext.getCurrentLocale()).thenReturn(Locale.GERMANY);
        SolrQuery query = new SolrQuery("seizures");
        query.set(DisMaxParams.QF, "name");
        query.set(DisMaxParams.PF, "name");
        this.component.extendQuery(query, this.vocabulary);
        Assert.assertEquals("name name_de^60 synonym_de^45 def_de^12 ", query.get(DisMaxParams.PF));
        Assert.assertEquals("name name_de^30 synonym_de^21 def_de^6 ", query.get(DisMaxParams.QF));
    }

    @Test
    public void queriesAreNotModifiedWhenLocaleIsFr()
    {
        when(this.localizationContext.getCurrentLocale()).thenReturn(Locale.FRENCH);
        SolrQuery query = new SolrQuery("seizures");
        query.set(DisMaxParams.QF, "name");
        query.set(DisMaxParams.PF, "name");
        this.component.extendQuery(query, this.vocabulary);
        Assert.assertEquals("name", query.get(DisMaxParams.PF));
        Assert.assertEquals("name", query.get(DisMaxParams.QF));
    }

    @Test
    public void queriesAreNotModifiedWhenLocaleIsNull()
    {
        when(this.localizationContext.getCurrentLocale()).thenReturn(null);
        SolrQuery query = new SolrQuery("seizures");
        query.set(DisMaxParams.QF, "name");
        query.set(DisMaxParams.PF, "name");
        this.component.extendQuery(query, this.vocabulary);
        Assert.assertEquals("name", query.get(DisMaxParams.PF));
        Assert.assertEquals("name", query.get(DisMaxParams.QF));
    }

    @Test
    public void queriesAreNotModifiedWhenFieldsAreInitiallyEmpty()
    {
        SolrQuery query = new SolrQuery("seizures");
        query.set(DisMaxParams.BQ, "hello");
        this.component.extendQuery(query, this.vocabulary);
        Assert.assertNull(query.get(DisMaxParams.PF));
        Assert.assertNull(query.get(DisMaxParams.QF));
    }

    @Test
    public void termsAreExtendedWhenLocaleIsSupported()
    {
        when(this.term.getId()).thenReturn("HP:0000001");
        this.component.extendTerm(this.term, this.vocabulary);
        Mockito.verify(this.term).set("name_de", "Alle");
    }
}
