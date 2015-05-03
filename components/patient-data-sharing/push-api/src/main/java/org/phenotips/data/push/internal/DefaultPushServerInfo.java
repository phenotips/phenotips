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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
