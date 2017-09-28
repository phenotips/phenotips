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
package org.phenotips.vocabulary;

import org.phenotips.Constants;
import org.phenotips.vocabulary.internal.solr.DefaultVocabularySourceRelocationService;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VocabularySourceRelocationService}.
 */
public class VocabularySourceRelocationServiceTest
{
    /** The document where vocabulary relocation sources are stored. */
    private static final EntityReference VOCABULARY_RELOCATION_CLASS = new EntityReference(
        "VocabularySourceRelocationClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String ANNOTATION_SOURCE = "http://compbio.charite.de/jenkins/job/hpo.annotations/"
        + "lastStableBuild/artifact/misc/negative_phenotype_annotation.tab";

    @Rule
    public final MockitoComponentMockingRule<DefaultVocabularySourceRelocationService> mocker =
        new MockitoComponentMockingRule<DefaultVocabularySourceRelocationService>(
            DefaultVocabularySourceRelocationService.class);

    @Mock
    private VocabularyInputTerm inputTerm;

    @Mock
    private Vocabulary vocabulary;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private BaseObject object;

    private VocabularySourceRelocationService relocationServer;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        this.relocationServer = this.mocker.getComponentUnderTest();
        this.object.setStringValue("relocation", ANNOTATION_SOURCE);
    }

    @Test
    public void getAnnotationSource() throws Exception
    {
        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        final XWikiContext context = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDocument(any(DocumentReference.class), any(XWikiContext.class)))
            .thenReturn(this.document);
        when(this.document.isNew()).thenReturn(false);
        when(this.document.getXObject(VOCABULARY_RELOCATION_CLASS))
            .thenReturn(this.object);
        String source = this.relocationServer.getRelocation(ANNOTATION_SOURCE);
        Assert.assertEquals(ANNOTATION_SOURCE, source);
    }
}
