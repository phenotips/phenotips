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
