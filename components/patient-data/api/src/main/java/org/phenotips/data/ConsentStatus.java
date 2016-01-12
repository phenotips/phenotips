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

/**
 * Used to indicate the status of a consent, which is not limited to 'granted'/'not granted'.
 *
 * @version $Id$
 * @since 1.3M1
 */
public enum ConsentStatus
{
    /** Used for when a consent instance has just been loaded, and has no status. */
    NOT_SET("not_set"),
    /** Used when a consent is granted. */
    YES("yes"),
    /** Used when a consent is not granted. */
    NO("no");

    private String stringRepresentation;

    ConsentStatus(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String toString()
    {
        return this.stringRepresentation;
    }

    /**
     * For converting a string into this {@link Enum}.
     * @param string which corresponds to one of this enum values
     * @return corresponding enum
     */
    public static ConsentStatus fromString(String string) {
        for (ConsentStatus status : ConsentStatus.values()) {
            if (status.toString().contentEquals(string))
            {
                return status;
            }
        }
        return null;
    }
}
