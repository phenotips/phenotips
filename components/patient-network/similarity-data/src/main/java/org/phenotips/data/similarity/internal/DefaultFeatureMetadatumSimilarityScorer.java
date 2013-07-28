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

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Simple {@link FeatureMetadatumSimilarityScorer similarity scorer} that gives {@code 0} if any of the two values is
 * missing, {@code 1} for exact matches, and {@code -1} if the two values are different.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Singleton
public class DefaultFeatureMetadatumSimilarityScorer implements FeatureMetadatumSimilarityScorer
{
    @Override
    public double getScore(FeatureMetadatum match, FeatureMetadatum reference)
    {
        if (reference == null || match == null || !StringUtils.equals(reference.getType(), match.getType())) {
            // Not enough information, or different metadata types
            return 0;
        } else if (StringUtils.equals(reference.getId(), match.getId())) {
            // Same term
            return 1;
        } else {
            // Different terms
            return -1;
        }
    }
}
