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

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientScorer;
import org.phenotips.data.PatientSpecificity;
import org.phenotips.ontology.OntologyService;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Patient scorer that computes a local score based on the information content provided by the selected positive and
 * negative features with regards to identifying OMIM disorders.
 *
 * @version $Id$
 * @since 1.0M12
 */
@Component
@Named("omimInformationContent")
@Singleton
public class OmimInformationContentPatientScorer implements PatientScorer, Initializable
{
    private static final String SEARCH_FOR = "symptom";

    /** Provides access to the OMIM ontology, where the information content is checked. */
    @Inject
    @Named("omim")
    private OntologyService omim;

    /** The HPO ontology, needed for accessing the ancestors of a term that might not be present in OMIM. */
    @Inject
    @Named("hpo")
    private OntologyService hpo;

    /** The total information present in OMIM that is reachable through phenotypes. */
    private double totalTerms;

    @Override
    public void initialize() throws InitializationException
    {
        this.totalTerms = this.omim.count(Collections.singletonMap(SEARCH_FOR, "HP:0000001"));
    }

    @Override
    public PatientSpecificity getSpecificity(Patient patient)
    {
        double score = getScore(patient);
        if (score != -1) {
            return new PatientSpecificity(score, new Date(), "local-omim");
        }
        return null;
    }

    @Override
    public double getScore(Patient patient)
    {
        Pair<Double, Integer> symptomsScore = process(patient, true);
        Pair<Double, Integer> negativeSymptomsScore = process(patient, false);
        double score = -1;

        if (symptomsScore.getRight() + negativeSymptomsScore.getRight() > 0) {
            score = 2 * Math.atan(symptomsScore.getLeft() / 10 + negativeSymptomsScore.getLeft() / 20) / Math.PI;
        }
        return score;
    }

    /**
     * Compute the information content of a patient's positive or negative symptoms.
     *
     * @param p the patient profile to score
     * @param presentFeatures whether the score for positive ({@code true}) or negative ({@code false}) features is
     *            computed
     * @return the score (information content) and the number of features
     */
    private Pair<Double, Integer> process(Patient p, boolean presentFeatures)
    {
        double score = 0;
        int count = 0;
        for (Feature f : p.getFeatures()) {
            if (StringUtils.isNotEmpty(f.getId()) && f.isPresent() == presentFeatures) {
                score += informationContent(f);
                count++;
            }
        }
        return new ImmutablePair<Double, Integer>(score, count);
    }

    /**
     * How much information is captured by a feature? In other words, how many diseases are selected by a feature out of
     * the total selectable diseases. If a feature doesn't select any diseases at all, the information content of its
     * nearest represented ancestor is considered, with a slight boost for even more specificity.
     *
     * @param f the target feature to measure
     * @return the information content captured by this term
     */
    private double informationContent(Feature f)
    {
        String toSearch = f.getId();
        double ic = informationContent(this.omim.count(Collections.singletonMap(SEARCH_FOR, toSearch)));
        int i = 0;

        while (ic == 0 && ++i < 5) {
            toSearch = this.hpo.getTerm(toSearch).getParents().iterator().next().getId();
            ic = informationContent(this.omim.count(Collections.singletonMap(SEARCH_FOR, toSearch)));
        }
        return ic * (1 + i / 5);
    }

    /**
     * How much information is contained in {@code n} terms out of the whole ontology?
     *
     * @param n the number of selected terms
     * @return the information content captured by the selected terms
     */
    private double informationContent(long n)
    {
        return n == 0 ? 0 : -Math.log((n * 1.0) / this.totalTerms) / Math.log(2);
    }
}
