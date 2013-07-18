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

import org.xwiki.component.annotation.Role;

/**
 * Creates a custom view of the similarities between two patients, a reference patients and a patient matching the
 * reference patient's phenotypic profile. The resulting object is an extended version of the {@link Patient base
 * patient API}, which may block access to certain restricted information, and may extend data with similarity
 * information. For example, a phenotype from the matched patient that matches another phenotype from the reference
 * patient will {@link SimilarPhenotype#getReference() indicate that}, and will be able to compute a
 * {@link SimilarPhenotype#getScore() similarity score}.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Role
public interface SimilarPatientFactory
{
    /**
     * Instantiates a {@link SimilarPatient} specific to this factory, linking the two patients.
     * 
     * @param match the matched patient whose data will be exposed
     * @param reference the patient used as the reference against which to compare
     * @return the extended patient
     * @throws IllegalArgumentException if one of the patients is {@code null}
     */
    SimilarPatient makeSimilarPatient(Patient match, Patient reference) throws IllegalArgumentException;
}
