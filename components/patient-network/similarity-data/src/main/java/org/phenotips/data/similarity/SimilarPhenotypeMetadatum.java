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

import org.xwiki.stability.Unstable;

/**
 * View of a phenotype metadatum as related to another reference metadatum.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public interface SimilarPhenotypeMetadatum extends PhenotypeMetadatum
{
    /**
     * Does this similar phenotype metadata pair have both a match and a reference?
     * 
     * @return {@code true} if both related values are present, {@code false} otherwise
     */
    boolean isMatchingPair();

    /**
     * Returns the reference phenotype metadatum matched by this value, if any.
     * 
     * @return a phenotype metadatum from the reference patient, or {@code null} if this metadatum doesn't match a
     *         reference metadatum
     */
    PhenotypeMetadatum getReference();

    /**
     * How similar is this phenotype metadatum to the reference.
     * 
     * @return a similarity score, between {@code -1} for opposite phenotype descriptions and {@code 1} for an exact
     *         match, with {@code 0} for values with no similarities, and {@code NaN} in case there's no matched
     *         reference
     */
    double getScore();
}
