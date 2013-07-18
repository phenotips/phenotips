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
package org.phenotips.data.similarity.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Disease;
import org.phenotips.data.Patient;
import org.phenotips.data.Phenotype;
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.PhenotypeSimilarityScorer;
import org.phenotips.data.similarity.SimilarDisease;
import org.phenotips.data.similarity.SimilarPatient;
import org.phenotips.data.similarity.SimilarPhenotype;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Implementation of {@link SimilarPatient} that reveals the full patient information if the user has full access to the
 * patient, and only limited information for similar features if the patient is matchable.
 * 
 * @version $Id$
 * @since 1.0M8
 */
public class RestrictedSimilarPatient implements SimilarPatient
{
    /** The matched patient to represent. */
    private Patient match;

    /** The reference patient against which to compare. */
    private Patient reference;

    /** The access type the user has to this patient. */
    private AccessType access;

    /** Links phenotype values from this patient to the reference. */
    private Set<SimilarPhenotype> matchedPhenotypes;

    /** Links disease values from this patient to the reference. */
    private Set<SimilarDisease> matchedDiseases;

    /**
     * Simple constructor passing both {@link #match the patient} and the {@link #reference reference patient}.
     * 
     * @param match the matched patient to represent, must not be {@code null}
     * @param reference the reference patient against which to compare, must not be {@code null}
     * @throws IllegalArgumentException if one of the patients is {@code null}
     */
    public RestrictedSimilarPatient(Patient match, Patient reference) throws IllegalArgumentException
    {
        if (match == null || reference == null) {
            throw new IllegalArgumentException("Similar patients require both a match and a reference");
        }
        this.match = match;
        this.reference = reference;
        // FIXME Implement the other access types
        if (this.match == null || this.reference == null || this.reference.getReporter() == null) {
            // Not enough information to compute the proper access type, it's safer to refuse any access
            this.access = AccessType.PRIVATE;
        } else if (ObjectUtils.equals(this.match.getReporter(), this.reference.getReporter())) {
            this.access = AccessType.OWNED;
        } else {
            this.access = AccessType.MATCH;
        }
        matchPhenotypes();
        matchDiseases();
    }

    @Override
    public DocumentReference getDocument()
    {
        return this.access.isOpenAccess() ? this.match.getDocument() : null;
    }

    @Override
    public DocumentReference getReporter()
    {
        return this.access.isOpenAccess() ? this.match.getReporter() : null;
    }

    @Override
    public Set<? extends Phenotype> getPhenotypes()
    {
        if (this.access.isPrivateAccess()) {
            return Collections.emptySet();
        }

        Set<Phenotype> result = new HashSet<Phenotype>();
        for (SimilarPhenotype phenotype : this.matchedPhenotypes) {
            if (phenotype.isMatchingPair() || this.access.isOpenAccess() && phenotype.getId() != null) {
                result.add(phenotype);
            }
        }

        return result;
    }

    @Override
    public Set<? extends Disease> getDiseases()
    {
        if (!this.access.isOpenAccess()) {
            return Collections.emptySet();
        }

        Set<Disease> result = new HashSet<Disease>();
        for (SimilarDisease disease : this.matchedDiseases) {
            if (disease.getId() != null) {
                result.add(disease);
            }
        }

        return result;
    }

    @Override
    public String getContactToken()
    {
        // FIXME Implementation missing
        return "";
    }

    @Override
    public AccessType getAccess()
    {
        return this.access;
    }

    @Override
    public Patient getReference()
    {
        return this.reference;
    }

    @Override
    public double getScore()
    {
        double phenotypeScore = getPhenotypesScore();
        return adjustScoreWithDiseasesScore(phenotypeScore);
    }

    @Override
    public JSONObject toJSON()
    {
        if (this.access.isPrivateAccess()) {
            return new JSONObject(true);
        }
        JSONObject result = new JSONObject();

        if (this.access.isOpenAccess()) {
            result.element("id", this.match.getDocument().getName());
            result.element("token", getContactToken());
            result.element("owner", this.match.getReporter().getName());
        }
        result.element("access", this.access.toString());
        result.element("myCase", ObjectUtils.equals(this.reference.getReporter(), this.match.getReporter()));
        result.element("score", getScore());
        result.element("featuresCount", this.match.getPhenotypes().size());

        Set<? extends Phenotype> phenotypes = getPhenotypes();
        if (!phenotypes.isEmpty()) {
            JSONArray featuresJSON = new JSONArray();
            for (Phenotype phenotype : phenotypes) {
                featuresJSON.add(phenotype.toJSON());
            }
            result.element("features", featuresJSON);
        }

        Set<? extends Disease> diseases = getDiseases();
        if (!diseases.isEmpty()) {
            JSONArray disordersJSON = new JSONArray();
            for (Disease disease : diseases) {
                disordersJSON.add(disease.toJSON());
            }
            result.element("disorders", disordersJSON);
        }

        return result;
    }

    /**
     * Create pairs of matching phenotypes, one from the current patient and one from the reference patient. Unmatched
     * values from either side are paired with a {@code null} value.
     */
    private void matchPhenotypes()
    {
        Set<SimilarPhenotype> result = new HashSet<SimilarPhenotype>();
        for (Phenotype phenotype : this.match.getPhenotypes()) {
            Phenotype matching = findMatchingPhenotype(phenotype, this.reference.getPhenotypes());
            result.add(new RestrictedSimilarPhenotype(phenotype, matching, this.access));
        }
        for (Phenotype phenotype : this.reference.getPhenotypes()) {
            Phenotype matching = findMatchingPhenotype(phenotype, this.match.getPhenotypes());
            if (matching == null) {
                result.add(new RestrictedSimilarPhenotype(null, phenotype, this.access));
            }
        }
        this.matchedPhenotypes = Collections.unmodifiableSet(result);
    }

