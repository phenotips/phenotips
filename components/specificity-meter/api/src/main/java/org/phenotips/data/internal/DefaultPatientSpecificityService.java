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
