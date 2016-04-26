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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Rule;
import org.junit.Test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class UsersAndGroupsTest
{
    private static final EntityReference USER_CLASS = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final EntityReference GROUP_CLASS = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    @Rule
    public final MockitoComponentMockingRule<UsersAndGroups> mocker =
    new MockitoComponentMockingRule<UsersAndGroups>(UsersAndGroups.class);

    @Test
    public void getTypeTest() throws Exception
    {
        DocumentReference groupDocument = new DocumentReference("xwiki", "Groups", "g1");
        XWikiDocument groupXDocument = mock(XWikiDocument.class);
        org.mockito.Mockito.when(groupXDocument.getXObject(GROUP_CLASS)).thenReturn(new BaseObject());
        DocumentAccessBridge bridge = this.mocker.getInstance(DocumentAccessBridge.class);
        when(bridge.getDocument(groupDocument)).thenReturn(groupXDocument);
        UsersAndGroups usersAndGroups = this.mocker.getComponentUnderTest();
        org.junit.Assert.assertEquals(UsersAndGroups.GROUP, usersAndGroups.getType(groupDocument));

        DocumentReference userDocument = new DocumentReference("xwiki", "XWiki", "u1");
        XWikiDocument userXDocument = mock(XWikiDocument.class);
        org.mockito.Mockito.when(userXDocument.getXObject(USER_CLASS)).thenReturn(new BaseObject());
        when(bridge.getDocument(userDocument)).thenReturn(userXDocument);
        org.junit.Assert.assertEquals(UsersAndGroups.USER, usersAndGroups.getType(userDocument));

        DocumentReference unknownDocument = new DocumentReference("xwiki", "qwerty", "qwerty");
        XWikiDocument unknownXDocument = mock(XWikiDocument.class);
        when(bridge.getDocument(unknownDocument)).thenReturn(unknownXDocument);
        org.junit.Assert.assertEquals(UsersAndGroups.UNKNOWN, usersAndGroups.getType(unknownDocument));

        DocumentReference mock = mock(DocumentReference.class);
        when(bridge.getDocument(mock)).thenThrow(new Exception("Failed"));
        org.junit.Assert.assertEquals(UsersAndGroups.UNKNOWN, usersAndGroups.getType(mock));
    }
}