    /**
     * Create pairs of matching diseases, one from the current patient and one from the reference patient. Unmatched
     * values from either side are paired with a {@code null} value.
     */
    private void matchDiseases()
    {
        Set<SimilarDisease> result = new HashSet<SimilarDisease>();
        for (Disease disease : this.match.getDiseases()) {
            result.add(new RestrictedSimilarDisease(disease, findMatchingDisease(disease,
                this.reference.getDiseases()), this.access));
        }
        for (Disease disease : this.reference.getDiseases()) {
            if (this.match == null || findMatchingDisease(disease, this.match.getDiseases()) == null) {
                result.add(new RestrictedSimilarDisease(null, disease, this.access));
            }
        }
        this.matchedDiseases = Collections.unmodifiableSet(result);
    }

    /**
     * Searches for a similar phenotype in the reference patient, matching one of the matched patient's phenotypes, or
     * vice-versa.
     * 
     * @param toMatch the phenotype to match
     * @param lookIn the list of phenotypes to look in, either the reference patient or the matched patient phenotypes
     * @return one of the phenotypes from the list, if it matches the target phenotype, or {@code null} otherwise
     */
    private Phenotype findMatchingPhenotype(Phenotype toMatch, Set<? extends Phenotype> lookIn)
    {
        try {
            PhenotypeSimilarityScorer scorer =
                ComponentManagerRegistry.getContextComponentManager().getInstance(PhenotypeSimilarityScorer.class);
            double bestScore = 0;
            Phenotype bestMatch = null;
            for (Phenotype candidate : lookIn) {
                double score = scorer.getScore(candidate, toMatch);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = candidate;
                }
            }
            return bestMatch;
        } catch (ComponentLookupException e) {
            for (Phenotype candidate : lookIn) {
                if (StringUtils.equals(candidate.getId(), toMatch.getId())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Searches for a similar disease in the reference patient, matching one of the matched patient's diseases, or
     * vice-versa.
     * 
     * @param toMatch the disease to match
     * @param lookIn the list of diseases to look in, either the reference patient or the matched patient diseases
     * @return one of the diseases from the list, if it matches the target disease, or {@code null} otherwise
     */
    private Disease findMatchingDisease(Disease toMatch, Set<? extends Disease> lookIn)
    {
        for (Disease candidate : lookIn) {
            if (StringUtils.equals(candidate.getId(), toMatch.getId())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Compute the patient's score as given by the phenotypic similarity with the reference patient.
     * 
     * @return a similarity score, between {@code -1} for opposite patient descriptions and {@code 1} for an exact
     *         match, with {@code 0} for patients with no similarities
     * @see #getScore()
     */
    private double getPhenotypesScore()
    {
        if (this.matchedPhenotypes.isEmpty()) {
            return 0;
        }
        double phenotypeScore;
        // Lower bias means that positive values are far more important ("heavy") than negative ones
        // Higher bias means that the score is closer to an arithmetic mean
        double bias = 3.0;
        double squareSum = 0;
        double sum = 0;
        int matchingMetadataPairs = 0;
        int unmatchedMetadataPairs = 0;

        for (SimilarPhenotype phenotype : this.matchedPhenotypes) {
            double elementScore = phenotype.getScore();
            if (Double.isNaN(elementScore)) {
                ++unmatchedMetadataPairs;
                continue;
            }
            squareSum += (bias + elementScore) * (bias + elementScore);
            sum += bias + elementScore;
            ++matchingMetadataPairs;
        }
        if (matchingMetadataPairs == 0) {
            return 0;
        }
        phenotypeScore = squareSum / sum - bias;

        if (unmatchedMetadataPairs > 0 && phenotypeScore > 0) {
            // When there are many unmatched metadata, lower the score towards 0 (irrelevant patient pair score)
            phenotypeScore *=
                Math.pow(0.9, Math.max(0, unmatchedMetadataPairs - Math.ceil(Math.log(matchingMetadataPairs))));
        }
        return phenotypeScore;
    }

    /**
     * Adjust the similarity score by taking into account common diseases. Matching diseases will boost the base score
     * given by the phenotypic similarity, while unmatched diseases don't affect the score at all. If the base score is
     * negative, no boost is awarded.
     * 
     * @param baseScore the score given by phenotypes alone, a number between {@code -1} and {@code 1}
     * @return the adjusted similarity score, boosted closer to {@code 1} if there are common diseases between this
     *         patient and the reference patient, or the unmodified base score otherwise; the score is never lowered,
     *         and never goes above {@code 1}
     * @see #getScore()
     */
    private double adjustScoreWithDiseasesScore(double baseScore)
    {
        if (this.matchedDiseases.isEmpty() || baseScore <= 0) {
            return baseScore;
        }
        double score = baseScore;
        double bias = 3;
        for (SimilarDisease disease : this.matchedDiseases) {
            if (disease.isMatchingPair()) {
                // For each disease match, reduce the distance between the current score to 1 by 1/3
                score = score + (1 - score) / bias;
            }
        }
        return score;
    }
}
