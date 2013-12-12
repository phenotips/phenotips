/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.configuration.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.configuration.internal.configured.ConfiguredRecordConfiguration;
import org.phenotips.configuration.internal.global.GlobalRecordConfiguration;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
        DocumentReference bindingClass = new DocumentReference("xwiki", "PhenoTips", "FormCustomizationBindingClass");
        DocumentReference gr = new DocumentReference("xwiki", "Groups", "Dentists");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocument);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(DefaultRecordConfigurationManager.CUSTOMIZATION_BINDING_CLASS_REFERENCE))
            .thenReturn(bindingClass);
        when(dab.getProperty(currentDocument, bindingClass, "configReference")).thenReturn("Groups.Dentists");
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
        DocumentReference bindingClass = new DocumentReference("xwiki", "PhenoTips", "FormCustomizationBindingClass");
        DocumentReference gr = new DocumentReference("xwiki", "Groups", "Dentists");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocument);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(DefaultRecordConfigurationManager.CUSTOMIZATION_BINDING_CLASS_REFERENCE))
            .thenReturn(bindingClass);
        when(dab.getProperty(currentDocument, bindingClass, "configReference")).thenReturn("Groups.Dentists");
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
        when(x.getDocument(gr, context)).thenThrow(new XWikiException());

        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns the global configuration when the user
     * doesn't belong to any groups.
     */
    @Test
    public void getActiveConfigurationWithNoUserGroups() throws ComponentLookupException
    {
        UserManager um = this.mocker.getInstance(UserManager.class);
        User u = mock(User.class);
        when(um.getCurrentUser()).thenReturn(u);
        GroupManager gm = this.mocker.getInstance(GroupManager.class);
        when(gm.getGroupsForUser(u)).thenReturn(Collections.<Group> emptySet());
        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);

        when(gm.getGroupsForUser(u)).thenReturn(null);
        result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns the global configuration when the user only
     * belongs to groups without a configuration override.
     */
    @Test
    public void getActiveConfigurationWithUnconfiguredUserGroups() throws ComponentLookupException, XWikiException
    {
        UserManager um = this.mocker.getInstance(UserManager.class);
        User u = mock(User.class);
        when(um.getCurrentUser()).thenReturn(u);
        GroupManager gm = this.mocker.getInstance(GroupManager.class);
        Group g = mock(Group.class);
        DocumentReference gr = new DocumentReference("xwiki", "Groups", "Dentists");
        when(g.getReference()).thenReturn(gr);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(x.getDocument(gr, context)).thenReturn(doc);
        Set<Group> groups = new HashSet<Group>();
        groups.add(g);

        g = mock(Group.class);
        gr = new DocumentReference("xwiki", "Groups", "Interns");
        when(g.getReference()).thenReturn(gr);
        doc = mock(XWikiDocument.class);
        when(x.getDocument(gr, context)).thenReturn(doc);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getListValue("sections")).thenReturn(Collections.emptyList());
        groups.add(g);

        when(gm.getGroupsForUser(u)).thenReturn(groups);
        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns a custom configuration when the user belongs
     * to a configured group.
     */
    @Test
    public void getActiveConfigurationWithConfiguredGroup() throws ComponentLookupException, XWikiException
    {
        UserManager um = this.mocker.getInstance(UserManager.class);
        User u = mock(User.class);
        when(um.getCurrentUser()).thenReturn(u);
        GroupManager gm = this.mocker.getInstance(GroupManager.class);
        Group g = mock(Group.class);
        DocumentReference gr = new DocumentReference("xwiki", "Groups", "Dentists");
        when(g.getReference()).thenReturn(gr);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(x.getDocument(gr, context)).thenReturn(doc);
        Set<Group> groups = new HashSet<Group>();
        groups.add(g);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getListValue("sections")).thenReturn(Collections.singletonList("patient_info"));

        when(gm.getGroupsForUser(u)).thenReturn(groups);
        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof ConfiguredRecordConfiguration);
    }

    /**
     * {@link RecordConfigurationManager#getActiveConfiguration()} returns a global configuration when reading the group
     * configuration fails when accessing the group.
     */
    @Test
    public void getActiveConfigurationWithExceptions() throws ComponentLookupException, XWikiException
    {
        UserManager um = this.mocker.getInstance(UserManager.class);
        User u = mock(User.class);
        when(um.getCurrentUser()).thenReturn(u);
        GroupManager gm = this.mocker.getInstance(GroupManager.class);
        Group g = mock(Group.class);
        DocumentReference gr = new DocumentReference("xwiki", "Groups", "Dentists");
        when(g.getReference()).thenReturn(gr);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(x.getDocument(gr, context)).thenThrow(new XWikiException());
        Set<Group> groups = new HashSet<Group>();
        groups.add(g);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getListValue("sections")).thenReturn(Collections.singletonList("patient_info"));

        when(gm.getGroupsForUser(u)).thenReturn(groups);
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
        UserManager um = this.mocker.getInstance(UserManager.class);
        User u = mock(User.class);
        when(um.getCurrentUser()).thenReturn(u);
        GroupManager gm = this.mocker.getInstance(GroupManager.class);
        Group g = mock(Group.class);
        DocumentReference gr = new DocumentReference("xwiki", "Groups", "Dentists");
        when(g.getReference()).thenReturn(gr);
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        XWikiContext context = mock(XWikiContext.class);
        when(ec.getProperty("xwikicontext")).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(x.getDocument(gr, context)).thenReturn(doc).thenThrow(new XWikiException());
        Set<Group> groups = new HashSet<Group>();
        groups.add(g);
        BaseObject o = mock(BaseObject.class);
        when(doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS)).thenReturn(o);
        when(o.getListValue("sections")).thenReturn(Collections.singletonList("patient_info"));

        when(gm.getGroupsForUser(u)).thenReturn(groups);
        RecordConfiguration result = this.mocker.getComponentUnderTest().getActiveConfiguration();
        Assert.assertTrue(result instanceof GlobalRecordConfiguration);
    }
}
