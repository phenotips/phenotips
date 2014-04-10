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

import org.phenotips.data.push.PatientPushHistory;
import org.phenotips.data.securestorage.PatientPushedToInfo;

import java.sql.Timestamp;

/**
 * Default implementation.
 *
 * @version $Id$
 * @since 1.0M11
 */
public class DefaultPatientPushHistory implements PatientPushHistory
{
    // TODO: List<Timestamp> of all the times this patient was pushed?
    /** Last time this patient was pushed to this server. */
    private final Timestamp lastTimePushed;

    /** The ID on the remote server as of last push (in theory may change - GUID should be used for updating). */
    private final String remotePatientID;

    /**
     * URL as of last push. In practice this can be reconstructed from the ID, but is stored to account for possibly
     * differently configured remote server.
     */
    private final String remotePatientURL;

    /** Supposedly never changes. May be {@code null} if the remote server does not provide a GUID. */
    private final String remotePatientGUID;

    /**
     * Constructor that copies the information from a persisted entity.
     *
     * @param pushInfo the stored data to copy, must be non-null
     */
    public DefaultPatientPushHistory(PatientPushedToInfo pushInfo)
    {
        this.lastTimePushed = pushInfo.getLastPushTime();
        this.remotePatientGUID = pushInfo.getRemotePatientGUID();
        this.remotePatientID = pushInfo.getRemotePatientID();
        this.remotePatientURL = pushInfo.getRemotePatientURL();
    }

    /**
     * Constructor that receives all the relevant information as parameters.
     *
     * @param lastTimePushed see {@link #getLastPushTime()}
     * @param remotePatientGUID see {@link #getRemotePatientGUID()}
     * @param remotePatientID see {@link #getRemotePatientID()}
     * @param remotePatientURL see {@link #getRemotePatientURL()}
     */
    public DefaultPatientPushHistory(Timestamp lastTimePushed, String remotePatientGUID, String remotePatientID,
        String remotePatientURL)
    {
        this.lastTimePushed = lastTimePushed;
        this.remotePatientGUID = remotePatientGUID;
        this.remotePatientID = remotePatientID;
        this.remotePatientURL = remotePatientURL;
    }

    @Override
    public Timestamp getLastPushTime()
    {
        return this.lastTimePushed;
    }

    @Override
    public long getLastPushAgeInDays()
    {
        long diffInMilliseconds = System.currentTimeMillis() - getLastPushTime().getTime();
        return (diffInMilliseconds / (1000 * 60 * 60 * 24));
    }

    @Override
    public long getLastPushAgeInHours()
    {
        long diffInMilliseconds = System.currentTimeMillis() - getLastPushTime().getTime();
        return (diffInMilliseconds / (1000 * 60 * 60));
    }

    @Override
    public String getRemotePatientGUID()
    {
        return this.remotePatientGUID;
    }

    @Override
    public String getRemotePatientID()
    {
        return this.remotePatientID;
    }

    @Override
    public String getRemotePatientURL()
    {
        return this.remotePatientURL;
    }
}
