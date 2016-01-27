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

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientScorer;
import org.phenotips.data.PatientSpecificity;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

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
    private Vocabulary omim;

    /** The HPO ontology, needed for accessing the ancestors of a term that might not be present in OMIM. */
    @Inject
    @Named("hpo")
    private Vocabulary hpo;

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
        return new PatientSpecificity(score, now(), "local-omim");
    }

    @Override
    public double getScore(Patient patient)
    {
        Pair<Double, Integer> symptomsScore = process(patient, true);
        Pair<Double, Integer> negativeSymptomsScore = process(patient, false);
        double score = 0;

        if (symptomsScore.getRight() + negativeSymptomsScore.getRight() > 0) {
            score = 2 * Math.atan(symptomsScore.getLeft() / 10 + negativeSymptomsScore.getLeft() / 20) / Math.PI;
        }
        return score;
    }

    public int getScorerPriority() {
        return 10;
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
            VocabularyTerm term = this.hpo.getTerm(toSearch);
            if (term == null) {
                break;
            }
            Set<VocabularyTerm> parents = term.getParents();
            if (parents.isEmpty()) {
                break;
            }
            toSearch = parents.iterator().next().getId();
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

    private Date now()
    {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime();
    }
}
