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
package org.phenotips.data.push.internal;

import org.phenotips.data.push.PushServerInfo;

/**
 * Basic implementation of the {@link PushServerInfo} interface.
 *
 * @version $Id$
 * @since 1.0M11
 */
public class DefaultPushServerInfo implements PushServerInfo
{
    private final String serverID;

    private final String serverURL;

    private final String serverDescription;

    DefaultPushServerInfo(String serverID, String serverURL, String serverDescription)
    {
        this.serverID = serverID;
        this.serverURL = serverURL;
        this.serverDescription = serverDescription;
    }

    @Override
    public String getServerID()
    {
        return this.serverID;
    }

    @Override
    public String getServerURL()
    {
        return this.serverURL;
    }

    @Override
    public String getServerDescription()
    {
        return this.serverDescription;
    }

    @Override
    public int compareTo(PushServerInfo other)
    {
        return getServerID().compareTo(other.getServerID());
    }
}
