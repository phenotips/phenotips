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

package org.phenotips.data.push;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * API that allows pushing patient data to a remote PhenoTips instance.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
@Role
public interface PushServerSendPatientResponse extends PushServerGetPatientIDResponse
{
    /**
     * @return {@code true} iff the group name provided in the push request is either not a valid group name or
     *         corresponds to a group that the user provided is not a member of.
     */
    boolean isActionFailed_incorrectGroup();

    /**
     * @return {@code true} iff GUID was provided in the push request and updating existing patients is disabled on the
     *         server.
     */
    boolean isActionFailed_UpdatesDisabled();

    /**
     * @return {@code true} iff GUID provided in the push request does not correspond to an existing PhenoTips Patient
     *         object on the remote server.
     */
    boolean isActionFailed_IncorrectGUID();

    /**
     * @return {@code true} iff GUID was provided in the push request and the user specified in the request does not
     *         have permissions to update the corresponding document on the remote server.
     */
    boolean isActionFailed_GUIDAccessDenied();

    /**
     * @return {@code true} iff at least one consent that is required to have a patient record on the remote server is
     *         missing. A missing consent is a consent that was not granted by the user on the local instance during
     *         push.
     */
    boolean isActionFailed_MissingConsent();
}
