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
package org.phenotips.configuration.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.configuration.internal.configured.ConfiguredRecordConfiguration;
import org.phenotips.configuration.internal.global.GlobalRecordConfiguration;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link RecordConfigurationManager} implementation, {@link GlobalRecordConfiguration}.
 *
 * @version $Id$
 */
public class DefaultRecordConfigurationManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<RecordConfigurationManager> mocker =
        new MockitoComponentMockingRule<RecordConfigurationManager>(DefaultRecordConfigurationManager.class);

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns a custom configuration when there's an
     * explicit binding in the current document.
     */
    @Test
    public void getActiveConfigurationWithBoundConfiguration() throws ComponentLookupException, XWikiException
    {
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference currentDocument = new DocumentReference("xwiki", "data", "P0000001");
        DocumentReference bindingClass = new DocumentReference("xwiki", "PhenoTips", "StudyBindingClass");
        DocumentReference gr = new DocumentReference("xwiki", "Groups", "Dentists");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocument);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(DefaultRecordConfigurationManager.STUDY_BINDING_CLASS_REFERENCE))
            .thenReturn(bindingClass);
        when(dab.getProperty(currentDocument, bindingClass, "studyReference")).thenReturn("Groups.Dentists");
        DocumentReferenceResolver<String> referenceParser =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(referenceParser.resolve("Groups.Dentists")).thenReturn(gr);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(x.getDocument(gr, context)).thenReturn(doc);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getListValue("sections")).thenReturn(Collections.singletonList("patient_info"));

        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof ConfiguredRecordConfiguration);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns the global configuration when there's an
     * explicit binding in the current document, but reading the custom configuration fails.
     */
    @Test
    public void getActiveConfigurationWithBoundConfigurationAndExceptions() throws ComponentLookupException,
        XWikiException
    {
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference currentDocument = new DocumentReference("xwiki", "data", "P0000001");
        DocumentReference bindingClass = new DocumentReference("xwiki", "PhenoTips", "StudyBindingClass");
        DocumentReference gr = new DocumentReference("xwiki", "Groups", "Dentists");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocument);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(DefaultRecordConfigurationManager.STUDY_BINDING_CLASS_REFERENCE))
            .thenReturn(bindingClass);
        when(dab.getProperty(currentDocument, bindingClass, "studyReference")).thenReturn("Groups.Dentists");
        DocumentReferenceResolver<String> referenceParser =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(referenceParser.resolve("Groups.Dentists")).thenReturn(gr);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        when(x.getDocument(gr, context)).thenThrow(new XWikiException());

        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns the global configuration when there's an
     * explicit binding in the current document, but the document doesn't exist.
     */
    @Test
    public void getActiveConfigurationWithDeletedBoundConfiguration() throws ComponentLookupException,
        XWikiException
    {
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference currentDocument = new DocumentReference("xwiki", "data", "P0000001");
        DocumentReference bindingClass = new DocumentReference("xwiki", "PhenoTips", "StudyBindingClass");
        DocumentReference sr = new DocumentReference("xwiki", "Studies", "Missing");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocument);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(DefaultRecordConfigurationManager.STUDY_BINDING_CLASS_REFERENCE))
            .thenReturn(bindingClass);
        when(dab.getProperty(currentDocument, bindingClass, "studyReference")).thenReturn("Studies.Missing");
        DocumentReferenceResolver<String> referenceParser =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(referenceParser.resolve("Studies.Missing")).thenReturn(sr);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(x.getDocument(sr, context)).thenReturn(doc);
        when(doc.isNew()).thenReturn(true);

        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns the global configuration when there's an
     * explicit binding in the current document, but requesting the document returns null.
     */
    @Test
    public void getActiveConfigurationWithInaccessibleBoundConfiguration() throws ComponentLookupException,
        XWikiException
    {
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference currentDocument = new DocumentReference("xwiki", "data", "P0000001");
        DocumentReference bindingClass = new DocumentReference("xwiki", "PhenoTips", "StudyBindingClass");
        DocumentReference sr = new DocumentReference("xwiki", "Studies", "Inaccessible");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocument);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(DefaultRecordConfigurationManager.STUDY_BINDING_CLASS_REFERENCE))
            .thenReturn(bindingClass);
        when(dab.getProperty(currentDocument, bindingClass, "studyReference")).thenReturn("Studies.Inaccessible");
        DocumentReferenceResolver<String> referenceParser =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(referenceParser.resolve("Studies.Inaccessible")).thenReturn(sr);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        when(x.getDocument(sr, context)).thenReturn(null);

        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns the global configuration when the user
     * doesn't belong to any groups.
     */
    @Test
    public void getDefaultActiveConfiguration() throws ComponentLookupException
    {
        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns a global configuration when reading the group
     * configuration fails when accessing the group.
     */
    @Test
    public void getActiveConfigurationWithExceptions() throws ComponentLookupException, XWikiException
    {
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument doc = mock(XWikiDocument.class);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getListValue("sections")).thenReturn(Collections.singletonList("patient_info"));

        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns a global configuration when reading the group
     * configuration fails when building the group configuration.
     */
    @Test
    public void getActiveConfigurationWithExceptionsOnSecondTry() throws ComponentLookupException, XWikiException
    {
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument doc = mock(XWikiDocument.class);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getListValue("sections")).thenReturn(Collections.singletonList("patient_info"));

        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }
}
