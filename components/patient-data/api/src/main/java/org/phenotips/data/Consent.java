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

import org.xwiki.stability.Unstable;

import org.json.JSONObject;

/**
 * Used for representing a single consent; for example in a patient record.
 *
 * @version $Id$
 * @since 1.2RC2
 */
@Unstable
public interface Consent
{
    /**
     * Internally, consents are referred by their identifiers.
     *
     * @return the id of this consent
     */
    String getId();

    /**
     * The users never sees the ids of the consents, they only see titles (which are more like descriptions).
     *
     * @return the description of this consent
     */
    String getDescription();

    /**
     * Could be notLoaded/Yes/No. The consent configurations (description, id) will be coming from the database, and
     * should not be reloaded often. There should then be a cache of all the consents in the system with a status of
     * notLoaded.
     *
     * @return {@link ConsentStatus} which should generally be Yes or No, but could be notLoaded
     */
    ConsentStatus getStatus();

    /**
     * The status should be dynamically changeable.
     *
     * @param status the new status
     */
    void setStatus(ConsentStatus status);

    /**
     * Some consents are required to for any interaction with a patient record data (viewing, modifying, etc).
     *
     * @return {@link true} if required, {@link false} otherwise
     */
    boolean isRequired();

    /**
     * Consents should be convertible into a JSON format.
     *
     * @return a JSON object representing this consent
     */
    JSONObject toJson();

    /**
     * Should be a static method, but should also be in the interface.
     *
     * @param json the representation of a consent
     * @return a new {@link Consent} instance, as represented by
     */
    Consent fromJson(JSONObject json);
}
