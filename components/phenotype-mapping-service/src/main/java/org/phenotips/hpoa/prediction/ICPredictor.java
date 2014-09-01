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

import org.phenotips.hpoa.annotation.AnnotationTerm;
import org.phenotips.hpoa.annotation.SearchResult;
import org.phenotips.hpoa.ontology.OntologyTerm;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

@Component
@Named("ic")
@Singleton
public class ICPredictor extends AbstractPredictor
{
    private final Map<String, Double> icCache = new HashMap<String, Double>();

    public double getIC(String hpoId)
    {
        return getIC(this.annotations.getHPONode(hpoId));
    }

    private double getIC(AnnotationTerm hpoNode)
    {
        return hpoNode == null ? 0 : getCachedIC(hpoNode);
    }

    private double getCachedIC(AnnotationTerm hpoNode)
    {
        Double result = this.icCache.get(hpoNode.getId());
        if (result == null) {
            result = -Math.log((double) hpoNode.getNeighborsCount() / this.annotations.getAnnotations().size());
            this.icCache.put(hpoNode.getId(), result);
        }
        return result;
    }

    public OntologyTerm getMICA(String hpoId1, String hpoId2)
    {
        String micaId = getMICAId(hpoId1, hpoId2);
        if (micaId != null) {
            return this.annotations.getOntology().getTerm(micaId);
        }
        return null;
    }

    public String getMICAId(String hpoId1, String hpoId2)
    {
        // TODO: implement more efficiently!
        Set<String> common = new HashSet<String>();
        common.addAll(this.annotations.getOntology().getAncestors(hpoId1));
        common.retainAll(this.annotations.getOntology().getAncestors(hpoId2));
        double max = -1;
        String micaId = this.annotations.getOntology().getRootId();
        for (String a : common) {
            double ic = this.getIC(a);
            if (ic >= max) {
                max = ic;
                micaId = a;
            }
        }
        return micaId;
    }

    public double asymmetricPhenotypeSimilarity(Collection<String> query, Collection<String> reference)
    {
        double result = 0.0;
        for (String q : query) {
            double bestMatchIC = 0;
            for (String r : reference) {
                double ic = this.getIC(this.getMICAId(q, r));
                if (ic > bestMatchIC) {
                    bestMatchIC = ic;
                }
            }
            result += bestMatchIC;
        }
        return result / (query.size() > 0 ? query.size() : 1);
    }

    public double symmetricPhenotypeSimilarity(Collection<String> query, Collection<String> reference)
    {
        return .5 * asymmetricPhenotypeSimilarity(query, reference) + .5
            * asymmetricPhenotypeSimilarity(reference, query);
    }

    @Override
    public List<SearchResult> getMatches(Collection<String> phenotypes)
    {
        List<SearchResult> result = new LinkedList<SearchResult>();
        for (AnnotationTerm o : this.annotations.getAnnotations()) {
            Set<String> annPhenotypes = this.annotations.getPhenotypesWithAnnotation(o.getId()).keySet();
            double matchScore = this.asymmetricPhenotypeSimilarity(phenotypes, annPhenotypes);
            if (matchScore > 0) {
                result.add(new SearchResult(o.getId(), o.getName(), matchScore));
            }
        }
        Collections.sort(result);
        return result;
    }
}
