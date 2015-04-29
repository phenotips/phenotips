/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.hpoa.prediction;

import org.phenotips.hpoa.annotation.HPOAnnotation;
import org.phenotips.hpoa.annotation.SearchResult;
import org.phenotips.hpoa.utils.maps.CounterMap;
import org.phenotips.hpoa.utils.maps.SumMap;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractPredictor implements Predictor
{
    protected HPOAnnotation annotations;

    @Override
    public void setAnnotation(HPOAnnotation annotations)
    {
        this.annotations = annotations;
    }

    @Override
    public List<SearchResult> getDifferentialPhenotypes(Collection<String> phenotypes)
    {
        List<SearchResult> result = new LinkedList<SearchResult>();
        SumMap<String> cummulativeScore = new SumMap<String>();
        CounterMap<String> matchCounter = new CounterMap<String>();
        List<SearchResult> matches = getMatches(phenotypes);
        for (SearchResult r : matches) {
            String omimId = r.getId();
            for (String hpoId : this.annotations.getPhenotypesWithAnnotation(omimId).keySet()) {
                if (phenotypes.contains(hpoId)) {
                    continue;
                }
                cummulativeScore.addTo(hpoId, r.getScore());
                matchCounter.addTo(hpoId);
            }
        }
        if (matchCounter.getMinValue() <= matches.size() / 2) {
            for (String hpoId : cummulativeScore.keySet()) {
                result.add(new SearchResult(hpoId, this.annotations.getOntology().getTerm(hpoId).getName(),
                    cummulativeScore.get(hpoId) / (matchCounter.get(hpoId) * matchCounter.get(hpoId))));
            }
            Collections.sort(result);
        }
        return result;
    }
}
