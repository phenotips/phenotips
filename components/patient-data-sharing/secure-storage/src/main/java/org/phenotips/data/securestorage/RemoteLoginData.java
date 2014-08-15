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
     *
     * @param
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
