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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

    private static final String PADAMS = "padams";

    private static final String HMCCOY = "hmccoy";

    /** The user used as the owner of the patient. */
    private static final DocumentReference OWNER = new DocumentReference(WIKI_NAME, SPACE_NAME, PADAMS);

    /** The user used as a collaborator. */
    private static final DocumentReference COLLABORATOR = new DocumentReference(WIKI_NAME, SPACE_NAME, HMCCOY);

    /** Group used as collaborator. */
    private static final DocumentReference GROUP = new DocumentReference(WIKI_NAME, SPACE_NAME, "collaborators");

    @Rule
    public final MockitoComponentMockingRule<EntityAccessHelper> mocker =
        new MockitoComponentMockingRule<>(DefaultEntityAccessHelper.class);

    private DocumentAccessBridge bridge;

    @Before
    public void setup() throws ComponentLookupException
    {
        this.bridge = this.mocker.getInstance(DocumentAccessBridge.class);
    }

    /** Basic tests for {@link EntityAccessHelper#getCurrentUser()}. */
    @Test
    public void getCurrentUser() throws ComponentLookupException
    {
        when(this.bridge.getCurrentUserReference()).thenReturn(OWNER);
        Assert.assertSame(OWNER, this.mocker.getComponentUnderTest().getCurrentUser());
    }

    /** {@link EntityAccessHelper#getCurrentUser()} returns null for guests. */
    @Test
    public void getCurrentUserForGuest() throws ComponentLookupException
    {
        when(this.bridge.getCurrentUserReference()).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getCurrentUser());
    }

    /** Basic tests for {@link EntityAccessHelper#getType(EntityReference)}. */
    @Test
    public void getType() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(OWNER)).thenReturn(doc);
        when(doc.getXObject(new EntityReference("XWikiUsers", EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(mock(BaseObject.class));

        doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(GROUP)).thenReturn(doc);
        when(doc.getXObject(new EntityReference("XWikiUsers", EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(null);
        when(doc.getXObject(new EntityReference("XWikiGroups", EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(mock(BaseObject.class));

        doc = mock(XWikiDocument.class);
        when(this.bridge.getDocument(COLLABORATOR)).thenReturn(doc);
        when(doc.getXObject(new EntityReference("XWikiUsers", EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(null);
        when(doc.getXObject(new EntityReference("XWikiGroups", EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE)))).thenReturn(null);

        Assert.assertEquals("user", this.mocker.getComponentUnderTest().getType(OWNER));
        Assert.assertEquals("group", this.mocker.getComponentUnderTest().getType(GROUP));
        Assert.assertEquals("unknown", this.mocker.getComponentUnderTest().getType(COLLABORATOR));
    }
}
