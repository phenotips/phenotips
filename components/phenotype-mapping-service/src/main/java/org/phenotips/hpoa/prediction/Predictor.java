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
package org.phenotips.hpoa.prediction;

import org.phenotips.hpoa.annotation.HPOAnnotation;
import org.phenotips.hpoa.annotation.SearchResult;

import org.xwiki.component.annotation.Role;

import java.util.Collection;
import java.util.List;

@Role
public interface Predictor
{
    /**
     * Obtains the list of OMIM diseases that fit a set of phenotypes, ordered descending by a "matching" score.
     *
     * @param phenotypes A set of HPO ids
     * @return A list of {@link SearchResult}s which map OMIM ids to fitness scores, ordered descending by score.
     */
    public List<SearchResult> getMatches(Collection<String> phenotypes);

    /**
     * Obtains a list of phenotypes that are likely to be useful in a differential diagnosis. These are basically
     * phenotypes present only in some of the diseases matching the input phenotypes. The score reflects the reliability
     * of the differentiation.
     *
     * @param phenotypes A set of HPO ids
     * @return A list of {@link SearchResult}s which map HPO ids to fitness scores, ordered descending by score.
     */
    public List<SearchResult> getDifferentialPhenotypes(Collection<String> phenotypes);

    public void setAnnotation(HPOAnnotation annotation);
}
