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
package org.phenotips.data.securestorage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Used to store given local user's remote credentials on the given remote server (which can be used for more convenient
 * passwordless logins and pre-filled username fields).
 *
 * @version $Id$
 * @since 1.0M10
 */
@Entity
public class RemoteLoginData
{
    /** @see #getId() */
    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    private String localUserName;

    @Column(nullable = false)
    private String serverName;

    @Column(nullable = false)
    private String remoteUserName;

    private String loginToken; // may be null when tokens are disabled. In that case username is stored only to pre-populate UI user/password boxes

    /** Default constructor used by Hibernate. */
    protected RemoteLoginData()
    {
        // Nothing to do, Hibernate will populate all the fields from the database
    }

    /**
     * For use by SecureStorageManager
     */
    public RemoteLoginData(String localUserName, String serverName, String remoteUserName, String loginToken)
    {
        this.localUserName = localUserName;
        this.serverName = serverName;
        this.remoteUserName = remoteUserName;
        this.loginToken = loginToken;
    }

    public String getLoginToken()
    {
        return this.loginToken;
    }

    public void setLoginToken(String newToken)
    {
        this.loginToken = newToken;
    }

    public String getRemoteUserName()
    {
        return this.remoteUserName;
    }

    public void setRemoteUserName(String remoteUserName)
    {
        this.remoteUserName = remoteUserName;
    }

    public String getLocalUserName()
    {
        return this.localUserName;
    }

    public String getServerName()
    {
        return this.serverName;
    }
}
