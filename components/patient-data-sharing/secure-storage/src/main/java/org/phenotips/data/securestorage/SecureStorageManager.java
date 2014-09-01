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

import org.xwiki.component.annotation.Role;

/**
 * Used to store data in a way inaccessible from any of the wiki pages by regular users without programming rights.
 * <p>
 * Most sensitive information stored is remote user names and login tokens, which can be used to push data to a remote
 * server without a password.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Role
public interface SecureStorageManager
{
    /**
     * TODO
     *
     * @param
     * @return
     */
    void storeRemoteLoginData(String userName, String serverName, String remoteUserName, String remoteLoginToken);

    void storeLocalLoginToken(String userName, String sourceServerName, String loginToken);

    void removeRemoteLoginData(String userName, String serverName);

    // null if not found
    RemoteLoginData getRemoteLoginData(String userName, String serverName);

    LocalLoginToken getLocalLoginToken(String userName, String sourceServerName);

    void removeAllLocalTokens(String sourceServerName);

    void storePatientSourceServerInfo(String patientGUID, String sourceServerName);

    // null if local
    PatientSourceServerInfo getPatientSourceServerInfo(String patientGUID);

    void storePatientPushInfo(String localPatientID, String remoteServerName,
        String remotePatientGUID, String remotePatientID, String remotePatientURL);

    // null if never pushed to the given server
    PatientPushedToInfo getPatientPushInfo(String localPatientID, String remoteServerName);
}
