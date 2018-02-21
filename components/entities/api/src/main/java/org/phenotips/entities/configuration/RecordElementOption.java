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
package org.phenotips.entities.configuration;

import org.xwiki.stability.Unstable;

import java.util.Locale;

/**
 * Options that affect the behavior of a {@link RecordElement}.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public enum RecordElementOption
{
    /** The field is to be displayed read-only. It is assumed that the data will be provided in other ways. */
    READ_ONLY,
    /**
     * While a value hasn't been set, the field will be displayed as writable. Once a value is set, it will be displayed
     * read-only.
     */
    WRITE_ONCE,
    /**
     * A value for the field is required, which means that the "Save and view" will stop and ask the user to enter a
     * value, but autosaves and non-UI saves are allowed to proceed, which means that a record with an empty value may
     * still exist.
     */
    SOFT_MANDATORY,
    /**
     * A value for the field is always required, which means that any save attempt will be blocked while the value is
     * missing, including autosaves, REST requests, API requests, pushes, etc.
     */
    HARD_MANDATORY,

    /**
     * When created through the browser, a value for this field will be requested in a separate dialog before the record
     * is actually created. Usually should be combined with {@link #READ_ONLY}.
     */
    PREREQUESTED,

    /**
     * Uniqueness is enforced in the UI, which means that the "Save and view" will stop and ask the user to enter a
     * different value, but autosaves and non-UI saves are allowed to proceed, which means that multiple records with
     * the same value may still exist.
     */
    SOFT_UNIQUE,
    /**
     * Uniqueness is always enforced, which means that any save attempt will be blocked while the value is in use,
     * including autosaves, REST requests, API requests, pushes, etc.
     */
    HARD_UNIQUE,
    /**
     * Uniqueness is enforced only among the records owned by the same user. Requires either {@link #SOFT_UNIQUE} or
     * {@link #HARD_UNIQUE} to be set as well.
     */
    PER_USER_UNIQUENESS;

    @Override
    public String toString()
    {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
