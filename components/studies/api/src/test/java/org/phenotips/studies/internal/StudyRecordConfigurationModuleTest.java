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
package org.phenotips.studies.internal;

import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.internal.DefaultRecordConfiguration;
import org.phenotips.configuration.spi.RecordConfigurationModule;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.uiextension.UIExtension;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link StudyRecordConfigurationModule}.
 *
 * @version $Id$
 */
public class StudyRecordConfigurationModuleTest
{
    private static final String SECTIONS_LABEL = "sections";

    private static final String FIELDS_LABEL = "fields";

    @Rule
    public final MockitoComponentMockingRule<RecordConfigurationModule> mocker =
        new MockitoComponentMockingRule<>(StudyRecordConfigurationModule.class);

    private DocumentAccessBridge dab;

    @Mock
    private DefaultRecordConfiguration config;

    @Mock
    private XWiki xwiki;

    private DocumentReference studyDocumentReference = new DocumentReference("xwiki", "Studies", "Ataxia");

    @Mock
    private XWikiDocument studyDocument;

    private DocumentReference mappingDocumentReference =
        new DocumentReference("xwiki", "PhenoTips", "Ataxia phenotype mapping");

    @Mock
    private BaseObject studyObject;

    private DocumentReference currentDocument = new DocumentReference("xwiki", "data", "P0000001");

    private DocumentReference bindingClassDocument = new DocumentReference("xwiki", "PhenoTips", "StudyBindingClass");

    private final RecordElement elementA1 = mockElement("elementA1");

    private final RecordElement elementA2 = mockElement("elementA2");

    private final RecordSection sectionA = mockSection("sectionA", this.elementA1, this.elementA2);

    private final RecordElement elementB1 = mockElement("elementB1");

    private final RecordElement elementB2 = mockElement("elementB2");

    private final RecordElement elementB3 = mockElement("elementB3");

    private final RecordSection sectionB = mockSection("sectionB", this.elementB1, this.elementB2, this.elementB3);

    private final RecordElement elementC1 = mockElement("elementC1");

    private final RecordSection sectionC = mockSection("sectionC", this.elementC1);

    @Before
    public void setUp() throws ComponentLookupException, XWikiException
    {
        MockitoAnnotations.initMocks(this);

        DocumentReferenceResolver<EntityReference> referenceResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(referenceResolver.resolve(StudyRecordConfigurationModule.STUDY_BINDING_CLASS_REFERENCE))
            .thenReturn(this.bindingClassDocument);

        DocumentReferenceResolver<String> referenceParser =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(referenceParser.resolve("Studies.Ataxia")).thenReturn(this.studyDocumentReference);
        when(referenceParser.resolve("PhenoTips.Ataxia phenotype mapping")).thenReturn(this.mappingDocumentReference);

        this.dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(this.dab.getCurrentDocumentReference()).thenReturn(this.currentDocument);
        when(this.dab.getProperty(this.currentDocument, this.bindingClassDocument,
            StudyRecordConfigurationModule.STUDY_REFERENCE_PROPERTY_LABEL))
                .thenReturn("Studies.Ataxia");

        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        final XWikiContext context = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getDocument(any(DocumentReference.class), any(XWikiContext.class)))
            .thenReturn(this.studyDocument);
        when(this.studyDocument.isNew()).thenReturn(false);
        when(this.studyDocument.getXObject(StudyRecordConfigurationModule.STUDY_CLASS_REFERENCE))
            .thenReturn(this.studyObject);

