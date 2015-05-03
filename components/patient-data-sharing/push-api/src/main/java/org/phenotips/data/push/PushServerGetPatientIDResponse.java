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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
public interface PushServerGetPatientIDResponse extends PushServerResponse
{
    /**
     * GUID of the remote patient object, which can be used to update remote patient data or to get the current remote
     * patient URL.
     *
     * @return {@code String}, remote GUID; {@code null} if the response did not include this information (e.g. in case
     *         of a failure)
     */
    String getRemotePatientGUID();

    /**
     * Current remote URL of the patient created or patient updated.
     *
     * @return {@code String}, URL of the patient on the remote server; {@code null} if the response did not include
     *         this information (e.g. in case of a failure)
     */
    String getRemotePatientURL();

    /**
     * Current remote URL of the patient created or patient updated.
     *
     * @return {@code String}, URL of the patient on the remote server; {@code null} if the response did not include
     *         this information (e.g. in case of a failure)
     */
    String getRemotePatientID();
}
