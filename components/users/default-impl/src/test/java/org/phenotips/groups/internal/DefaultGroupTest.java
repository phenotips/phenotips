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
package org.phenotips.groups.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;
import org.phenotips.studies.data.Study;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryException;
import org.xwiki.users.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/**
 * Tests for the default {@link Group} implementation, {@link DefaultGroup}.
 *
 * @version $Id$
 */
public class DefaultGroupTest
{
    /** The xObject under which members are saved. */
    private static final DocumentReference MEMBERS_REFERENCE = new DocumentReference("xwiki", "XWiki", "XWikiGroups");

    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private GroupManager groupManager;

    @Mock
    private DocumentAccessBridge bridge;

    @Mock
    DocumentReferenceResolver<String> resolver;

    @Mock
    UsersAndGroups usersAndGroups;

    @Mock
    Logger logger;

    @Before
    public void setupComponentManager() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);

        when(this.cm.getInstance(GroupManager.class)).thenReturn(this.groupManager);
        when(this.cm.getInstance(DocumentAccessBridge.class)).thenReturn(bridge);
        when(this.cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current")).thenReturn(this.resolver);
        when(this.cm.getInstance(Logger.class)).thenReturn(this.logger);
        when(this.cm.getInstance(UsersAndGroups.class)).thenReturn(this.usersAndGroups);
    }

    /** Basic tests for {@link DefaultGroup#getReference()}. */
    @Test
    public void getReference() throws ComponentLookupException, QueryException
    {
        Group g = new DefaultGroup(null);
        Assert.assertNull(g.getReference());

        DocumentReference a = new DocumentReference("xwiki", "Groups", "Group A");
        Assert.assertEquals(a, new DefaultGroup(a).getReference());
    }

    /** There's a nicer toString implementation showing the group name. */
    @Test
    public void toStringTest()
    {
        DocumentReference a = new DocumentReference("xwiki", "Groups", "Group A");
        Assert.assertTrue(new DefaultGroup(a).toString().contains("Group A"));
    }

    @Test
    public void equalsTest()
    {
        DocumentReference a1 = new DocumentReference("xwiki", "Groups", "Group A");
        DocumentReference a2 = new DocumentReference("xwiki", "Groups", "Group A");
        DocumentReference a3 = new DocumentReference("xwiki", "Groups", "Group B");
        Assert.assertTrue(new DefaultGroup(a1).equals(new DefaultGroup(a2)));
        Assert.assertFalse(new DefaultGroup(a1).equals(new DefaultGroup(a3)));
        Assert.assertFalse(new DefaultGroup(a1).equals("not a group"));
    }

    @Test
    public void hashCodeTest()
    {
        DocumentReference a = new DocumentReference("xwiki", "Groups", "group A");
        Assert.assertTrue(new DefaultGroup(a).hashCode() == "group A".hashCode());
        Assert.assertFalse(new DefaultGroup(a).hashCode() == "aaa".hashCode());
    }

    @Test
    public void isUserInGroupTest() throws ComponentLookupException, QueryException
    {
        DocumentReference docref = new DocumentReference("xwiki", "Groups", "Group A");
        DefaultGroup groupA = new DefaultGroup(docref);
        User u = mock(User.class);

        when(this.groupManager.getGroupsForUser(u)).thenReturn(null);
        Assert.assertFalse(groupA.isUserInGroup(u));

        Set<Group> groups = new HashSet<Group>();
        when(this.groupManager.getGroupsForUser(u)).thenReturn(groups);
        Assert.assertFalse(groupA.isUserInGroup(u));

        groups.add(groupA);
        Assert.assertTrue(groupA.isUserInGroup(u));

        when(this.cm.getInstance(GroupManager.class)).thenThrow(new ComponentLookupException("failed"));
        boolean fails = false;
        try {
            groupA.isUserInGroup(u);
        } catch (NullPointerException e) {
            fails = true;
        }
        Assert.assertTrue(fails);
    }

    @Test
    public void getStudiesTest() throws Exception
    {
        XWikiDocument groupXDocument = mock(XWikiDocument.class);
        DocumentReference groupReference = mock(DocumentReference.class);
        DefaultGroup group = new DefaultGroup(groupReference);
        when(this.bridge.getDocument(groupReference)).thenReturn(groupXDocument);

        // No studies
        List<String> studiesIdsList = new ArrayList<String>();
        when(groupXDocument.getListValue("studies")).thenReturn(studiesIdsList);
        Assert.assertEquals(group.getStudies().size(), 0);

        // With 2 studies
        String id = "studies.t1";
        studiesIdsList.add(id);
        DocumentReference studyRef = mock(DocumentReference.class);
        XWikiDocument studyXDocumnet = mock(XWikiDocument.class);
        when(this.resolver.resolve(id, Study.DEFAULT_DATA_SPACE)).thenReturn(studyRef);
        when(this.bridge.getDocument(studyRef)).thenReturn(studyXDocumnet);
        when(studyXDocumnet.getDocumentReference()).thenReturn(studyRef);
        when(groupXDocument.getListValue("studies")).thenReturn(studiesIdsList);
        Collection<Study> studies = group.getStudies();
        Assert.assertEquals(studies.size(), 1);
    }

    @Test
    public void getStudiesFailsTest() throws Exception
    {
        DocumentReference groupReference = mock(DocumentReference.class);
        DefaultGroup group = new DefaultGroup(groupReference);
        when(this.bridge.getDocument(groupReference)).thenThrow(new Exception(""));
        Assert.assertNull(group.getStudies());
    }

    @Test
    public void getAllUserNamesTest() throws Exception
    {
        XWikiDocument groupXDocument = mock(XWikiDocument.class);
        DocumentReference groupReference = mock(DocumentReference.class);
        DefaultGroup group = new DefaultGroup(groupReference);
        when(this.bridge.getDocument(groupReference)).thenReturn(groupXDocument);

        when(groupXDocument.getXObjects(MEMBERS_REFERENCE)).thenReturn(null);
        Assert.assertTrue(group.getAllUserNames().isEmpty());

        List<BaseObject> membersList = new LinkedList<BaseObject>();
        when(groupXDocument.getXObjects(MEMBERS_REFERENCE)).thenReturn(membersList);
        Assert.assertTrue(group.getAllUserNames().isEmpty());

        membersList.add(null);

        BaseObject base1 = mock(BaseObject.class);
        StringProperty st1 = mock(StringProperty.class);
        when(base1.getField("member")).thenReturn(st1);
        when(st1.getValue()).thenReturn("");
        membersList.add(base1);

        BaseObject base2 = mock(BaseObject.class);
        StringProperty st2 = mock(StringProperty.class);
        DocumentReference doc2 = mock(DocumentReference.class);
        when(base2.getField("member")).thenReturn(st2);
        when(st2.getValue()).thenReturn("user");
        when(resolver.resolve("user", Group.GROUP_SPACE)).thenReturn(doc2);
        when(this.usersAndGroups.getType(doc2)).thenReturn(UsersAndGroups.USER);
        membersList.add(base2);

        BaseObject base3 = mock(BaseObject.class);
        StringProperty st3 = mock(StringProperty.class);
        DocumentReference doc3 = mock(DocumentReference.class);
        when(base3.getField("member")).thenReturn(st3);
        when(st3.getValue()).thenReturn("group");
        when(resolver.resolve("group", Group.GROUP_SPACE)).thenReturn(doc3);
        when(this.usersAndGroups.getType(doc3)).thenReturn(UsersAndGroups.GROUP);
        membersList.add(base3);

        Assert.assertEquals(1, group.getAllUserNames().size());
    }

    @Test
    public void getComponentsFailsTest() throws ComponentLookupException {
        MockitoAnnotations.initMocks(this);
        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);

        when(this.cm.getInstance(DocumentAccessBridge.class)).thenThrow(new ComponentLookupException(""));
        when(this.cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current")).thenThrow(new ComponentLookupException(""));
        when(this.cm.getInstance(Logger.class)).thenThrow(new ComponentLookupException(""));
        when(this.cm.getInstance(UsersAndGroups.class)).thenThrow(new ComponentLookupException(""));

        DocumentReference groupReference = mock(DocumentReference.class);
        DefaultGroup g = new DefaultGroup(groupReference);
        boolean failed = false;
        try {
            g.getAllUserNames().isEmpty();
        } catch (NullPointerException e) {
            failed = true;
        }
        Assert.assertTrue(failed);
    }
}
