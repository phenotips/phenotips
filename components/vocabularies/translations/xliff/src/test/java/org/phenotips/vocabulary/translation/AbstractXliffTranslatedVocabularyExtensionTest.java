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
package org.phenotips.vocabulary.translation;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.internal.solr.SolrVocabularyInputTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.DisMaxParams;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link AbstractXliffTranslatedVocabularyExtension}.
 *
 * @version $Id$
 */
public class AbstractXliffTranslatedVocabularyExtensionTest
{
    /** The mocker component manager. */
    @Rule
    public final MockitoComponentMockingRule<VocabularyExtension> mocker =
        new MockitoComponentMockingRule<VocabularyExtension>(MockXLIFFTranslatedVocabularyExtension.class);

    /** The component under test. */
    private VocabularyExtension extension;

    /** The vocabulary instance. */
    private Vocabulary vocabularyInstance;

    @Before
    public void setUp() throws Exception
    {
        this.vocabularyInstance = mock(Vocabulary.class);
        when(this.vocabularyInstance.getIdentifier()).thenReturn("hpo");
        Locale locale = new Locale("en");
        LocalizationContext ctx = this.mocker.getInstance(LocalizationContext.class);
        when(ctx.getCurrentLocale()).thenReturn(locale);
        ConfigurationSource config = this.mocker.getInstance(ConfigurationSource.class, "wiki");
        Set<String> languages = new HashSet<>();
        languages.add("en");
        languages.add("es");
        languages.add("");
        languages.add("fr");
        when(config.getProperty("languages", Collections.singleton("en"))).thenReturn(languages);
        when(config.getProperty("multilingual", Boolean.FALSE)).thenReturn(Boolean.TRUE);
        this.extension = this.mocker.getComponentUnderTest();
        this.extension.indexingStarted(this.vocabularyInstance);
    }

    @After
    public void tearDown() throws Exception
    {
        this.extension.indexingEnded(this.vocabularyInstance);
    }

    @Test
    public void translationsAreLoaded()
    {
        SolrInputDocument doc = new SolrInputDocument();
        String testId = "HP:0000003";
        doc.setField("id", testId);
        VocabularyInputTerm term = new SolrVocabularyInputTerm(doc, this.vocabularyInstance);
        this.extension.extendTerm(term, this.vocabularyInstance);
        assertEquals(testId, term.getId());
        assertEquals("Displasia renal multiquística", term.get("name_es"));
        assertEquals("La displasia multicística del riñón se caracteriza...", term.get("def_es"));
        @SuppressWarnings("unchecked")
        List<String> translatedSynonyms = (List<String>) term.get("synonym_es");
        Assert.assertThat(translatedSynonyms, Matchers.hasItems("Riñón displásico multicística",
            "Riñones multicísticos", "Displasia renal multicística"));
        // This kind of crazy stuff should never happen
        assertNull(term.get("id_es"));
    }

    @Test
    public void missingTranslationsDontAffectBehavior()
    {
        SolrInputDocument doc = new SolrInputDocument();
        // Abnormality of body height ought to be a relatively uncontroversial term, so ideally won't change later.
        String testId = "HP:0000001";
        doc.setField("id", testId);
        VocabularyInputTerm term = new SolrVocabularyInputTerm(doc, this.vocabularyInstance);
        this.extension.extendTerm(term, this.vocabularyInstance);
        assertEquals(testId, term.getId());
        assertNull(term.get("id_es"));
        assertNull(term.get("name_es"));
        assertNull(term.get("def_es"));
        assertNull(term.get("synonym_es"));
    }

    @Test
    public void queryIsExtendedWithLocalizedFields() throws ComponentLookupException
    {
        LocalizationContext ctx = this.mocker.getInstance(LocalizationContext.class);
        when(ctx.getCurrentLocale()).thenReturn(Locale.forLanguageTag("es"));
        SolrQuery query = new SolrQuery();
        query.set(DisMaxParams.PF, "name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5");
        query.set(DisMaxParams.QF,
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5");
        this.extension.extendQuery(query, this.vocabularyInstance);

        Assert.assertEquals("name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5 "
            + "name_es^60 synonym_es^45 def_es^12 ", query.get(DisMaxParams.PF));
        Assert.assertEquals(
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5"
                + " name_es^30 synonym_es^21 def_es^6 ",
            query.get(DisMaxParams.QF));
    }

    @Test
    public void queryIsExtendedWhenCurrentLanguageIsMoreSpecific() throws ComponentLookupException
    {
        LocalizationContext ctx = this.mocker.getInstance(LocalizationContext.class);
        when(ctx.getCurrentLocale()).thenReturn(Locale.forLanguageTag("es-US"));
        SolrQuery query = new SolrQuery();
        query.set(DisMaxParams.PF, "name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5");
        query.set(DisMaxParams.QF,
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5");
        this.extension.extendQuery(query, this.vocabularyInstance);

        Assert.assertEquals("name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5 "
            + "name_es^60 synonym_es^45 def_es^12 ", query.get(DisMaxParams.PF));
        Assert.assertEquals(
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5"
                + " name_es^30 synonym_es^21 def_es^6 ",
            query.get(DisMaxParams.QF));
    }

    @Test
    public void queryIsUnmodifiedWhenLanguageIsNotSupported() throws ComponentLookupException
    {
        LocalizationContext ctx = this.mocker.getInstance(LocalizationContext.class);
        when(ctx.getCurrentLocale()).thenReturn(Locale.forLanguageTag("fr"));
        SolrQuery query = new SolrQuery();
        query.set(DisMaxParams.PF, "name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5");
        query.set(DisMaxParams.QF,
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5");
        this.extension.extendQuery(query, this.vocabularyInstance);

        Assert.assertEquals("name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5",
            query.get(DisMaxParams.PF));
        Assert.assertEquals(
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5",
            query.get(DisMaxParams.QF));
    }

    @Test
    public void supportsHpo()
    {
        Assert.assertTrue(this.extension.isVocabularySupported(this.vocabularyInstance));
    }

    @Test
    public void doesntSupportAnyVocabulary()
    {
        when(this.vocabularyInstance.getIdentifier()).thenReturn("invalid");
        Assert.assertFalse(this.extension.isVocabularySupported(this.vocabularyInstance));
    }

    @Component
    @Singleton
    public static final class MockXLIFFTranslatedVocabularyExtension extends AbstractXliffTranslatedVocabularyExtension
    {
        @Override
        protected Locale getTargetLocale()
        {
            return Locale.forLanguageTag("es");
        }

        @Override
        protected String getTargetVocabularyId()
        {
            return "hpo";
        }
    }
}
