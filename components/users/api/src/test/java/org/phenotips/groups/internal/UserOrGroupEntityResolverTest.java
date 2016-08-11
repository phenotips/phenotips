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

import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link GroupManager} implementation, {@link DefaultGroupManager}.
 *
 * @version $Id$
 */
public class UserOrGroupEntityResolverTest
{
    private static final EntityReference USERS_SPACE = new EntityReference("XWiki", EntityType.SPACE);

    private static final EntityReference GROUPS_SPACE = new EntityReference("Groups", EntityType.SPACE);

    private static final String USER_ID = "jdoe";

    private static final DocumentReference USER_REF = new DocumentReference("xwiki", "XWiki", USER_ID);

    private static final DocumentReference USER_AS_GROUP_REF = new DocumentReference("xwiki", "Groups", USER_ID);

    private static final String GROUP_ID = "Cardio";

    private static final DocumentReference GROUP_REF = new DocumentReference("xwiki", "Groups", GROUP_ID);

    private static final DocumentReference GROUP_AS_USER_REF = new DocumentReference("xwiki", "XWiki", GROUP_ID);

    @Rule
    public final MockitoComponentMockingRule<DocumentReferenceResolver<String>> mocker =
        new MockitoComponentMockingRule<DocumentReferenceResolver<String>>(UserOrGroupDocumentEntityResolver.class);

    private DocumentReferenceResolver<String> baseResolver;

    private DocumentAccessBridge bridge;

    @Before
    public void setUp() throws ComponentLookupException
    {
        this.baseResolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        this.bridge = this.mocker.getInstance(DocumentAccessBridge.class);
        when(this.baseResolver.resolve(USER_ID, USERS_SPACE)).thenReturn(USER_REF);
        when(this.baseResolver.resolve(USER_ID, GROUPS_SPACE)).thenReturn(USER_AS_GROUP_REF);
        when(this.baseResolver.resolve(GROUP_ID, USERS_SPACE)).thenReturn(GROUP_AS_USER_REF);
        when(this.baseResolver.resolve(GROUP_ID, GROUPS_SPACE)).thenReturn(GROUP_REF);
    }

    @Test
    public void resolveWithValidUserWorks() throws ComponentLookupException
    {
        when(this.bridge.exists(USER_REF)).thenReturn(true);
        Assert.assertSame(USER_REF, this.mocker.getComponentUnderTest().resolve(USER_ID));
    }

    @Test
    public void resolveWithValidGroupWorks() throws ComponentLookupException
    {
        when(this.bridge.exists(GROUP_REF)).thenReturn(true);
        Assert.assertSame(GROUP_REF, this.mocker.getComponentUnderTest().resolve(GROUP_ID));
    }

    @Test
    public void resolveWithInvalidUserReturnsNull() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().resolve(USER_ID));
    }
}
