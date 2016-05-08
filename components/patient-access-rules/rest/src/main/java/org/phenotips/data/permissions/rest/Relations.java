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
package org.phenotips.data.permissions.rest;

/**
 * Common relation types used for PhenoTips data.
 *
 * @version $Id$
 * @since 1.2M5
 */
public final class Relations
{
    /** Relation for links pointing to the resource that returned the current representation. */
    public static final String SELF = "self";

    /** Relation for links pointing to the main permissions representation. */
    public static final String PERMISSIONS = "https://phenotips.org/rel/permissions";

    /** Relation for links pointing to the owner of a patient. */
    public static final String OWNER = "https://phenotips.org/rel/owner";

    /** Relation for links pointing to the visibility of a patient. */
    public static final String VISIBILITY = "https://phenotips.org/rel/visibility";

    /** Relation for links pointing to a patient record. */
    public static final String PATIENT_RECORD = "https://phenotips.org/rel/patientRecord";

    /** Relation for links pointing to a collaborator. */
    public static final String COLLABORATOR = "https://phenotips.org/rel/collaborator";

    /** Relation for links pointing to a collection of collaborators. */
    public static final String COLLABORATORS = "https://phenotips.org/rel/collaborators";

    /** Avoid instantiation. */
    private Relations()
    {
    }
}
