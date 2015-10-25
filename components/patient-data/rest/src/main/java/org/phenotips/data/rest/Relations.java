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
package org.phenotips.data.rest;

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

    /** Relation for links pointing to a search service for the current type of resource. */
    public static final String SEARCH = "https://phenotips.org/rel/search";

    /** Relation for links pointing to a suggest (automatic search) service for the current type of resource. */
    public static final String SUGGEST = "https://phenotips.org/rel/suggest";

    /** Relation for links pointing to the history of a resource. */
    public static final String HISTORY = "https://phenotips.org/rel/history";

    /** Relation for links pointing to a patient record. */
    public static final String PATIENT_RECORD = "https://phenotips.org/rel/patientRecord";

    /** Relation for links pointing to a pedigree. */
    public static final String PEDIGREE = "https://phenotips.org/rel/pedigree";

    /** Relation for links pointing to a family. */
    public static final String FAMILY = "https://phenotips.org/rel/family";

    /** Relation for links pointing to a vocabulary. */
    public static final String VOCABULARY = "https://phenotips.org/rel/vocabulary";

    /** Relation for links pointing to a vocabulary term. */
    public static final String VOCABULARY_TERM = "https://phenotips.org/rel/vocabularyTerm";

    /** Avoid instantiation. */
    private Relations()
    {
    }
}
