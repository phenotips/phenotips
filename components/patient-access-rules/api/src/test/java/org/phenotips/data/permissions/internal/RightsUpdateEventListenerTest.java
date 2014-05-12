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
package org.phenotips.data.permissions.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link RightsUpdateEventListener}
 *
 * @version $Id$
 */
public class RightsUpdateEventListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<RightsUpdateEventListener> mocker =
        new MockitoComponentMockingRule<RightsUpdateEventListener>(RightsUpdateEventListener.class);

    public XWikiDocument doc = mock(XWikiDocument.class);

    public XWikiContext context = mock(XWikiContext.class);

    public RightsUpdateEventListener testComponent;

    @Before
    public void setUp() throws ComponentLookupException
    {
        testComponent = mocker.getComponentUnderTest();
    }
    /** Basic test for {@link RightsUpdateEventListener#findRights} */
    @Test
    public void findRightsTest() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Class[] args = new Class[1];
        args[0] = XWikiDocument.class;
        Method testMethod = testComponent.getClass().getDeclaredMethod("findRights", args);
        testMethod.setAccessible(true);

        List<BaseObject> mockRightObjects = mock(List.class);
        Iterator<BaseObject> mockRightIterator = mock(Iterator.class);
        BaseObject mockRightObject = mock(BaseObject.class);
        when(doc.getXObjects(any(EntityReference.class))).thenReturn(mockRightObjects);
        when(mockRightObjects.iterator()).thenReturn(mockRightIterator);
        when(mockRightIterator.hasNext()).thenReturn(true, false);
        when(mockRightIterator.next()).thenReturn(mockRightObject);

        testMethod.invoke(testComponent, doc);
    }
}
