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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

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

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns a custom configuration when there's an
     * explicit binding in the current document.
     */
    @Test
    public void getActiveConfigurationWithBoundConfiguration() throws ComponentLookupException, XWikiException
    {
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference currentDocument = new DocumentReference("xwiki", "data", "P0000001");
        DocumentReference bindingClass = new DocumentReference("xwiki", "PhenoTips", "TemplateBindingClass");
        DocumentReference gr = new DocumentReference("xwiki", "Groups", "Dentists");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocument);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(DefaultRecordConfigurationManager.TEMPLATE_BINDING_CLASS_REFERENCE))
            .thenReturn(bindingClass);
        when(dab.getProperty(currentDocument, bindingClass, "templateReference")).thenReturn("Groups.Dentists");
        DocumentReferenceResolver<String> referenceParser =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(referenceParser.resolve("Groups.Dentists")).thenReturn(gr);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(gr, this.context)).thenReturn(doc);
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
        DocumentReference bindingClass = new DocumentReference("xwiki", "PhenoTips", "TemplateBindingClass");
        DocumentReference gr = new DocumentReference("xwiki", "Groups", "Dentists");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocument);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(DefaultRecordConfigurationManager.TEMPLATE_BINDING_CLASS_REFERENCE))
            .thenReturn(bindingClass);
        when(dab.getProperty(currentDocument, bindingClass, "templateReference")).thenReturn("Groups.Dentists");
        DocumentReferenceResolver<String> referenceParser =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(referenceParser.resolve("Groups.Dentists")).thenReturn(gr);
        when(this.xwiki.getDocument(gr, this.context)).thenThrow(new XWikiException());

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
        DocumentReference bindingClass = new DocumentReference("xwiki", "PhenoTips", "TemplateBindingClass");
        DocumentReference sr = new DocumentReference("xwiki", "Templates", "Missing");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocument);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(DefaultRecordConfigurationManager.TEMPLATE_BINDING_CLASS_REFERENCE))
            .thenReturn(bindingClass);
        when(dab.getProperty(currentDocument, bindingClass, "templateReference")).thenReturn("Templates.Missing");
        DocumentReferenceResolver<String> referenceParser =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(referenceParser.resolve("Templates.Missing")).thenReturn(sr);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(sr, this.context)).thenReturn(doc);
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
        DocumentReference bindingClass = new DocumentReference("xwiki", "PhenoTips", "TemplateBindingClass");
        DocumentReference sr = new DocumentReference("xwiki", "Templates", "Inaccessible");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocument);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(DefaultRecordConfigurationManager.TEMPLATE_BINDING_CLASS_REFERENCE))
            .thenReturn(bindingClass);
        when(dab.getProperty(currentDocument, bindingClass, "templateReference")).thenReturn("Templates.Inaccessible");
        DocumentReferenceResolver<String> referenceParser =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(referenceParser.resolve("Templates.Inaccessible")).thenReturn(sr);
        when(this.xwiki.getDocument(sr, this.context)).thenReturn(null);

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
        XWikiDocument doc = mock(XWikiDocument.class);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getListValue("sections")).thenReturn(Collections.singletonList("patient_info"));

        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }
}
