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
