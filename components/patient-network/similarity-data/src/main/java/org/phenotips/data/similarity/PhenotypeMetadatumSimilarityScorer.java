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

import org.phenotips.data.PhenotypeMetadatum;

import org.xwiki.component.annotation.Role;

/**
 * Computes the similarity between two different values of the same {@link PhenotypeMetadatum metadatum type}. Since the
 * possible metadata types have very different structures, with overlapping and/or tree-organized values, it's not
 * possible to have a generic method good for all cases, so specific implementations for several metadata types are
 * available.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Role
public interface PhenotypeMetadatumSimilarityScorer
{
    /**
     * Computes the similarity score between two values. The values have to be from the same category.
     * 
     * @param match the value for the matched phenotype, may be {@code null}
     * @param reference the value for the reference phenotype, may be {@code null}
     * @return a similarity score, between {@code -1} for opposite values and {@code 1} for an exact match, with
     *         {@code 0} for incomparable values, including when one of the values is {@code null}
     */
    double getScore(PhenotypeMetadatum match, PhenotypeMetadatum reference);
}
