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
package org.phenotips.data;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Computes the {@link PatientSpecificity patient specificity}, a score estimating how "good" a patient record is, be
 * invoking one of the available {@link PatientScorer scorers}.
 *
 * @version $Id$
 * @since 1.0M12
 */
@Unstable
@Role
public interface PatientSpecificityService
{
    /**
     * Compute the specificity score for a patient, along with metadata about the score.
     *
     * @param patient the patient to score
     * @return a valid specificity score if the score was successfully computed, {@code null} in case of failure
     */
    PatientSpecificity getSpecificity(Patient patient);

    /**
     * Compute the raw specificity score for a patient.
     *
     * @param patient the patient to score
     * @return a score between {@code 0} and {@code 1}, or {@code -1} if the score cannot be computed by this scorer
     */
    double getScore(Patient patient);
}
