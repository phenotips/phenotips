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
package org.phenotips.security.audit;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.users.User;

import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link AuditEvent} class.
 *
 * @version $Id$
 */
public class AuditEventTest
{
    @Mock
    private User user;

    private DocumentReference doc = new DocumentReference("wiki", "Space", "Page");

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        when(this.user.getId()).thenReturn("wiki:XWiki.user");
    }

    @Test
    public void eventStoresConstructorParameters()
    {
        Calendar c = Calendar.getInstance();
        AuditEvent e = new AuditEvent(this.user, "ip", "action", "extra", this.doc, c);
        Assert.assertSame(this.user, e.getUser());
        Assert.assertEquals("ip", e.getIp());
        Assert.assertEquals("action", e.getAction());
        Assert.assertEquals("extra", e.getExtraInformation());
        Assert.assertSame(this.doc, e.getEntity());
        Assert.assertSame(c, e.getTime());
    }

    @Test
    public void eventConstructorAcceptsNullParameters()
    {
        AuditEvent e = new AuditEvent(null, null, null, null, null, null);
        Assert.assertNull(e.getUser());
        Assert.assertNull(e.getIp());
        Assert.assertNull(e.getAction());
        Assert.assertNull(e.getExtraInformation());
        Assert.assertNull(e.getEntity());
        Assert.assertNull(e.getTime());
    }

    @Test
    public void eventHasCustomToString()
    {
        Calendar c = Calendar.getInstance();
        AuditEvent e = new AuditEvent(this.user, "ip", "action", "extra", this.doc, c);
        Assert.assertEquals("wiki:XWiki.user (ip): action on wiki:Space.Page at " + c.getTime(), e.toString());
    }

    @Test
    public void toStringWithNullData()
    {
        AuditEvent e = new AuditEvent();
        Assert.assertEquals("null (null): null on null at null", e.toString());
    }

    @Test
    public void eventHasCustomEquals()
    {
        Calendar c = Calendar.getInstance();
        AuditEvent e1 = new AuditEvent(this.user, "ip", "action", "extra", this.doc, c);
        AuditEvent e2 = new AuditEvent(this.user, "ip", "action", "extra", this.doc, c);
        Assert.assertEquals(e1, e2);

        User u = Mockito.mock(User.class);
        when(u.getId()).thenReturn("wiki:XWiki.otherUser");
        e2 = new AuditEvent(u, "ip", "action", "extra", this.doc, c);
        Assert.assertNotEquals(e1, e2);
        e2 = new AuditEvent(this.user, "ip2", "action", "extra", this.doc, c);
        Assert.assertNotEquals(e1, e2);
        e2 = new AuditEvent(this.user, "ip", "action2", "extra", this.doc, c);
        Assert.assertNotEquals(e1, e2);
        e2 = new AuditEvent(this.user, "ip", "action", "extra2", this.doc, c);
        Assert.assertNotEquals(e1, e2);
        e2 = new AuditEvent(this.user, "ip", "action", "extra", new DocumentReference("wiki", "Space", "OtherPage"), c);
        Assert.assertNotEquals(e1, e2);
        e2 = new AuditEvent(this.user, "ip", "action", "extra", this.doc, SecurityTestUtils.getCalendar(10));
        Assert.assertNotEquals(e1, e2);

        Assert.assertNotEquals(new AuditEvent(), e1);
        Assert.assertEquals(new AuditEvent(), new AuditEvent());
    }

    @Test
    public void eventHasCustomHashCode()
    {
        Calendar c = Calendar.getInstance();
        AuditEvent e1 = new AuditEvent(this.user, "ip", "action", "extra", this.doc, c);
        AuditEvent e2 = new AuditEvent(this.user, "ip", "action", "extra", this.doc, c);
        Assert.assertEquals(e1.hashCode(), e2.hashCode());

        User u = Mockito.mock(User.class);
        when(u.getId()).thenReturn("wiki:XWiki.otherUser");
        e2 = new AuditEvent(u, "ip", "action", "extra", this.doc, c);
        Assert.assertNotEquals(e1.hashCode(), e2.hashCode());
        e2 = new AuditEvent(this.user, "ip2", "action", "extra", this.doc, c);
        Assert.assertNotEquals(e1.hashCode(), e2.hashCode());
        e2 = new AuditEvent(this.user, "ip", "action2", "extra", this.doc, c);
        Assert.assertNotEquals(e1.hashCode(), e2.hashCode());
        e2 = new AuditEvent(this.user, "ip", "action", "extra2", this.doc, c);
        Assert.assertNotEquals(e1.hashCode(), e2.hashCode());
        e2 = new AuditEvent(this.user, "ip", "action", "extra", new DocumentReference("wiki", "Space", "OtherPage"), c);
        Assert.assertNotEquals(e1.hashCode(), e2.hashCode());
        e2 = new AuditEvent(this.user, "ip", "action", "extra", this.doc, SecurityTestUtils.getCalendar(10));
        Assert.assertNotEquals(e1.hashCode(), e2.hashCode());

        Assert.assertNotEquals(new AuditEvent().hashCode(), e1.hashCode());
        Assert.assertEquals(new AuditEvent().hashCode(), new AuditEvent().hashCode());
    }

    @Test
    public void toJSONOutputsAllInfo()
    {
        Calendar c = Calendar.getInstance();
        AuditEvent e = new AuditEvent(this.user, "127.0.0.1", "view", "extra", this.doc, c);
        JSONObject result = e.toJSON();
        Assert.assertEquals("wiki:XWiki.user", result.get("user"));
        Assert.assertEquals("127.0.0.1", result.get("ip"));
        Assert.assertEquals("view", result.get("action"));
        Assert.assertEquals("extra", result.get("extra"));
        Assert.assertEquals("wiki:Space.Page", result.get("entity"));
        Assert.assertEquals(DateTimeFormatter.ISO_INSTANT.format(c.toInstant()), result.get("time"));
    }

    @Test
    public void toJSONSkipsUnknownInformation()
    {
        AuditEvent e = new AuditEvent(null, null, null, null, null, null);
        JSONObject result = e.toJSON();
        Assert.assertEquals(0, result.length());
    }
}
