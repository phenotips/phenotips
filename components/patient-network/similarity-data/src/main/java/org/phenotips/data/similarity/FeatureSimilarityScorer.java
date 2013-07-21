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
package org.phenotips.data.similarity;

import org.phenotips.data.Feature;

import org.xwiki.component.annotation.Role;

/**
 * Computes the similarity between two different {@link Feature} values.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Role
public interface FeatureSimilarityScorer
{
    /**
     * Computes the similarity score between two feature terms. The values have to be from the same ontology.
     * 
     * @param match the matched feature, should not be {@code null}
     * @param reference the reference feature, should not be {@code null}
     * @return a similarity score, between {@code -1} for opposite values and {@code 1} for an exact match, with
     *         {@code 0} for incomparable values, and {@code NaN} when one of the values is {@code null} or one of the
     *         terms can't be found in the ontology
     */
    double getScore(Feature match, Feature reference);
}
