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
package org.phenotips.ontology.internal.solr;

import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;

import java.util.Collections;
import java.util.Set;

import org.apache.solr.common.SolrDocument;

/**
 * Implementation for {@link OntologyTerm} based on an indexed Solr document.
 * 
 * @version $Id$
 */
public class SolrOntologyTerm implements OntologyTerm
{
    /** The Solr document representing this term. */
    private SolrDocument doc;

    /**
     * The owner ontology.
     * 
     * @see #getOntology()
     */
    private OntologyService ontology;

    /**
     * The parents of this term, transformed from a set of IDs into a real set of terms.
     * 
     * @see #getParents()
     */
    private Set<OntologyTerm> parents;

    /**
     * The ancestors of this term, transformed from a set of IDs into a real set of terms.
     * 
     * @see #getAncestors()
     */
    private Set<OntologyTerm> ancestors;

    /**
     * Constructor that provides the backing {@link #doc Solr document} and the {@link #ontology owner ontology}.
     * 
     * @param doc the {@link #doc Solr document} representing this term
     * @param ontology the {@link #ontology owner ontology}
     */
    public SolrOntologyTerm(SolrDocument doc, OntologyService ontology)
    {
        this.doc = doc;
        this.ontology = ontology;
        if (doc != null) {
            this.parents = new LazySolrTermSet(doc.getFieldValues("is_a"), ontology);
            this.ancestors = new LazySolrTermSet(doc.getFieldValues("term_category"), ontology);
        }
    }

    @Override
    public String getId()
    {
        return this.doc != null ? (String) this.doc.getFirstValue("id") : null;
    }

    @Override
    public String getName()
    {
        return this.doc != null ? (String) this.doc.getFirstValue("name") : null;
    }

    @Override
    public String getDescription()
    {
        return this.doc != null ? (String) this.doc.getFirstValue("def") : null;
    }

    @Override
    public Set<OntologyTerm> getParents()
    {
        return this.parents != null ? this.parents : Collections.<OntologyTerm> emptySet();
    }

    @Override
    public Set<OntologyTerm> getAncestors()
    {
        return this.ancestors != null ? this.ancestors : Collections.<OntologyTerm> emptySet();
    }

    @Override
    public Object get(String name)
    {
        return this.doc != null ? this.doc.getFieldValue(name) : null;
    }

    @Override
    public OntologyService getOntology()
    {
        return this.ontology;
    }

    @Override
    public String toString()
    {
        return "[" + this.getId() + "] " + this.getName();
    }
}
