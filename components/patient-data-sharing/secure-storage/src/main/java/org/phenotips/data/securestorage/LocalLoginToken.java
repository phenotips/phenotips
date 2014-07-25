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

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Used on the server side to store the login token assigned to a user upon remote "push" login.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Entity
public class LocalLoginToken
{
    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    private String localUserName;

    @Column(nullable = false)
    private String sourceServerName;

    @Column(nullable = false)
    private String loginToken;

    @Column(nullable = false)
    private Timestamp timeTokenCreated;

    /** Default constructor used by Hibernate. */
    protected LocalLoginToken()
    {
        // Nothing to do, Hibernate will populate all the fields from the database
    }

    /**
     * TODO
     *
     * @param
     */
    public LocalLoginToken(String localUserName, String sourceServerName, String loginToken)
    {
        this.localUserName = localUserName;
        this.sourceServerName = sourceServerName;
        this.loginToken = loginToken;
        this.timeTokenCreated = new Timestamp(System.currentTimeMillis());
    }

    public String getLoginToken()
    {
        return this.loginToken;
    }

    public void setLoginToken(String newToken)
    {
        this.loginToken = newToken;
    }

    public String getLocalUserName()
    {
        return this.localUserName;
    }

    public String getSourceServerName()
    {
        return this.sourceServerName;
    }

    public Timestamp getTimeCreated()
    {
        return this.timeTokenCreated;
    }

    public long getTokenAgeInDays()
    {
        long diffInMilliseconds = System.currentTimeMillis() - getTimeCreated().getTime();
        return (diffInMilliseconds / (1000 * 60 * 60 * 24));
    }
}
