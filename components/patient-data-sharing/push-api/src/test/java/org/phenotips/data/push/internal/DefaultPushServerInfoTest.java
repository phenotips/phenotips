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
package org.phenotips.data.push.internal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultPushServerInfoTest
{

    private DefaultPushServerInfo serverInfo;

    @Before
    public void setUp()
    {
        this.serverInfo = new DefaultPushServerInfo("serverId", "serverURL", "serverDescription");
    }

    @Test
    public void getServerIDReturnsCorrectID()
    {
        Assert.assertEquals("serverId", this.serverInfo.getServerID());
    }

    @Test
    public void getServerURLReturnsCorrectURL()
    {
        Assert.assertEquals("serverURL", this.serverInfo.getServerURL());
    }

    @Test
    public void getServerDescriptionReturnsCorrectDescription()
    {
        Assert.assertEquals("serverDescription", this.serverInfo.getServerDescription());
    }

    @Test
    public void compareToIdentifiesSameServerID()
    {
        DefaultPushServerInfo otherInfo = new DefaultPushServerInfo("serverId", "otherURL", "otherDescription");
        Assert.assertEquals(this.serverInfo.compareTo(otherInfo), 0);
    }

    @Test
    public void compareToIdentifiesDifferentServerID()
    {
        DefaultPushServerInfo otherInfo = new DefaultPushServerInfo("otherId", "otherURL", "otherDescription");
        Assert.assertNotEquals(this.serverInfo.compareTo((otherInfo)), 0);
    }

}
