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
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.FeatureMetadatumSimilarityView;
import org.phenotips.data.similarity.FeatureSimilarityScorer;
import org.phenotips.data.similarity.FeatureSimilarityView;

import org.xwiki.component.manager.ComponentLookupException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Implementation of {@link FeatureSimilarityView} that reveals the full patient information if the user has full access
 * to the patient, and only matching reference information for similar features if the patient is matchable.
 * 
 * @version $Id$
 * @since 1.0M8
 */
public class RestrictedFeatureSimilarityView implements FeatureSimilarityView
{
    /** The matched feature to represent. */
    private Feature match;

    /** The reference feature against which to compare. */
    private Feature reference;

    /** The access type the user has to the patient having this feature. */
    private AccessType access;

    /** Links metadata values about this feature in the two patients. */
    private Map<String, FeatureMetadatumSimilarityView> matchedMetadata;

    /**
     * Simple constructor passing the {@link #match matched feature}, the {@link #reference reference feature}, and the
     * {@link #access patient access type}.
     * 
     * @param match the matched feature to represent
     * @param reference the reference feature against which to compare, can be {@code null}
     * @param access the access type the user has to the patient having this feature
     */
    public RestrictedFeatureSimilarityView(Feature match, Feature reference, AccessType access)
    {
        this.match = match;
        this.reference = reference;
        this.access = access;

        // Pre-compute the metadata matching pairs
        matchMetadata();
    }

    @Override
    public String getType()
    {
        return this.match != null ? this.match.getType() : this.reference != null ? this.reference.getType() : null;
    }

    @Override
    public String getId()
    {
        return this.access.isOpenAccess() && this.match != null ? this.match.getId() : null;
    }

    @Override
    public String getName()
    {
        return this.access.isOpenAccess() && this.match != null ? this.match.getName() : null;
    }

    @Override
    public boolean isPresent()
    {
        return this.access.isOpenAccess() && this.match != null ? this.match.isPresent() : true;
    }

    @Override
    public Map<String, ? extends FeatureMetadatum> getMetadata()
    {
        if (!this.access.isOpenAccess()) {
            return Collections.emptyMap();
        }

        Map<String, FeatureMetadatum> result = new HashMap<String, FeatureMetadatum>();
        for (FeatureMetadatumSimilarityView meta : this.matchedMetadata.values()) {
            if (meta.getId() != null) {
                result.put(meta.getType(), meta);
            }
        }
        return result;
    }

    @Override
    public JSONObject toJSON()
    {
        if (this.match == null || this.access.isPrivateAccess()) {
            return new JSONObject(true);
        }

        JSONObject result = new JSONObject();
        if (this.access.isOpenAccess()) {
            result.element("id", this.match.getId());
            result.element("name", this.match.getName());
            result.element("type", this.match.getType());
            if (!this.match.isPresent()) {
                result.element("isPresent", false);
            }

            Map<String, ? extends FeatureMetadatum> metadata = this.getMetadata();
            if (!metadata.isEmpty()) {
                JSONArray metadataList = new JSONArray();
                for (FeatureMetadatum metadatum : metadata.values()) {
                    metadataList.add(metadatum.toJSON());
                }
                result.element("metadata", metadataList);
            }
        }

        if (this.reference != null) {
            result.element("queryId", this.reference.getId());
            result.element("queryType", this.reference.getType());
        }

        Double score = getScore();
        if (!Double.isNaN(score)) {
            result.element("score", getScore());
        }

        return result;
    }

    @Override
    public boolean isMatchingPair()
    {
        return this.match != null && this.reference != null;
    }

    @Override
    public Feature getReference()
    {
        return this.reference;
    }

    @Override
    public double getScore()
    {
        if (this.reference == null || this.match == null) {
            return Double.NaN;
        }

        double score = 0;

        if (StringUtils.equals(this.match.getId(), this.reference.getId())) {
            score = 1;
        } else {
            score = getRelativeScore();
        }

        if (this.match.isPresent() != this.reference.isPresent()) {
            score = -score;
        }

        return adjustScoreWithMetadataScores(score);
    }