        when(this.config.getAllSections()).thenReturn(Arrays.asList(this.sectionA, this.sectionB, this.sectionC));
    }

    @Test
    public void processNullConfigurationReturnsNull() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().process(null));
    }

    @Test
    public void processWithNullCurrentDocumentReferenceReturnsPreviousConfigurationUnchanged()
        throws ComponentLookupException
    {
        when(this.dab.getCurrentDocumentReference()).thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);
    }

    @Test
    public void processWithNoStudyBoundReturnsPreviousConfigurationUnchanged() throws ComponentLookupException
    {
        when(this.dab.getProperty(this.currentDocument, this.bindingClassDocument,
            StudyRecordConfigurationModule.STUDY_REFERENCE_PROPERTY_LABEL))
                .thenReturn("");
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);

        when(this.dab.getProperty(this.currentDocument, this.bindingClassDocument,
            StudyRecordConfigurationModule.STUDY_REFERENCE_PROPERTY_LABEL))
                .thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);
    }

    @Test
    public void processWithMissingStudyReturnsPreviousConfigurationUnchanged() throws ComponentLookupException
    {
        when(this.studyDocument.isNew()).thenReturn(true);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);
    }

    @Test
    public void processWithInaccessibleStudyReturnsPreviousConfigurationUnchanged()
        throws ComponentLookupException, XWikiException
    {
        when(this.xwiki.getDocument(any(DocumentReference.class), any(XWikiContext.class)))
            .thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);
    }

    @Test
    public void processWithExceptionReturnsPreviousConfigurationUnchanged()
        throws ComponentLookupException, XWikiException
    {
        when(this.xwiki.getDocument(any(DocumentReference.class), any(XWikiContext.class)))
            .thenThrow(new XWikiException());
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);
    }

    @Test
    public void processWithNonStudyReturnsPreviousConfigurationUnchanged() throws ComponentLookupException
    {
        when(this.studyDocument.getXObject(StudyRecordConfigurationModule.STUDY_CLASS_REFERENCE)).thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);
    }

    @Test
    public void processWithUnconfiguredStudyReturnsPreviousConfigurationUnchanged() throws ComponentLookupException
    {
        when(this.studyObject.getListValue(SECTIONS_LABEL)).thenReturn(Collections.emptyList());
        when(this.studyObject.getListValue(FIELDS_LABEL)).thenReturn(Collections.emptyList());
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);

        when(this.studyObject.getListValue(SECTIONS_LABEL)).thenReturn(null);
        when(this.studyObject.getListValue(FIELDS_LABEL)).thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);

        when(this.studyObject.getListValue(SECTIONS_LABEL)).thenReturn(Collections.singletonList("sectionA"));
        when(this.studyObject.getListValue(FIELDS_LABEL)).thenReturn(null);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);

        when(this.studyObject.getListValue(SECTIONS_LABEL)).thenReturn(Collections.emptyList());
        when(this.studyObject.getListValue(FIELDS_LABEL)).thenReturn(Collections.singletonList("elementA1"));
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verifyZeroInteractions(this.config);
    }

    @Test
    public void processReordersAndDisablesSections() throws ComponentLookupException
    {
        when(this.studyObject.getListValue(SECTIONS_LABEL)).thenReturn(Arrays.asList("sectionB", "sectionA"));
        when(this.studyObject.getListValue(FIELDS_LABEL))
            .thenReturn(Arrays.asList("elementB2", "elementB1", "elementA2"));

        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));

        verify(this.sectionA, never()).setEnabled(false);
        verify(this.elementA1).setEnabled(false);
        verify(this.elementA2, never()).setEnabled(false);
        verify(this.sectionA).setElements(Arrays.asList(this.elementA2, this.elementA1));

        verify(this.sectionB, never()).setEnabled(false);
        verify(this.elementB1, never()).setEnabled(false);
        verify(this.elementB2, never()).setEnabled(false);
        verify(this.elementB3).setEnabled(false);
        verify(this.sectionB).setElements(Arrays.asList(this.elementB2, this.elementB1, this.elementB3));

        verify(this.sectionC).setEnabled(false);
    }

    @Test
    public void processSetsCustomMapping() throws ComponentLookupException
    {
        when(this.studyObject.getStringValue("mapping")).thenReturn("PhenoTips.Ataxia phenotype mapping");
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().process(this.config));
        verify(this.config).setPhenotypeMapping(this.mappingDocumentReference);
    }

    @Test
    public void priorityIs50() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getPriority());
    }

    @Test
    public void supportsPatientRecordType() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().supportsRecordType("patient"));

    }

    @Test
    public void doesNotSupportOtherRecordTypes() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType("family"));
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType("asdfasdf"));
    }

    @Test
    public void supportsRecordTypeReturnsFalseIfNullOrEmpty() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType(null));
        Assert.assertFalse(this.mocker.getComponentUnderTest().supportsRecordType(""));
    }

    private RecordSection mockSection(String sectionId, RecordElement... elements)
    {
        RecordSection section = mock(RecordSection.class);
        when(section.isEnabled()).thenReturn(true);
        UIExtension uix = mock(UIExtension.class);
        when(uix.getId()).thenReturn(sectionId);
        when(section.getExtension()).thenReturn(uix);
        when(section.getAllElements()).thenReturn(Arrays.asList(elements));
        return section;
    }

    private RecordElement mockElement(String elementId)
    {
        RecordElement element = mock(RecordElement.class);
        UIExtension uix = mock(UIExtension.class);
        when(uix.getId()).thenReturn(elementId);
        when(element.getExtension()).thenReturn(uix);
        return element;
    }
}
