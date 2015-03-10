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
package org.phenotips.ontology;

import org.xwiki.stability.Unstable;

import java.util.Set;

import net.sf.json.JSON;

/**
 * A term from an {@link OntologyService ontology}. A few common properties are available as explicit individual
 * methods, and any property defined for the term can be accessed using the generic {@link #get(String)} method. As a
 * minimum, each term should have an identifier and a name. Terms can be accessed either using the owner
 * {@link OntologyService}, or the generic {@link OntologyManager}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public interface OntologyTerm
{
    /**
     * The (mandatory) term identifier, in the format {@code <ontology prefix>:<term id>}, for example
     * {@code HP:0002066} or {@code MIM:260540}.
     *
     * @return the term identifier, or {@code null} if the term doesn't have an associated identifier
     */
    String getId();

    /**
     * The short human-readable term name, for example {@code Gait ataxia}.
     *
     * @return the term name, or {@code null} if the term doesn't have an associated identifier
     */
    String getName();

    /**
     * The human-readable term description, usually a longer phrase or paragraph that describes the term.
     *
     * @return the term description, or {@code null} if the term doesn't have a description
     */
    String getDescription();

    /**
     * Returns the parents (direct ancestors) of this term.
     *
     * @return a set of ontology terms, or an empty set if the term doesn't have any ancestors in the ontology
     */
    Set<OntologyTerm> getParents();

    /**
     * Returns the ancestors (both direct and indirect ancestors) of this term.
     *
     * @return a set of ontology terms, or an empty set if the term doesn't have any ancestors in the ontology
     */
    Set<OntologyTerm> getAncestors();

    /**
     * Returns the ancestors (both direct and indirect ancestors) of this term <em>and</em> the term itself.
     *
     * @return a set of ontology terms, or a set with one term if the term doesn't have any ancestors in the ontology
     */
    Set<OntologyTerm> getAncestorsAndSelf();

    /**
     * Find the distance to another term in the ontology structure.
     *
     * @param other the term to which the distance should be computed
     * @return the minimum number of edges that connect the two terms in the DAG representing the ontology, or -1 if the
     *         terms are not connected
     */
    long getDistanceTo(OntologyTerm other);

    /**
     * Generic meta-property access. Any property defined in the ontology for this term can be accessed this way.
     *
     * @param name the name of the property to access
     * @return the value defined for the requested property in the ontology, or {@code null} if no value is defined
     */
    Object get(String name);

    /**
     * Returns the ontology where this term is defined.
     *
     * @return the owner ontology
     */
    OntologyService getOntology();

    /**
     * @return near-complete information contained in this term, in JSON format. Example:
     * <pre>
     * {
     *    "term_category":[
     *      "HP:0004325",
     *      "HP:0004323",
     *      "HP:0000001",
     *      "HP:0001507",
     *      "HP:0000118"
     *    ],
     *    "xref":[
     *      "MeSH:D015431 \"Weight Loss\"",
     *      "UMLS:C0043096 \"Decreased body weight\""
     *    ],
     *      "is_a":[
     *      "HP:0004325 ! Decreased body weight"
     *    ],
     *    "id":"HP:0001824",
     *    "name":"Weight loss",
     *    "def":"Reduction inexisting body weight."
     * }
     * </pre>
     * @throws java.lang.Exception could happen if casting fails
     * @since 1.1-rc1
     */
    JSON toJson() throws Exception;
}
