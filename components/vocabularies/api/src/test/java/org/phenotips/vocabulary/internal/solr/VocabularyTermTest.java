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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.localization.LocalizationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;

import javax.inject.Provider;

import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link org.phenotips.vocabulary.VocabularyTerm}.
 */
public class VocabularyTermTest
{
    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private ComponentManager cm;

    @Mock
    private LocalizationContext lc;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(LocalizationContext.class)).thenReturn(this.lc);
        when(this.lc.getCurrentLocale()).thenReturn(new Locale("es", "ES"));
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
    }

    @Test
    public void testDistanceToRootTerm()
    {
        Vocabulary vocabulary = mock(Vocabulary.class);

        SolrDocument rootDoc = new SolrDocument();
        rootDoc.setField("id", "T0");
        VocabularyTerm rootTerm = new SolrVocabularyTerm(rootDoc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T0"))).thenReturn(Collections.singleton(rootTerm));

        SolrDocument p1Doc = new SolrDocument();
        p1Doc.setField("id", "T1");
        p1Doc.setField("is_a", Collections.singleton("T0"));
        VocabularyTerm p1Term = new SolrVocabularyTerm(p1Doc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T1"))).thenReturn(Collections.singleton(p1Term));

        SolrDocument p2Doc = new SolrDocument();
        p2Doc.setField("id", "T2");
        p2Doc.setField("is_a", Collections.singleton("T1"));
        VocabularyTerm p2Term = new SolrVocabularyTerm(p2Doc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T2"))).thenReturn(Collections.singleton(p2Term));

        SolrDocument childDoc = new SolrDocument();
        childDoc.setField("id", "T3");
        childDoc.setField("is_a", Collections.singleton("T2"));
        VocabularyTerm childTerm = new SolrVocabularyTerm(childDoc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T3"))).thenReturn(Collections.singleton(childTerm));

        assertEquals(3, childTerm.getDistanceTo(rootTerm));
        assertEquals(3, rootTerm.getDistanceTo(childTerm));
    }

    @Test
    public void toJSONContainsParents()
    {
        Vocabulary vocabulary = mock(Vocabulary.class);
        SolrDocument rootDoc = new SolrDocument();
        rootDoc.setField("id", "T0");
        VocabularyTerm rootTerm = new SolrVocabularyTerm(rootDoc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T0"))).thenReturn(Collections.singleton(rootTerm));

        SolrDocument p1Doc = new SolrDocument();
        p1Doc.setField("id", "T1");
        p1Doc.setField("name", "Term 1");
        p1Doc.setField("is_a", Collections.singleton("T0"));
        VocabularyTerm p1Term = new SolrVocabularyTerm(p1Doc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T1"))).thenReturn(Collections.singleton(p1Term));

        SolrDocument p2Doc = new SolrDocument();
        p2Doc.setField("id", "T2");
        p2Doc.setField("name", "Term 2");
        p2Doc.setField("is_a", Collections.singleton("T0"));
        VocabularyTerm p2Term = new SolrVocabularyTerm(p2Doc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T2"))).thenReturn(Collections.singleton(p2Term));

        SolrDocument childDoc = new SolrDocument();
        childDoc.setField("id", "T3");
        childDoc.setField("is_a", Arrays.asList("T1", "T2"));
        VocabularyTerm childTerm = new SolrVocabularyTerm(childDoc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T3"))).thenReturn(Collections.singleton(childTerm));

        when(vocabulary.getTerms(new LinkedHashSet<>(Arrays.asList("T1", "T2"))))
            .thenReturn(new LinkedHashSet<>(Arrays.asList(p1Term, p2Term)));

        when(this.lc.getCurrentLocale()).thenReturn(new Locale("es", "ES", "normal"));

        JSONObject json = childTerm.toJSON();
        Assert.assertTrue(json.has("parents"));
        JSONArray parents = json.getJSONArray("parents");
        Assert.assertEquals(2, parents.length());
        JSONObject parent = parents.getJSONObject(0);
        Assert.assertTrue(
            parent.similar(new JSONObject("{\"id\":\"T1\",\"name\":\"Term 1\",\"name_translated\":\"Term 1\"}")));
        parent = parents.getJSONObject(1);
        Assert.assertTrue(
            parent.similar(
                new JSONObject("{\"id\":\"T2\",\"name\":\"Term 2\",\"name_translated\":\"Term 2\"}")));
    }

    @Test
    public void toJSONContainsNoParentsWhenTermHasNoParents()
    {
        Vocabulary vocabulary = mock(Vocabulary.class);
        SolrDocument rootDoc = new SolrDocument();
        rootDoc.setField("id", "T0");
        VocabularyTerm rootTerm = new SolrVocabularyTerm(rootDoc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T0"))).thenReturn(Collections.singleton(rootTerm));

        JSONObject json = rootTerm.toJSON();
        Assert.assertFalse(json.has("parents"));
    }

    @Test
    public void translatedPropertiesUseDefaultValuesWhenLocaleIsNotKnown() throws ComponentLookupException
    {
        Vocabulary vocabulary = mock(Vocabulary.class);

        SolrDocument doc = new SolrDocument();
        doc.setField("id", "T1");
        doc.setField("name", "Term");
        doc.setField("name_", "Invalid");
        doc.setField("name_es", "El Term");
        doc.setField("def", "Def");
        doc.setField("def_", "Invalid");
        doc.setField("def_en", "The term");
        doc.setField("is_a", Collections.singleton("T0"));
        VocabularyTerm term = new SolrVocabularyTerm(doc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T1"))).thenReturn(Collections.singleton(term));

        when(this.lc.getCurrentLocale()).thenReturn(null);
        JSONObject json = term.toJSON();
        Assert.assertEquals("Term", json.get("name_translated"));
        Assert.assertEquals("Def", json.get("def_translated"));

        when(this.cm.getInstance(LocalizationContext.class)).thenThrow(new ComponentLookupException("Unavailable"));
        json = term.toJSON();
        Assert.assertEquals("Term", json.get("name_translated"));
        Assert.assertEquals("Def", json.get("def_translated"));
    }

    @Test
    public void translatedPropertiesCascadeDownLocaleParts() throws ComponentLookupException
    {
        Vocabulary vocabulary = mock(Vocabulary.class);

        SolrDocument doc = new SolrDocument();
        doc.setField("id", "T1");
        doc.setField("name", "Term");
        doc.setField("is_a", Collections.singleton("T0"));
        VocabularyTerm term = new SolrVocabularyTerm(doc, vocabulary);
        when(vocabulary.getTerms(Collections.singleton("T1"))).thenReturn(Collections.singleton(term));
        when(this.lc.getCurrentLocale()).thenReturn(new Locale("es", "ES", "normal"));

        Assert.assertEquals("Term", term.toJSON().get("name_translated"));

        doc.setField("name_es", "El Term 1");
        Assert.assertEquals("El Term 1", term.toJSON().get("name_translated"));

        doc.setField("name_es_ES", "El Term 2");
        Assert.assertEquals("El Term 2", term.toJSON().get("name_translated"));

        doc.setField("name_es_ES_normal", "El Term 3");
        Assert.assertEquals("El Term 3", term.toJSON().get("name_translated"));

        when(this.lc.getCurrentLocale()).thenReturn(new Locale("es", "ES"));
        Assert.assertEquals("El Term 2", term.toJSON().get("name_translated"));

        when(this.lc.getCurrentLocale()).thenReturn(new Locale("es"));
        Assert.assertEquals("El Term 1", term.toJSON().get("name_translated"));

        when(this.lc.getCurrentLocale()).thenReturn(new Locale("fr"));
        Assert.assertEquals("Term", term.toJSON().get("name_translated"));
    }
}
