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
import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.DisorderSimilarityView;
import org.phenotips.data.similarity.FeatureSimilarityScorer;
import org.phenotips.data.similarity.FeatureSimilarityView;
import org.phenotips.data.similarity.PatientSimilarityView;

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
 * Implementation of {@link PatientSimilarityView} that reveals the full patient information if the user has full access
 * to the patient, and only limited information for similar features if the patient is matchable.
 * 
 * @version $Id$
 * @since 1.0M8
 */
public class RestrictedPatientSimilarityView implements PatientSimilarityView
{
    /** The matched patient to represent. */
    private final Patient match;

    /** The reference patient against which to compare. */
    private final Patient reference;

    /** The access level the user has to this patient. */
    private final AccessType access;

    /** Links feature values from this patient to the reference. */
    private Set<FeatureSimilarityView> matchedFeatures;

    /** Links disorder values from this patient to the reference. */
    private Set<DisorderSimilarityView> matchedDisorders;

    /**
     * Simple constructor passing both {@link #match the patient} and the {@link #reference reference patient}.
     * 
     * @param match the matched patient to represent, must not be {@code null}
     * @param reference the reference patient against which to compare, must not be {@code null}
     * @param access the access level the current user has on the matched patient
     * @throws IllegalArgumentException if one of the patients is {@code null}
     */
    public RestrictedPatientSimilarityView(Patient match, Patient reference, AccessType access)
        throws IllegalArgumentException
    {
        if (match == null || reference == null) {
            throw new IllegalArgumentException("Similar patients require both a match and a reference");
        }
        this.match = match;
        this.reference = reference;
        this.access = access;
        matchFeatures();
        matchDisorders();
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
    public Set<? extends Feature> getFeatures()
    {
        if (this.access.isPrivateAccess()) {
            return Collections.emptySet();
        }

        Set<Feature> result = new HashSet<Feature>();
        for (FeatureSimilarityView feature : this.matchedFeatures) {
            if (feature.isMatchingPair() || this.access.isOpenAccess() && feature.getId() != null) {
                result.add(feature);
            }
        }

        return result;
    }

    @Override
    public Set<? extends Disorder> getDisorders()
    {
        if (!this.access.isOpenAccess()) {
            return Collections.emptySet();
        }

        Set<Disorder> result = new HashSet<Disorder>();
        for (DisorderSimilarityView disorder : this.matchedDisorders) {
            if (disorder.getId() != null) {
                result.add(disorder);
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
    public AccessLevel getAccess()
    {
        return this.access.getAccessLevel();
    }

    @Override
    public Patient getReference()
    {
        return this.reference;
    }

    @Override
    public double getScore()
    {
        double featuresScore = getFeaturesScore();
        return adjustScoreWithDisordersScore(featuresScore);
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
        result.element("featuresCount", this.match.getFeatures().size());

        Set<? extends Feature> features = getFeatures();
        if (!features.isEmpty()) {
            JSONArray featuresJSON = new JSONArray();
            for (Feature feature : features) {
                featuresJSON.add(feature.toJSON());
            }
            result.element("features", featuresJSON);
        }

        Set<? extends Disorder> disorders = getDisorders();
        if (!disorders.isEmpty()) {
            JSONArray disordersJSON = new JSONArray();
            for (Disorder disorder : disorders) {
                disordersJSON.add(disorder.toJSON());
            }
            result.element("disorders", disordersJSON);
        }

        return result;
    }

    /**
     * Create pairs of matching features, one from the current patient and one from the reference patient. Unmatched
     * values from either side are paired with a {@code null} value.
     */
    private void matchFeatures()
    {
        Set<FeatureSimilarityView> result = new HashSet<FeatureSimilarityView>();
        for (Feature feature : this.match.getFeatures()) {
            Feature matching = findMatchingFeature(feature, this.reference.getFeatures());
            result.add(new RestrictedFeatureSimilarityView(feature, matching, this.access));
        }
        for (Feature feature : this.reference.getFeatures()) {
            Feature matching = findMatchingFeature(feature, this.match.getFeatures());
            if (matching == null) {
                result.add(new RestrictedFeatureSimilarityView(null, feature, this.access));
            }
        }
        this.matchedFeatures = Collections.unmodifiableSet(result);
    }

    /**
     * Create pairs of matching disorders, one from the current patient and one from the reference patient. Unmatched
     * values from either side are paired with a {@code null} value.
     */
    private void matchDisorders()
    {
        Set<DisorderSimilarityView> result = new HashSet<DisorderSimilarityView>();
        for (Disorder disorder : this.match.getDisorders()) {
            result.add(new RestrictedDisorderSimilarityView(disorder, findMatchingDisorder(disorder,
                this.reference.getDisorders()), this.access));
        }
        for (Disorder disorder : this.reference.getDisorders()) {
            if (this.match == null || findMatchingDisorder(disorder, this.match.getDisorders()) == null) {
                result.add(new RestrictedDisorderSimilarityView(null, disorder, this.access));
            }
        }
        this.matchedDisorders = Collections.unmodifiableSet(result);
    }

    /**
     * Searches for a similar feature in the reference patient, matching one of the matched patient's features, or
     * vice-versa.
     * 
     * @param toMatch the feature to match
     * @param lookIn the list of features to look in, either the reference patient or the matched patient features
     * @return one of the featuress from the list, if it matches the target feature, or {@code null} otherwise
     */
    private Feature findMatchingFeature(Feature toMatch, Set<? extends Feature> lookIn)
    {
        try {
            FeatureSimilarityScorer scorer =
                ComponentManagerRegistry.getContextComponentManager().getInstance(FeatureSimilarityScorer.class);
            double bestScore = 0;
            Feature bestMatch = null;
            for (Feature candidate : lookIn) {
                double score = scorer.getScore(candidate, toMatch);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = candidate;
                }
            }
            return bestMatch;
        } catch (ComponentLookupException e) {
            for (Feature candidate : lookIn) {
                if (StringUtils.equals(candidate.getId(), toMatch.getId())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Searches for a similar disorder in the reference patient, matching one of the matched patient's disorders, or
     * vice-versa.
     * 
     * @param toMatch the disorder to match
     * @param lookIn the list of disorders to look in, either the reference patient or the matched patient diseases
     * @return one of the disorders from the list, if it matches the target disorder, or {@code null} otherwise
     */
    private Disorder findMatchingDisorder(Disorder toMatch, Set<? extends Disorder> lookIn)
    {
        for (Disorder candidate : lookIn) {
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
    private double getFeaturesScore()
    {
        if (this.matchedFeatures.isEmpty()) {
            return 0;
        }
        double featureScore;
        // Lower bias means that positive values are far more important ("heavy") than negative ones
        // Higher bias means that the score is closer to an arithmetic mean
        double bias = 3.0;
        double squareSum = 0;
        double sum = 0;
        int matchingFeaturePairs = 0;
        int unmatchedFeaturePairs = 0;

        for (FeatureSimilarityView feature : this.matchedFeatures) {
            double elementScore = feature.getScore();
            if (Double.isNaN(elementScore)) {
                ++unmatchedFeaturePairs;
                continue;
            }
            squareSum += (bias + elementScore) * (bias + elementScore);
            sum += bias + elementScore;
            ++matchingFeaturePairs;
        }
        if (matchingFeaturePairs == 0) {
            return 0;
        }
        featureScore = squareSum / sum - bias;

        if (unmatchedFeaturePairs > 0 && featureScore > 0) {
            // When there are many unmatched features, lower the score towards 0 (irrelevant patient pair score)
            featureScore *=
                Math.pow(0.9, Math.max(0, unmatchedFeaturePairs - Math.ceil(Math.log(matchingFeaturePairs))));
        }
        return featureScore;
    }

    /**
     * Adjust the similarity score by taking into account common disorders. Matching disorders will boost the base score
     * given by the phenotypic similarity, while unmatched disorders don't affect the score at all. If the base score is
     * negative, no boost is awarded.
     * 
     * @param baseScore the score given by features alone, a number between {@code -1} and {@code 1}
     * @return the adjusted similarity score, boosted closer to {@code 1} if there are common disorders between this
     *         patient and the reference patient, or the unmodified base score otherwise; the score is never lowered,
     *         and never goes above {@code 1}
     * @see #getScore()
     */
    private double adjustScoreWithDisordersScore(double baseScore)
    {
        if (this.matchedDisorders.isEmpty() || baseScore <= 0) {
            return baseScore;
        }
        double score = baseScore;
        double bias = 3;
        for (DisorderSimilarityView disorder : this.matchedDisorders) {
            if (disorder.isMatchingPair()) {
                // For each disorder match, reduce the distance between the current score to 1 by 1/3
                score = score + (1 - score) / bias;
            }
        }
        return score;
    }
}
