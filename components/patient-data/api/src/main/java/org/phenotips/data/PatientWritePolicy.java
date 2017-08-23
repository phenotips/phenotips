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
package org.phenotips.data;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Contains the types of valid policies with which patient data can be updated.
 * </p>
 * <p>
 * The {@link #REPLACE} policy removes all existing patient data and replaces it with provided data.
 * </p>
 * <p>
 * The {@link #UPDATE} policy removes the data for specified properties, and updates it with provided data.
 * </p>
 * <p>
 * The {@link #MERGE} policy retains the old data and adds the new data. Any conflicts are resolved in favor of new
 * data.
 * </p>
 * @version $Id$
 * @since 1.4
 */
public enum PatientWritePolicy
{
    /** The policy that indicates that all existing patient data should be removed and replaced with provided data. */
    REPLACE,
    /** The policy that indicates that patient data for selected controllers should be replaced with provided data. */
    UPDATE,
    /** The policy that indicates that patient data for selected controllers should be merged with provided data. */
    MERGE;

    /**
     * Tries to determine the policy type from the provided string.
     *
     * @param policy string value, representing the policy type
     * @return a {@link PatientWritePolicy}, if such a policy exists, null otherwise
     */
    public static PatientWritePolicy fromString(@Nullable final String policy)
    {
        try {
            return valueOf(StringUtils.upperCase(policy));
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }
}