    /**
     * Create pairs of matching metadata, one from the current patient and one from the reference patient. Unmatched
     * values from either side are paired with a {@code null} value.
     */
    private void matchMetadata()
    {
        if (this.match == null && this.reference == null) {
            // Nothing to match if both are missing
            this.matchedMetadata = Collections.emptyMap();
            return;
        }
        this.matchedMetadata = new HashMap<String, FeatureMetadatumSimilarityView>();

        // Add terms that exist in the matched phenotype, paired with a base phenotype if one exists
        if (this.match != null && this.match.getMetadata() != null) {
            for (Map.Entry<String, ? extends FeatureMetadatum> entry : this.match.getMetadata().entrySet()) {
                this.matchedMetadata.put(entry.getKey(), new RestrictedFeatureMetadatumSimilarityView(entry.getValue(),
                    getMetadatumIfExists(entry.getKey(), this.reference), this.access));
            }
        }

        // Add terms that only exist in the base phenotype
        if (this.reference != null && this.reference.getMetadata() != null) {
            for (Map.Entry<String, ? extends FeatureMetadatum> entry : this.reference.getMetadata().entrySet()) {
                if (!this.matchedMetadata.containsKey(entry.getKey())) {
                    this.matchedMetadata.put(entry.getKey(),
                        new RestrictedFeatureMetadatumSimilarityView(null, entry.getValue(), this.access));
                }
            }
        }

        // Readonly from now on
        this.matchedMetadata = Collections.unmodifiableMap(this.matchedMetadata);
    }

    /**
     * Get a metadatum element from one of the features.
     * 
     * @param toFind the type of metadatum to get
     * @param lookIn the feature to get from, may be {@code null}
     * @return the metadatum specified in the feature, or {@code null} if it does not exist
     */
    private FeatureMetadatum getMetadatumIfExists(String toFind, Feature lookIn)
    {
        return lookIn != null && lookIn.getMetadata() != null ? lookIn.getMetadata().get(toFind) : null;
    }

    /**
     * Compute the similarity score of two possibly related features. The score is between {@code 0} and {@code 1},
     * closer to {@code 1} for more similar features, closer or equal to {@code 0} for too far related features.
     * 
     * @return a number between {@code 0} and {@code 1} describing the similarity of the two features, or {@code NaN} if
     *         the terms can't be identified
     */
    private double getRelativeScore()
    {
        FeatureSimilarityScorer scorer;
        try {
            scorer = ComponentManagerRegistry.getContextComponentManager().getInstance(FeatureSimilarityScorer.class);
        } catch (ComponentLookupException e) {
            return Double.NaN;
        }
        return scorer.getScore(this.match, this.reference);
    }

    /**
     * Adjust the score of a features pair by taking into account the metadata matches. More correct metadata matches
     * will shift the score closer to a perfect score (either {@code 1} or {@code -1}), while more anti-matches will
     * shift the score closer to irrelevance ({@code 0}).
     * 
     * @param baseScore the base score for the features pair, a number between {@code -1} and {@code 1}
     * @return the adjusted score, still a number between {@code -1} and {@code 1}; the base score if no adjustments
     *         were needed
     */
    private double adjustScoreWithMetadataScores(double baseScore)
    {
        if (this.matchedMetadata.isEmpty()) {
            return baseScore;
        }
        // Shift the values to the [3, 5] interval for a more moderate mean
        double bias = 4.0;
        double squareSum = 0;
        double sum = 0;
        int matchingMetadataPairs = 0;

        // Compute a contra-harmonic mean of the individual metadata scores
        for (FeatureMetadatumSimilarityView meta : this.matchedMetadata.values()) {
            double elementScore = meta.getScore();
            if (Double.isNaN(elementScore)) {
                continue;
            }
            squareSum += (bias + elementScore) * (bias + elementScore);
            sum += bias + elementScore;
            ++matchingMetadataPairs;
        }
        double metadataScore = squareSum / sum - bias;

        // Shift the base score, proportionally with the number of matching metadata pairs and their mean score
        if (matchingMetadataPairs > 0) {
            double score = Math.abs(baseScore);
            if (metadataScore >= 0) {
                // For positive metadata matches, reduce the distance to 1 (perfect match score)
                score = score + (1 - score) * metadataScore / (Math.pow(2.0, 1.0 / matchingMetadataPairs));
            } else {
                // For negative metadata matches, reduce the distance to 0 (irrelevant features score)
                score = score + score * metadataScore / (1 + Math.pow(2.0, 1.0 / matchingMetadataPairs));
            }
            return Math.signum(baseScore) * score;
        }
        return baseScore;
    }
}
