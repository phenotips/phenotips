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

    private final Timestamp lastTimePushed;   // last time this patient was pushed to this server

    private final String remotePatientID;     // ID as of last push (in theory may change - GUID should be used for updating)

    private final String remotePatientURL;    // URL as of last push. In practice can recostruct form ID, but stored
                                        // to acount for possibly differently configured remote server

    private final String remotePatientGUID;   // supposedly never changes; nullable: in case remote server does not provide a GUID


    public DefaultPatientPushHistory(PatientPushedToInfo pushInfo)
    {
        this.lastTimePushed    = pushInfo.getLastPushTime();
        this.remotePatientGUID = pushInfo.getRemotePatientGUID();
        this.remotePatientID   = pushInfo.getRemotePatientID();
        this.remotePatientURL  = pushInfo.getRemotePatientURL();
    }

    public DefaultPatientPushHistory(Timestamp lastTimePushed, String remotePatientGUID, String remotePatientID, String remotePatientURL)
    {
        this.lastTimePushed    = lastTimePushed;
        this.remotePatientGUID = remotePatientGUID;
        this.remotePatientID   = remotePatientID;
        this.remotePatientURL  = remotePatientURL;
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
    public String getRemotePatientGUID()
    {
        return remotePatientGUID;
    }

    @Override
    public String getRemotePatientID()
    {
        return remotePatientID;
    }

    @Override
    public String getRemotePatientURL()
    {
        return remotePatientURL;
    }
}