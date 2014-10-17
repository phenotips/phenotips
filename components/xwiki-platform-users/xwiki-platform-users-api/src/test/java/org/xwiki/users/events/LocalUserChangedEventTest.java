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
package org.xwiki.users.events;

import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.users.User;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalUserChangedEventTest
{
    @Mock
    private User user;

    @Mock
    private User author;

    private DocumentReference pdoc = new DocumentReference("instance", "Users", "jdoe");

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(this.user.getProfileDocument()).thenReturn(this.pdoc);
    }

    @Test
    public void getEventType()
    {
        LocalUserChangedEvent evt = new LocalUserChangedEvent();
        Assert.assertEquals("localUserChanged", evt.getEventType());

        evt = new LocalUserChangedEvent(this.user, this.author);
        Assert.assertEquals("localUserChanged", evt.getEventType());
    }

    @Test
    public void getUser()
    {
        LocalUserChangedEvent evt = new LocalUserChangedEvent();
        Assert.assertNull(evt.getUser());

        evt = new LocalUserChangedEvent(this.user, this.author);
        Assert.assertEquals(this.user, evt.getUser());
    }

    @Test
    public void getAuthor()
    {
        LocalUserChangedEvent evt = new LocalUserChangedEvent();
        Assert.assertNull(evt.getAuthor());

        evt = new LocalUserChangedEvent(this.user, this.author);
        Assert.assertEquals(this.author, evt.getAuthor());
    }

    @Test
    public void matches()
    {
        LocalUserChangedEvent evt1 = new LocalUserChangedEvent();
        Assert.assertTrue(evt1.matches(evt1));

        LocalUserChangedEvent evt2 = new LocalUserChangedEvent(this.user, this.author);
        Assert.assertTrue(evt1.matches(evt2));
        Assert.assertFalse(evt2.matches(evt1));

        User p2 = mock(User.class);
        when(p2.getProfileDocument()).thenReturn(new DocumentReference("instance", "Users", "padams"));
        LocalUserChangedEvent evt3 = new LocalUserChangedEvent(p2, this.author);
        Assert.assertTrue(evt1.matches(evt3));
        Assert.assertFalse(evt2.matches(evt3));

        LocalUserChangingEvent evt4 = new LocalUserChangingEvent(this.user, this.author);
        Assert.assertFalse(evt1.matches(evt4));
        Assert.assertFalse(evt2.matches(evt4));

        DocumentUpdatedEvent evt5 = new DocumentUpdatedEvent(this.pdoc);
        Assert.assertFalse(evt1.matches(evt5));
        Assert.assertFalse(evt2.matches(evt5));
    }
}
