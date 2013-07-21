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
package org.phenotips.data.similarity;

import org.phenotips.data.Patient;

import org.xwiki.stability.Unstable;

/**
 * View of a patient as related to another reference patient.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public interface PatientSimilarityView extends Patient
{
    /**
     * The reference patient against which we're comparing.
     * 
     * @return the patient for which we're searching similar cases
     */
    Patient getReference();

    /**
     * What type of access does the user have to this patient profile.
     * 
     * @return an {@link AccessType} value
     */
    AccessType getAccess();

    /**
     * For matchable patients, the owner isn't listed, instead an anonymous email contact can be initiated using this
     * token as an identifier for the pair (reference patient<->matched patient).
     * 
     * @return a token which can be used for identifying the anonymous email session
     */
    String getContactToken();

    /**
     * How similar is this patient to the reference.
     * 
     * @return a similarity score, between {@code -1} for opposite patient descriptions and {@code 1} for an exact
     *         match, with {@code 0} for patients with no similarities
     */
    double getScore();
}
