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
package org.phenotips.data.permissions.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link EntityAccessHelper} implementation, {@link DefaultEntityAccessHelper}.
 *
 * @version $Id$
 */
public class DefaultEntityAccessHelperTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String SPACE_NAME = "Xwiki";

    private static final String GROUP_SPACE_NAME = "Groups";

    private static final String COLLABORATORS_NAME = "collaborators";

    private static final String PADAMS = "padams";

    private static final String HMCCOY = "hmccoy";

    private static final String XWIKI_USERS_LABEL = "XWikiUsers";

    private static final String XWIKI_GROUPS_LABEL = "XWikiGroups";

    /** The user used as the owner of the patient. */
    private static final DocumentReference OWNER = new DocumentReference(WIKI_NAME, SPACE_NAME, PADAMS);

    /** The user used as a collaborator. */
    private static final DocumentReference COLLABORATOR = new DocumentReference(WIKI_NAME, SPACE_NAME, HMCCOY);

    /** Group used as collaborator. */
    private static final DocumentReference GROUP = new DocumentReference(WIKI_NAME, GROUP_SPACE_NAME,
        COLLABORATORS_NAME);

    @Rule
    public final MockitoComponentMockingRule<EntityAccessHelper> mocker =
        new MockitoComponentMockingRule<>(DefaultEntityAccessHelper.class);

    @Mock
    private XWikiDocument ownerDoc;

    @Mock
    private XWikiDocument groupDoc;

    @Mock
    private XWikiDocument collaboratorDoc;

    private DocumentAccessBridge bridge;

    private EntityAccessHelper component;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        this.component = this.mocker.getComponentUnderTest();
        this.bridge = this.mocker.getInstance(DocumentAccessBridge.class);

        when(this.bridge.getDocument(OWNER)).thenReturn(this.ownerDoc);
        when(this.bridge.getDocument(GROUP)).thenReturn(this.groupDoc);
        when(this.bridge.getDocument(COLLABORATOR)).thenReturn(this.collaboratorDoc);

        when(this.ownerDoc.getXObject(new EntityReference(XWIKI_USERS_LABEL, EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(mock(BaseObject.class));

        when(this.groupDoc.getXObject(new EntityReference(XWIKI_USERS_LABEL, EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(null);
        when(this.groupDoc.getXObject(new EntityReference(XWIKI_GROUPS_LABEL, EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(mock(BaseObject.class));

        when(this.collaboratorDoc.getXObject(new EntityReference(XWIKI_USERS_LABEL, EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(null);
        when(this.collaboratorDoc.getXObject(new EntityReference(XWIKI_GROUPS_LABEL, EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(null);
    }

    /** Basic tests for {@link EntityAccessHelper#getCurrentUser()}. */
    @Test
    public void getCurrentUser()
    {
        when(this.bridge.getCurrentUserReference()).thenReturn(OWNER);
        Assert.assertSame(OWNER, this.component.getCurrentUser());
    }

    /** {@link EntityAccessHelper#getCurrentUser()} returns null for guests. */
    @Test
    public void getCurrentUserForGuest()
    {
        when(this.bridge.getCurrentUserReference()).thenReturn(null);
        Assert.assertNull(this.component.getCurrentUser());
    }

    /** Basic tests for {@link EntityAccessHelper#getType(EntityReference)}. */
    @Test
    public void getType()
    {
        Assert.assertEquals("user", this.component.getType(OWNER));
        Assert.assertEquals("group", this.component.getType(GROUP));
        Assert.assertEquals("unknown", this.component.getType(COLLABORATOR));
    }

    /** Basic tests for {@link EntityAccessHelper#isGroup(EntityReference)}, when entity reference is a user. */
    @Test
    public void isGroupWithUser()
    {
        Assert.assertFalse(this.component.isGroup(OWNER));
    }

    /** Basic tests for {@link EntityAccessHelper#isGroup(EntityReference)}, when entity reference is a group. */
    @Test
    public void isGroupWithGroup()
    {
        Assert.assertTrue(this.component.isGroup(GROUP));
    }

    /** Basic tests for {@link EntityAccessHelper#isGroup(EntityReference)}, when entity reference is some other doc. */
    @Test
    public void isGroupWithSomeDoc()
    {
        Assert.assertFalse(this.component.isGroup(COLLABORATOR));
    }

    /** Basic tests for {@link EntityAccessHelper#isUser(EntityReference)}, when entity reference is a user. */
    @Test
    public void isUserWithUser()
    {
        Assert.assertTrue(this.component.isUser(OWNER));
    }

    /** Basic tests for {@link EntityAccessHelper#isUser(EntityReference)}, when entity reference is a group. */
    @Test
    public void isUserWithGroup()
    {
        Assert.assertFalse(this.component.isUser(GROUP));
    }

    /** Basic tests for {@link EntityAccessHelper#isUser(EntityReference)}, when entity reference is some other doc. */

    @Test
    public void isUserWithSomeDoc()
    {
        Assert.assertFalse(this.component.isUser(COLLABORATOR));
    }
}
