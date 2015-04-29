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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientScorer;
import org.phenotips.data.PatientSpecificity;
import org.phenotips.data.PatientSpecificityService;

import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Default specificity computation, using the MONARCH initiative service, and, if that isn't available, falling back to
 * a local information content score.
 *
 * @version $Id$
 * @since 1.0M12
 */
@Component
@Singleton
public class DefaultPatientSpecificityService implements PatientSpecificityService
{
    /** The default, high quality scorer. */
    @Inject
    @Named("monarch")
    private PatientScorer monarchScorer;

    /** The fast local scorer. */
    @Inject
    @Named("omimInformationContent")
    private PatientScorer omimScorer;

    @Override
    public PatientSpecificity getSpecificity(Patient patient)
    {
        PatientSpecificity spec = null;

        spec = this.monarchScorer.getSpecificity(patient);
        if (spec == null) {
            spec = this.omimScorer.getSpecificity(patient);
        }
        return spec;
    }

    @Override
    public double getScore(Patient patient)
    {
        double score = -1;

        score = this.monarchScorer.getScore(patient);
        if (score == -1) {
            score = this.omimScorer.getScore(patient);
        }
        return score;
    }
}
