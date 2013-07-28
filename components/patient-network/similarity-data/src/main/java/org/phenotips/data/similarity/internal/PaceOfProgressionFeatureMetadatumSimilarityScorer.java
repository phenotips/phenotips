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

import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.similarity.FeatureMetadatumSimilarityScorer;

import org.xwiki.component.annotation.Component;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * {@link FeatureMetadatumSimilarityScorer Similarity scorer} for the Pace of Progression metaproperty, rewarding
 * speeds that are close to each other.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("pace_of_progression")
@Singleton
public class PaceOfProgressionFeatureMetadatumSimilarityScorer implements FeatureMetadatumSimilarityScorer
{
    /** The possible values with an associated "coordinate" which tells how close two values are. */
    private static final Map<String, Double> ORDERED_VALUES = new HashMap<String, Double>();
    static {
        // Nonprogressive disorder
        ORDERED_VALUES.put("HP:0003680", 0.5);
        // Slow progression
        ORDERED_VALUES.put("HP:0003677", 1.0);
        // Progressive disorder
        ORDERED_VALUES.put("HP:0003676", 2.0);
        // Rapidly progressive
        ORDERED_VALUES.put("HP:0003678", 3.5);
    }

    /** Variable progression, can't be compared with the constant ones. */
    private static final String VARIABLE_PROGRESSION = "HP:0003682";

    /** Distance at which two values are considered neutral, with a resulting score of {@code 0}. */
    private static final double NEUTRAL_DIST = 1.5;

    /** Distance at which two values are considered too different, with a resulting score of {@code -1}. */
    private static final double MAX_DIST = 2 * NEUTRAL_DIST;

    @Override
    public double getScore(FeatureMetadatum match, FeatureMetadatum reference)
    {
        if (match == null || reference == null || StringUtils.isEmpty(match.getId())
            || StringUtils.isEmpty(reference.getId())) {
            // Not enough information
            return 0;
        } else if (VARIABLE_PROGRESSION.equals(match.getId()) || VARIABLE_PROGRESSION.equals(reference.getId())) {
            // Can't compare a variable progression against another known progression
            return 0;
        }
        return (NEUTRAL_DIST - distance(match.getId(), reference.getId())) / NEUTRAL_DIST;
    }

    /**
     * Compute the distance between two values, based on {@link #ORDERED_VALUES their coordinates}.
     * 
     * @param matchedOnset the onset value being scored, one of the HPO terms for pace of progression values
     * @param referenceOnset the onset value against which we're comparing
     * @return a number between {@code 0} (for perfect match) to {@link #MAX_DIST} (for complete mismatch)
     */
    private double distance(String matchedOnset, String referenceOnset)
    {
        Double qValue = ORDERED_VALUES.get(matchedOnset);
        Double tValue = ORDERED_VALUES.get(referenceOnset);
        if (qValue != null && tValue != null) {
            return Math.min(Math.abs(qValue - tValue), MAX_DIST);
        }
        return NEUTRAL_DIST;
    }
}
