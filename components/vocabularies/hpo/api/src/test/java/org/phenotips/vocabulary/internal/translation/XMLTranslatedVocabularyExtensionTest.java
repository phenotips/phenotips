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
package org.phenotips.vocabulary.internal.translation;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.internal.solr.SolrVocabularyInputTerm;
import org.phenotips.vocabulary.internal.solr.HumanPhenotypeOntology;

import org.xwiki.localization.LocalizationContext;

import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link XMLTranslatedVocabularyExtension}.
 *
 * @version $Id$
 */
public class XMLTranslatedVocabularyExtensionTest
{
    /**
     * The name of the ontology to be fed to the translate extension.
     */
    private static final String ONTOLOGY_NAME = "hpo";

    /**
     * The mocker.
     */
    @Rule
    public final MockitoComponentMockingRule<VocabularyExtension> mocker =
        new MockitoComponentMockingRule<VocabularyExtension>(XMLTranslatedVocabularyExtension.class);

    /**
     * The component under test.
     */
    private VocabularyExtension extension;

    /**
     * The vocabulary instance.
     */
    private Vocabulary vocabularyInstance;

    @Before
    public void setUp() throws Exception
    {
        Locale locale = new Locale("es");
        LocalizationContext ctx = mocker.getInstance(LocalizationContext.class);
        when(ctx.getCurrentLocale()).thenReturn(locale);
        extension = mocker.getComponentUnderTest();
        extension.indexingStarted(ONTOLOGY_NAME);
    }

    @After
    public void tearDown() throws Exception
    {
        extension.indexingEnded(ONTOLOGY_NAME);
    }

    @Test
    public void testTranslate()
    {
        SolrInputDocument doc = new SolrInputDocument();
        /* Abnormality of body height ought to be a relatively uncontroversial term, so
         * ideally won't change later. */
        String testId = "HP:0000002";
        String testName = "Abnormality of body height";
        String expectedName = "Anormalidad de la estatura";
        doc.setField("id", testId);
        doc.setField("name", testName);
        VocabularyInputTerm term = new SolrVocabularyInputTerm(doc, vocabularyInstance);
        extension.extendTerm(term, ONTOLOGY_NAME);
        assertEquals(testName, term.getName());
        assertEquals(expectedName, term.get("name_es"));
        assertEquals(testId, term.getId());
        /* This kind of crazy stuff should never happen */
        assertNull(term.get("id_es"));
    }
}
