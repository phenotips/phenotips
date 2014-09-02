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
package org.phenotips.hpoa.annotation;

import org.phenotips.hpoa.ontology.Ontology;
import org.phenotips.hpoa.utils.graph.BGraph;
import org.phenotips.hpoa.utils.graph.IDAGNode;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class AbstractHPOAnnotation extends BGraph<AnnotationTerm> implements HPOAnnotation
{
    public static final Side HPO = BGraph.Side.R;

    public static final Side ANNOTATION = BGraph.Side.L;

    protected Ontology hpo;

    @Override
    public Ontology getOntology()
    {
        return this.hpo;
    }

    public AbstractHPOAnnotation(Ontology hpo)
    {
        this.hpo = hpo;
    }

    @Override
    public abstract int load(File source);

    public void propagateHPOAnnotations()
    {
        for (AnnotationTerm t : this.getAnnotations()) {
            propagateHPOAnnotations(t);
        }
    }

    public void propagateHPOAnnotations(AnnotationTerm annTerm)
    {
        Set<String> newAnnotations = new HashSet<String>();
        Set<String> front = new HashSet<String>();

        front.addAll(annTerm.getNeighbors());
        Set<String> newFront = new HashSet<String>();
        while (!front.isEmpty()) {
            for (String nextTermId : front) {
                IDAGNode nextNode = this.hpo.getTerm(nextTermId);
                if (nextNode == null) {
                    System.err.println("No matching term found in HPO for " + nextTermId + " (" + annTerm + ")");
                    continue;
                }
                for (String parentTermId : nextNode.getParents()) {
                    if (!newAnnotations.contains(parentTermId)) {
                        newFront.add(parentTermId);
                        newAnnotations.add(parentTermId);
                    }
                }
            }
            front.clear();
            front.addAll(newFront);
            newFront.clear();
        }
        newAnnotations.removeAll(annTerm.getNeighbors());
        for (String hpoId : newAnnotations) {
            this.addConnection(annTerm, new AnnotationTerm(this.hpo.getRealId(hpoId)));
        }
    }

    @Override
    public Set<String> getAnnotationIds()
    {
        return this.getNodesIds(ANNOTATION);
    }

    @Override
    public Set<String> getHPONodesIds()
    {
        return this.getNodesIds(HPO);
    }

    @Override
    public Collection<AnnotationTerm> getAnnotations()
    {
        return this.getNodes(ANNOTATION);
    }

    @Override
    public Collection<AnnotationTerm> getHPONodes()
    {
        return this.getNodes(HPO);
    }

    @Override
    public AnnotationTerm getAnnotationNode(String annId)
    {
        return this.getNode(annId, ANNOTATION);
    }

    @Override
    public AnnotationTerm getHPONode(String id)
    {
        return this.getNode(id, HPO);
    }

    @Override
    public Map<String, String> getPhenotypesWithAnnotation(String annId)
    {
        Map<String, String> results = new TreeMap<String, String>();
        AnnotationTerm omimNode = this.getAnnotationNode(annId);
        for (String hpId : omimNode.getNeighbors()) {
            String hpName = this.hpo != null ? this.hpo.getName(hpId) : hpId;
            results.put(hpId, hpName);
        }
        return results;
    }
}
