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
 * Stores information about previous pushes of patient data to a given remote server.
 * <p>
 * Stored data includes the last know patient URL on the remote server (for quick linking in UI)
 *
 * @version $Id$
 * @since 1.0M11
 */
@Entity
public class PatientPushedToInfo
{
    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    private String localPatientID;

    @Column(nullable = false)
    private String remoteServerName;

    @Column(nullable = false)
    private Timestamp lastTimePushed; // last time this patient was pushed to this server

    @Column(nullable = false)
    private String remotePatientID; // ID as of last push (in theory may change - GUID should be used for updating)

    private String remotePatientURL; // URL as of last push. In practice can recostruct form ID, but stored
    // to acount for possibly differently configured remote server

    private String remotePatientGUID; // supposedly never changes; nullable: in case remote server does not provide a GUID

    /** Default constructor used by Hibernate. */
    protected PatientPushedToInfo()
    {
        // Nothing to do, Hibernate will populate all the fields from the database
    }

    /**
     * Used by the SecureStorageManager
     *
     * @param
     */
    public PatientPushedToInfo(String patientID, String remoteServerName,
        String remotePatientGUID, String remotePatientID, String remotePatientURL)
    {
        this.localPatientID = patientID;
        this.remoteServerName = remoteServerName;
        this.remotePatientGUID = remotePatientGUID;
        this.remotePatientID = remotePatientID;
        this.remotePatientURL = remotePatientURL;
        this.setLastPushTimeToNow();
    }

    public String getRemoteServerName()
    {
        return this.remoteServerName;
    }

    public void setLastPushTimeToNow()
    {
        this.lastTimePushed = new Timestamp(System.currentTimeMillis());
    }

    public Timestamp getLastPushTime()
    {
        return this.lastTimePushed;
    }

    public long getLastPushAgeInDays()
    {
        long diffInMilliseconds = System.currentTimeMillis() - getLastPushTime().getTime();
        return (diffInMilliseconds / (1000 * 60 * 60 * 24));
    }

    public String getRemotePatientGUID()
    {
        return this.remotePatientGUID;
    }

    public void setRemotePatientGUID(String remotePatientGUID)
    {
        this.remotePatientGUID = remotePatientGUID;
    }

    public String getRemotePatientID()
    {
        return this.remotePatientID;
    }

    public void setRemotePatientID(String remotePatientID)
    {
        this.remotePatientID = remotePatientID;
    }

    public String getRemotePatientURL()
    {
        return this.remotePatientURL;
    }

    public void setRemotePatientURL(String remotePatientURL)
    {
        this.remotePatientURL = remotePatientURL;
    }
}
