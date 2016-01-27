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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * A service that computes the patient specificity, a score estimating how "good" a patient record is.
 *
 * @version $Id$
 * @since 1.0M12
 */
@Unstable
@Role
public interface PatientScorer
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

    /**
     * Get the priority of this scorer. Higher priority (larger integer) scorers will be used first.
     *
     * @since 1.2RC1
     * @return a score greater than {@code 0}
     */
    int getScorerPriority();
}
