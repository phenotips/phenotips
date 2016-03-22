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

import java.util.List;

import org.json.JSONObject;

/**
 * Used for representing a single consent; for example in a patient record.
 *
 * @version $Id$
 * @since 1.3M1
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
     * The users never sees the ids of the consents, they only see labels (aka titles).
     *
     * @return the description of this consent
     */
    String getLabel();

    /**
     * An optional (potentially long) description which is supposed to be displayed under a consent, possibly with
     * links to external documents such as consent form(s).
     *
     * @return the long description of this consent
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
     * A convenience method to check if Consent status is Yes.
     * @return {@code true} iff getStatus() == Yes, {@code false} otherwise
     */
    boolean isGranted();

    /**
     * The status should be dynamically changeable.
     *
     * @param status the new status
     */
    void setStatus(ConsentStatus status);

    /**
     * Some consents are required to for any interaction with a patient record data (viewing, modifying, etc).
     *
     * @return {@code true} if required, {@code false} otherwise
     */
    boolean isRequired();

    /**
     * The list of (patient form) fields only available if consent is granted.
     * If a field is affected by more than one consent, all of the consents have to be granted in order
     * to enable the field.
     *
     * @return A list of field IDs of the affected fields. If all fields are affected an empty list is returned.
     * If no fields are affected {@code null} is returned.
     */
    List<String> getFields();

    /**
     * A convenience method to determine that all or no fields are affected.
     * @return {@code true} if all fields are affected, {@code false} otherwise
     */
    boolean affectsAllFields();

    /**
     * A convenience method to determine that at least one field is affected (e.g. a consent may be
     * requred for something other than fields).
     * @return {@code true} if at least one fields is affected.
     */
    boolean affectsSomeFields();

    /**
     * Consents should be convertible into a JSON format.
     *
     * @return a JSON object representing this consent
     */
    JSONObject toJSON();

    /**
     * Returns a copy of this consent with the given status set instead of the current status.
     *
     * @param status the new status
     * @return a new consent object identical to current except that the status is set to the given one.
     */
    Consent copy(ConsentStatus status);
}
