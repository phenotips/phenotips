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

import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientEvent;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.web.XWikiResponse;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PatientOnCancelDeleterTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientOnCancelDeleter> mocker =
        new MockitoComponentMockingRule<>(PatientOnCancelDeleter.class);

    XWikiContext context;

    XWikiDocument doc;

    PatientEvent event;

    XWiki wiki;

    @Before
    public void setUp() throws XWikiException, ComponentLookupException
    {
        Provider<XWikiContext> provider = mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        event = mock(PatientEvent.class);
        Patient patient = mock(Patient.class);
        context = provider.get();
        wiki = mock(XWiki.class);
        doc = mock(XWikiDocument.class);

        doReturn(context).when(provider).get();
        when(event.getPatient()).thenReturn(patient);
        when(context.getWiki()).thenReturn(wiki);
        when(wiki.getDocument(any(DocumentReference.class), eq(context))).thenReturn(doc);
    }

    @Test
    public void testNoHistoryDoc() throws XWikiException, ComponentLookupException
    {
        List<String> versions = new LinkedList<>();
        when(doc.getRevisions(any(RevisionCriteria.class), eq(context))).thenReturn(versions);

        mocker.getComponentUnderTest().onEvent(event, null, null);
        verify(wiki, never()).deleteDocument(doc, context);
    }

    @Test
    public void testHistoryException() throws XWikiException, ComponentLookupException
    {
        when(wiki.getDocument(any(DocumentReference.class), eq(context))).thenThrow(XWikiException.class);

        mocker.getComponentUnderTest().onEvent(event, null, null);
        verify(wiki, never()).deleteDocument(doc, context);
        verify(mocker.getMockedLogger(), times(1)).error(anyString(), anyString());
    }

    @Test
    public void testDocumentRetrievingException() throws XWikiException, ComponentLookupException
    {
        when(doc.getRevisions(any(RevisionCriteria.class), eq(context))).thenThrow(XWikiException.class);

        mocker.getComponentUnderTest().onEvent(event, null, null);
        verify(wiki, never()).deleteDocument(doc, context);
    }

    @Test
    public void testMinorEditsOnly() throws XWikiException, ComponentLookupException
    {
        List<String> versions = new LinkedList<>();
        XWikiRCSNodeInfo info1 = mock(XWikiRCSNodeInfo.class);
        XWikiRCSNodeInfo info2 = mock(XWikiRCSNodeInfo.class);
        versions.add("1");
        versions.add("2");

        when(doc.getRevisions(any(RevisionCriteria.class), eq(context))).thenReturn(versions);
        when(doc.getRevisionInfo(eq("1"), eq(context))).thenReturn(info1);
        when(doc.getRevisionInfo(eq("2"), eq(context))).thenReturn(info2);
        when(info1.getComment()).thenReturn("");
        when(info2.getComment()).thenReturn("");
        when(info1.isMinorEdit()).thenReturn(true);
        when(info2.isMinorEdit()).thenReturn(true);

        mocker.getComponentUnderTest().onEvent(event, null, null);
        verify(wiki, never()).deleteDocument(doc, context);
    }

    @Test
    public void testAutosaveAndMinor() throws XWikiException, ComponentLookupException
    {
        List<String> versions = new LinkedList<>();
        XWikiRCSNodeInfo info1 = mock(XWikiRCSNodeInfo.class);
        XWikiRCSNodeInfo info2 = mock(XWikiRCSNodeInfo.class);
        versions.add("1");
        versions.add("2");

        when(doc.getRevisions(any(RevisionCriteria.class), eq(context))).thenReturn(versions);
        when(doc.getRevisionInfo(eq("1"), eq(context))).thenReturn(info1);
        when(doc.getRevisionInfo(eq("2"), eq(context))).thenReturn(info2);
        when(info1.getComment()).thenReturn("");
        when(info2.getComment()).thenReturn("(Autosaved)");
        when(info1.isMinorEdit()).thenReturn(true);
        when(info2.isMinorEdit()).thenReturn(false);

        mocker.getComponentUnderTest().onEvent(event, null, null);
        verify(wiki, never()).deleteDocument(doc, context);
    }

    @Test
    public void testAutosaveMinorAndUserSave() throws XWikiException, ComponentLookupException, IOException
    {
        PatientOnCancelDeleter deleterSpy = spy(mocker.getComponentUnderTest());
        List<String> versions = new LinkedList<>();
        XWikiRCSNodeInfo info1 = mock(XWikiRCSNodeInfo.class);
        XWikiRCSNodeInfo info2 = mock(XWikiRCSNodeInfo.class);
        XWikiRCSNodeInfo info3 = mock(XWikiRCSNodeInfo.class);
        versions.add("1");
        versions.add("2");
        versions.add("3");

        when(doc.getRevisions(any(RevisionCriteria.class), eq(context))).thenReturn(versions);
        when(doc.getRevisionInfo(eq("1"), eq(context))).thenReturn(info1);
        when(doc.getRevisionInfo(eq("2"), eq(context))).thenReturn(info2);
        when(doc.getRevisionInfo(eq("3"), eq(context))).thenReturn(info3);
        when(info1.getComment()).thenReturn("");
        when(info2.getComment()).thenReturn("(Autosaved)");
        when(info3.getComment()).thenReturn("");
        when(info1.isMinorEdit()).thenReturn(true);
        when(info2.isMinorEdit()).thenReturn(false);
        when(info3.isMinorEdit()).thenReturn(false);

        EntityReference reference = new EntityReference("defaultpage",
            EntityType.DOCUMENT, new EntityReference("defaultspace", EntityType.SPACE,
            new EntityReference("defaultwiki", EntityType.WIKI)));
        DocumentReference docReference = mock(DocumentReference.class);
        XWikiResponse response = mock(XWikiResponse.class);
        EntityReferenceResolver<EntityReference> referenceResolver =
            mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "current");
        when(wiki.getDefaultPage(context)).thenReturn("test");
        when(wiki.getDefaultSpace(context)).thenReturn("test");
        when(referenceResolver.resolve(any(EntityReference.class), any(EntityType.class))).thenReturn(reference);
        when(wiki.getURL(docReference, "view", context)).thenReturn("");
        when(context.getResponse()).thenReturn(response);
        doNothing().when(response).sendRedirect(anyString());

        deleterSpy.onEvent(event, null, null);
        verify(wiki, times(1)).deleteDocument(doc, true, context);
    }

    @Test
    public void testUserSaveOnly() throws XWikiException, ComponentLookupException, IOException
    {
        PatientOnCancelDeleter deleterSpy = spy(mocker.getComponentUnderTest());
        List<String> versions = new LinkedList<>();
        XWikiRCSNodeInfo info1 = mock(XWikiRCSNodeInfo.class);
        versions.add("1");

        when(doc.getRevisions(any(RevisionCriteria.class), eq(context))).thenReturn(versions);
        when(doc.getRevisionInfo(eq("1"), eq(context))).thenReturn(info1);
        when(info1.getComment()).thenReturn("");
        when(info1.isMinorEdit()).thenReturn(false);

        EntityReference reference = new EntityReference("defaultpage",
            EntityType.DOCUMENT, new EntityReference("defaultspace", EntityType.SPACE,
            new EntityReference("defaultwiki", EntityType.WIKI)));
        DocumentReference docReference = mock(DocumentReference.class);
        XWikiResponse response = mock(XWikiResponse.class);
        EntityReferenceResolver<EntityReference> referenceResolver =
            mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "current");
        when(wiki.getDefaultPage(context)).thenReturn("test");
        when(wiki.getDefaultSpace(context)).thenReturn("test");
        when(referenceResolver.resolve(any(EntityReference.class), any(EntityType.class))).thenReturn(reference);
        when(wiki.getURL(docReference, "view", context)).thenReturn("");
        when(context.getResponse()).thenReturn(response);
        doNothing().when(response).sendRedirect(anyString());

        deleterSpy.onEvent(event, null, null);
        verify(wiki, times(1)).deleteDocument(doc, true, context);
    }

    @Test
    public void failedRedirect() throws XWikiException, ComponentLookupException, IOException
    {
        PatientOnCancelDeleter deleterSpy = spy(mocker.getComponentUnderTest());
        List<String> versions = new LinkedList<>();
        XWikiRCSNodeInfo info1 = mock(XWikiRCSNodeInfo.class);
        versions.add("1");

        when(doc.getRevisions(any(RevisionCriteria.class), eq(context))).thenReturn(versions);
        when(doc.getRevisionInfo(eq("1"), eq(context))).thenReturn(info1);
        when(info1.getComment()).thenReturn("");
        when(info1.isMinorEdit()).thenReturn(false);

        EntityReference reference = new EntityReference("defaultpage",
            EntityType.DOCUMENT, new EntityReference("defaultspace", EntityType.SPACE,
            new EntityReference("defaultwiki", EntityType.WIKI)));
        DocumentReference docReference = mock(DocumentReference.class);
        XWikiResponse response = mock(XWikiResponse.class);
        EntityReferenceResolver<EntityReference> referenceResolver =
            mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "current");
        when(wiki.getDefaultPage(context)).thenReturn("test");
        when(wiki.getDefaultSpace(context)).thenReturn("test");
        when(referenceResolver.resolve(any(EntityReference.class), any(EntityType.class))).thenReturn(reference);
        when(wiki.getURL(docReference, "view", context)).thenReturn("");
        when(context.getResponse()).thenReturn(response);
        doThrow(IOException.class).when(response).sendRedirect(anyString());

        deleterSpy.onEvent(event, null, null);
        verify(wiki, times(1)).deleteDocument(doc, true, context);
        /* cannot verify logging action */
    }
}
