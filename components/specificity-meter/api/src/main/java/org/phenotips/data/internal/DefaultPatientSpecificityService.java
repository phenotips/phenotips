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
package org.phenotips.data.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientScorer;
import org.phenotips.data.PatientSpecificity;
import org.phenotips.data.PatientSpecificityService;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

/**
 * Default specificity computation, using the highest priority scorer from all available scorers.
 *
 * @version $Id$
 * @since 1.0M12
 */
@Component
@Singleton
public class DefaultPatientSpecificityService implements PatientSpecificityService, Initializable
{
    /** The list of all available scorers. */
    @Inject
    private List<PatientScorer> scorers;

    public void initialize()
    {
        Collections.sort(this.scorers, new PatientScorerComparator());
    }

    @Override
    public PatientSpecificity getSpecificity(Patient patient)
    {
        PatientSpecificity specificity = null;

        for (PatientScorer scorer : this.scorers) {
            specificity = scorer.getSpecificity(patient);

            if (specificity != null) {
                return specificity;
            }
        }

        return specificity;
    }

    @Override
    public double getScore(Patient patient)
    {
        double score = -1;

        for (PatientScorer scorer : this.scorers) {
            score = scorer.getScore(patient);

            if (score >= 0) {
                return score;
            }
        }

        return score;
    }
}
