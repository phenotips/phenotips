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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.data.internal;

import org.phenotips.data.PatientScorer;

import java.util.Comparator;

/**
 * Simple comparator for patient specificity scorers based on integer size.
 *
 * @version $Id$
 * @since 1.2RC1
 */
public class PatientScorerComparator implements Comparator<PatientScorer>
{
    /**
     * Determine which of two scorers takes priority.
     *
     * @param one first scorer
     * @param two second scorer
     * @return a positive integer if the second scorer has a higher priority, {@code 0} if they are equal, or a
     *         negative integer if the second scorer has a lower priority
     */
    public int compare(PatientScorer one, PatientScorer two)
    {
        if (one != null && two != null) {
            return two.getScorerPriority() - one.getScorerPriority();
        } else if (two != null) {
            return 1;
        } else if (one != null) {
            return -1;
        } else {
            return 0;
        }
    }
}
