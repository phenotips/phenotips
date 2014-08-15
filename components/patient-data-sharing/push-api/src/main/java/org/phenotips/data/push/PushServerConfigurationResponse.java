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

package org.phenotips.data.push;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Set;

/**
 * API that allows pushing patient data to a remote PhenoTips instance.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
@Role
public interface PushServerConfigurationResponse extends PushServerResponse
{
    /**
     * Get the list of remote PhenoTips group names the given remote user is a member of.
     *
     * @return {@code Null} if login attempt was not successful, otherwise a list of remote PhenoTips group names the
     *         given remote user is a member of on the remote server. The set may be empty if the user is not a member
     *         of any PhenoTips groups.
     */
    Set<String> getRemoteUserGroups();

    /**
     * Get the list of patient data fields accepted by the remote server. The list may or may not be different depending
     * on the remote group. All other fields will be discarded by the remote server.
     *
     * @param groupName remote group name (optional, may be {@code null}).
     * @return List of remote field names accepted by the remote server.
     */
    Set<String> getRemoteAcceptedPatientFields(String groupName);

    Set<String> getRemoteAcceptedPatientFields();

    /**
     * Get the intersection of locally available non-personally identifiable fields and fields accepted by the remote
     * server.<br>
     * The list may or may not be different depending on the remote group.
     *
     * @param groupName remote group name (optional, may be {@code null}).
     * @return List of patient data fields which are 1) accepted by the remote server 2) locally available and 3)
     *         considered non-personally identifiable by the local server.
     */
    Set<String> getPushableFields(String groupName);

    Set<String> getPushableFields();

    /**
     * Indicates whether remote server allows updates of existing patients. When false, only pushes of new patients are
     * allowed.
     * <p>
     * Note that even when updates are enabled, remote user used for pushing should have enough permissions to modify
     * the patient. Updates are also only allowed when a valid GUID is provided, supposedly obtained as a result of an
     * earlier push request.
     *
     * @return {@code true} if remote server allows
     */
    boolean remoteUpdatesEnabled();

    /**
     * Get the user_token for future passwordless patient data pushes to the same server by the same remote user. This
     * feature may be disabled on the remote server, in which case {@code null} is returned.
     *
     * @return User token (if provided by the remote server) or {@code null}.
     */
    String getRemoteUserToken();
}
