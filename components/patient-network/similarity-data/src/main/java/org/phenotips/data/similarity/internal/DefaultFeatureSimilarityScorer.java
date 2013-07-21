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

import org.phenotips.data.Feature;
import org.phenotips.data.similarity.FeatureSimilarityScorer;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.annotation.Component;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * {@link FeatureSimilarityScorer Similarity scorer} for features, rewarding features that are closely related.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Singleton
public class DefaultFeatureSimilarityScorer implements FeatureSimilarityScorer
{
    /** Provides access to the term ontology. */
    @Inject
    private OntologyManager ontologyManager;

    @Override
    public double getScore(Feature match, Feature reference)
    {
        if (match == null || reference == null) {
            return Double.NaN;
        } else if (StringUtils.equals(match.getId(), reference.getId())) {
            return 1;
        }

        OntologyTerm matchTerm = this.ontologyManager.resolveTerm(match.getId());
        OntologyTerm referenceTerm = this.ontologyManager.resolveTerm(reference.getId());
        if (matchTerm == null || referenceTerm == null) {
            return Double.NaN;
        }

        int distance = getDistance(matchTerm, referenceTerm);
        return distance <= 3 ? Math.pow(2.0, -distance) : 0;
    }

    /**
     * Computes the distance (number of levels) between two terms. If one of the terms isn't a close ancestor of the
     * other (at most 3 generations away), then {@code Integer.MAX_VALUE} is returned. The order of the two terms
     * doesn't matter.
     * 
     * @param match the first feature term
     * @param reference the second feature term
     * @return a number between {@code 1} and {@code 3} representing the number of generations between the two terms, or
     *         {@code Integer.MAX_VALUE} if the two terms aren't related or are more than 3 generations apart
     */
    private int getDistance(OntologyTerm match, OntologyTerm reference)
    {
        OntologyTerm toFind;
        Set<OntologyTerm> lookIn;
        // We're looking for the more generic term among the ancestors of the other, so order them accordingly
        if (match.getAncestors().contains(reference)) {
            toFind = reference;
            lookIn = match.getParents();
        } else if (reference.getAncestors().contains(match)) {
            toFind = match;
            lookIn = reference.getParents();
        } else {
            // The terms are not comparable, we can't compute a similarity for them
            return Integer.MAX_VALUE;
        }

        int distance = 1;
        while (!lookIn.contains(toFind)) {
            Set<OntologyTerm> next = new HashSet<OntologyTerm>();
            for (OntologyTerm term : lookIn) {
                next.addAll(term.getParents());
            }
            lookIn = next;
            if (++distance > 3) {
                break;
            }
        }

        return distance <= 3 ? distance : Integer.MAX_VALUE;
    }
}
